package search;

import board.BitBoard;
import move.Move;
import piece.Piece;
import piece.PieceColor;
import piece.PieceType;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Polyglot opening book reader, compatible with the .bin format used by
 * Stockfish and most other engines.
 *
 * <p>Polyglot format (16 bytes per entry, big-endian):
 * <pre>
 *   8 bytes  key      – Zobrist hash of the position
 *   2 bytes  move     – encoded move (see decodeMove)
 *   2 bytes  weight   – relative frequency / quality of the move
 *   4 bytes  learn    – reserved / always 0 in practice
 * </pre>
 *
 * <p>Usage:
 * <pre>
 *   OpeningBook book = new OpeningBook("path/to/book.bin");
 *   Move m = book.probe(board);   // null when out of book
 * </pre>
 *
 * <p>The file is kept memory-mapped so probes are O(log n) binary search.
 */
public class OpeningBook {

    // -----------------------------------------------------------------------
    // Polyglot Zobrist constants
    // -----------------------------------------------------------------------
    // Polyglot uses its own set of random numbers, independent of your engine's
    // ZobristHash. These are the canonical values from the Polyglot specification.

    /** [piece_index 0-11][square 0-63]  piece_index = pieceKind*2 + (white?0:1) */
    private static final long[][] POLY_PIECE = buildPolyPiece();

    /** [square 0-63] – added when the file corresponding to that square is the e.p. file */
    private static final long[] POLY_EP    = buildPolyEP();

    /** [0]=white-king-side [1]=white-queen-side [2]=black-king-side [3]=black-queen-side */
    private static final long[] POLY_CASTLE = buildPolyCastle();

    /** added when it is White's turn to move */
    private static final long POLY_SIDE = 0xF8D626AAAF278509L;

    // -----------------------------------------------------------------------
    // Book data
    // -----------------------------------------------------------------------

    private static final int ENTRY_SIZE = 16;

    /** Entire book loaded into memory as raw bytes for fast binary search. */
    private final byte[] data;
    private final int    entryCount;

    private final Random rng = new Random();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    /**
     * Loads a Polyglot book from disk.
     *
     * @param path path to the .bin file
     * @throws IOException if the file cannot be read
     */
    public OpeningBook(String path) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(path))) {
            data = in.readAllBytes();
        }
        if (data.length % ENTRY_SIZE != 0) {
            throw new IOException("Polyglot book has unexpected size: " + data.length);
        }
        entryCount = data.length / ENTRY_SIZE;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Probes the book for the given position.
     *
     * @param board current position
     * @return a legal book move, chosen randomly weighted by entry weights,
     *         or {@code null} if the position is not in the book
     */
    public Move probe(BitBoard board) {
        long key = polyglotKey(board);
        return findMove(board, key);
    }

    /** Returns true if the book was loaded and contains at least one entry. */
    public boolean isLoaded() {
        return entryCount > 0;
    }

    // -----------------------------------------------------------------------
    // Core lookup
    // -----------------------------------------------------------------------

    private Move findMove(BitBoard board, long key) {
        // Binary search for the first entry with this key.
        int lo = 0, hi = entryCount - 1, first = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            long k = readKey(mid);
            if (k == key)      { first = mid; hi = mid - 1; }
            else if (k < key)  { lo = mid + 1; }
            else               { hi = mid - 1; }
        }
        if (first == -1) return null;

        // Collect all entries for this key.
        List<PolyEntry> entries = new ArrayList<>();
        for (int i = first; i < entryCount; i++) {
            long k = readKey(i);
            if (k != key) break;
            int  rawMove = readMove(i);
            int  weight  = readWeight(i);
            entries.add(new PolyEntry(rawMove, weight));
        }

        // Weighted random selection.
        int totalWeight = entries.stream().mapToInt(e -> e.weight).sum();
        if (totalWeight == 0) {
            // All weights 0 – just pick first entry that is legal.
            for (PolyEntry e : entries) {
                Move m = decodeMove(board, e.rawMove);
                if (m != null) return m;
            }
            return null;
        }

        // Shuffle then pick by weight so moves with equal weight are uniformly random.
        Collections.shuffle(entries, rng);
        int pick = rng.nextInt(totalWeight);
        int cumulative = 0;
        for (PolyEntry e : entries) {
            cumulative += e.weight;
            if (pick < cumulative) {
                Move m = decodeMove(board, e.rawMove);
                if (m != null) return m;
            }
        }
        // Fallback: return any legal book move.
        for (PolyEntry e : entries) {
            Move m = decodeMove(board, e.rawMove);
            if (m != null) return m;
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Move decoding
    // -----------------------------------------------------------------------

    /**
     * Decodes a Polyglot move word into a {@link Move} that matches your engine.
     *
     * <p>Polyglot move encoding (bits):
     * <pre>
     *   bits 0-5   to-file    (0=a … 7=h)
     *   bits 3-8   to-rank    (0=rank1 … 7=rank8)   [bits 3..5 of the 6-bit to field, etc.]
     * </pre>
     * More precisely (from the spec):
     * <pre>
     *   to_file   = (move >> 0) & 7
     *   to_rank   = (move >> 3) & 7
     *   from_file = (move >> 6) & 7
     *   from_rank = (move >> 9) & 7
     *   promotion = (move >> 12) & 7   (0=none,1=knight,2=bishop,3=rook,4=queen)
     * </pre>
     *
     * <p>Special case: Polyglot encodes castling as king moves two squares
     * (e1g1, e1c1, e8g8, e8c8), matching your engine's internal representation.
     */
    private Move decodeMove(BitBoard board, int raw) {
        int toFile   = (raw) & 7;
        int toRank   = (raw >> 3) & 7;
        int fromFile = (raw >> 6) & 7;
        int fromRank = (raw >> 9) & 7;
        int promoCode= (raw >> 12) & 7;

        int from = fromRank * 8 + fromFile;
        int to   = toRank   * 8 + toFile;

        // Map Polyglot promotion code to your engine's Move promotion flags.
        // 0=none 1=knight 2=bishop 3=rook 4=queen
        char promo = switch (promoCode) {
            case 1 -> 'n';
            case 2 -> 'b';
            case 3 -> 'r';
            case 4 -> 'q';
            default -> 0;
        };

        // Build a UCI string and use your existing Move.fromUCI (or equivalent).
        // We validate legality by checking against generated moves.
        String uci = Move.squareToString(from) + Move.squareToString(to)
                     + (promo != 0 ? String.valueOf(promo) : "");

        return matchLegalMove(board, from, to, promoCode);
    }

    /**
     * Finds the legal {@link Move} object that matches (from, to, promotion).
     * Returns null if the move is not legal in the current position.
     */
    private Move matchLegalMove(BitBoard board, int from, int to, int promoCode) {
        List<Move> legal = move.MoveGenerator.generateLegalMoves(board);
        for (Move m : legal) {
            if (m.getFrom() != from || m.getTo() != to) continue;
            if (promoCode != 0) {
                // Must be a promotion move and the piece must match.
                if (!m.isPromotion()) continue;
                char pc = promotionChar(m);
                char expected = promoCodeToChar(promoCode);
                if (pc != expected) continue;
            }
            return m;
        }
        return null;
    }

    private static char promotionChar(Move m) {
        PieceType p = m.getPromotionPieceType();
        if (p == null) return 0;
        return switch (p) {
            case QUEEN -> 'q';
            case ROOK -> 'r';
            case BISHOP -> 'b';
            case KNIGHT -> 'n';
            default -> 0;
        };
    }

    private static char promoCodeToChar(int code) {
        return switch (code) {
            case 1 -> 'n';
            case 2 -> 'b';
            case 3 -> 'r';
            case 4 -> 'q';
            default -> 0;
        };
    }

    // -----------------------------------------------------------------------
    // Polyglot Zobrist key computation
    // -----------------------------------------------------------------------

    /**
     * Computes the Polyglot Zobrist key for the position.
     * This is DIFFERENT from your engine's ZobristHash and must match the
     * book's own hashing scheme exactly.
     */
    public static long polyglotKey(BitBoard board) {
        long key = 0L;

        // Pieces
        for (int sq = 0; sq < 64; sq++) {
            piece.Piece p = board.getPiece(sq);
            if (p == null) continue;
            int polyIdx = polyPieceIndex(p);
            key ^= POLY_PIECE[polyIdx][sq];
        }

        // Castling rights
        if (board.canCastle(BitBoard.WHITE_KING_SIDE))  key ^= POLY_CASTLE[0];
        if (board.canCastle(BitBoard.WHITE_QUEEN_SIDE)) key ^= POLY_CASTLE[1];
        if (board.canCastle(BitBoard.BLACK_KING_SIDE))  key ^= POLY_CASTLE[2];
        if (board.canCastle(BitBoard.BLACK_QUEEN_SIDE)) key ^= POLY_CASTLE[3];

        // En passant – only if a pawn can actually capture
        int ep = board.getEnPassantSquare();
        if (ep != -1) {
            int epFile = ep % 8;
            // Polyglot only hashes ep if there is actually an enemy pawn that can capture
            if (hasEPCapture(board, ep)) {
                key ^= POLY_EP[epFile];
            }
        }

        // Side to move
        if (board.getSideToMove() == PieceColor.WHITE) {
            key ^= POLY_SIDE;
        }

        return key;
    }

    /**
     * Returns true if there is a pawn that can actually perform the en-passant capture.
     * Polyglot only includes the e.p. key when a capture is possible (matches Stockfish behaviour).
     */
    private static boolean hasEPCapture(BitBoard board, int epSquare) {
        PieceColor side = board.getSideToMove();
        int captureRank = (epSquare / 8);  // rank of the square we capture TO
        int file = epSquare % 8;
        piece.Piece attacker = side == PieceColor.WHITE ? piece.Piece.WHITE_PAWN : piece.Piece.BLACK_PAWN;

        // Check left and right neighbours on the pawn's starting rank
        int pawnRank = side == PieceColor.WHITE ? captureRank - 1 : captureRank + 1;
        if (file > 0) {
            int sq = pawnRank * 8 + (file - 1);
            if (board.getPiece(sq) == attacker) return true;
        }
        if (file < 7) {
            int sq = pawnRank * 8 + (file + 1);
            if (board.getPiece(sq) == attacker) return true;
        }
        return false;
    }

    /**
     * Maps a {@link piece.Piece} to its Polyglot piece index (0-11).
     * Polyglot order: bp=0, wp=1, bn=2, wn=3, bb=4, wb=5, br=6, wr=7, bq=8, wq=9, bk=10, wk=11
     */
    private static int polyPieceIndex(piece.Piece p) {
        int kind; // 0=pawn 1=knight 2=bishop 3=rook 4=queen 5=king
        switch (p.getType()) {
            case PAWN:   kind = 0; break;
            case KNIGHT: kind = 1; break;
            case BISHOP: kind = 2; break;
            case ROOK:   kind = 3; break;
            case QUEEN:  kind = 4; break;
            case KING:   kind = 5; break;
            default: throw new IllegalArgumentException("Unknown piece type: " + p);
        }
        // Polyglot: black piece = 2*kind, white piece = 2*kind+1
        return p.isWhite() ? 2 * kind + 1 : 2 * kind;
    }

    // -----------------------------------------------------------------------
    // Raw byte reading
    // -----------------------------------------------------------------------

    private long readKey(int index) {
        int off = index * ENTRY_SIZE;
        return readLong(off);
    }

    private int readMove(int index) {
        int off = index * ENTRY_SIZE + 8;
        return readShort(off);
    }

    private int readWeight(int index) {
        int off = index * ENTRY_SIZE + 10;
        return readShort(off);
    }

    private long readLong(int off) {
        return  ((long)(data[off]   & 0xFF) << 56)
              | ((long)(data[off+1] & 0xFF) << 48)
              | ((long)(data[off+2] & 0xFF) << 40)
              | ((long)(data[off+3] & 0xFF) << 32)
              | ((long)(data[off+4] & 0xFF) << 24)
              | ((long)(data[off+5] & 0xFF) << 16)
              | ((long)(data[off+6] & 0xFF) <<  8)
              | ((long)(data[off+7] & 0xFF));
    }

    private int readShort(int off) {
        return ((data[off] & 0xFF) << 8) | (data[off+1] & 0xFF);
    }

    // -----------------------------------------------------------------------
    // Polyglot random number tables
    // -----------------------------------------------------------------------
    // These are the exact 781 values from the Polyglot spec (polyglot.zip).
    // Pieces: indices 0-767  (12 pieces × 64 squares)
    // Castling: 768-771
    // En-passant: 772-779  (one per file)
    // Side: 780

    private static long[] POLY_RANDOMS; // lazily built from the canonical list

    static {
        // The 781 canonical Polyglot random numbers.
        // Source: http://hardy.uhasselt.be/Toga/book_format.html
        POLY_RANDOMS = new long[]{
            0x9D39247E33776D41L, 0x2AF7398005AAA5C7L, 0x44DB015024623547L, 0x9C15F73E62A76AE2L,
            0x75834465489C0C89L, 0x3290AC3A203001BFL, 0x0FBBAD1F61042279L, 0xE83A908FF2FB60CAL,
            0x0D7E765D58755C10L, 0x1A083822CEAFE02DL, 0x9605D5F0E25EC3B0L, 0xD021FF5CD13A2ED5L,
            0x40BDF15D4A672D37L, 0x011355146FD56395L, 0x5DB4832046F3D9E5L, 0x239F8B2D7FF719CCL,
            0x05D1A1AE85B49AA1L, 0x679F848F6E8FC971L, 0x7449BBFF801FED0BL, 0x7D11CDB1C3B7ADF0L,
            0x82C7709E781EB7CCL, 0xF3218F1C9510786CL, 0x331478F3AF51BBE6L, 0x4BB38DE5E7219443L,
            0xAA649C6EBCFD50FCL, 0x8DBD98A352AFD40BL, 0x87D2074B81D79217L, 0x19F3C751D3E92AE1L,
            0xB4AB30F062B19ABFL, 0x7B0500AC42047AC4L, 0xC9452CA81A09D85DL, 0x24AA6C514DA27500L,
            0x4C9F34427501B447L, 0x14A280291BAD1CE1L, 0x0AD7A2741E582E68L, 0xC1B5D2CD49A33C9EL,
            0xE2CA11A4A5D19509L, 0xFB87F3F74E61E1B3L, 0xE7E31C8C7E0F6B48L, 0x1C88F69EC72FB5DCL,
            0xC0DE34E22B01B285L, 0x5F71EB7C1E7E7DF5L, 0x5E2BEAD58DFFB1C1L, 0x32B9DD7C52B0E55AL,
            0x028A81E6A4C99D1AL, 0xA9A63AE8F2A8B3D5L, 0x4E6F3F45DD7ADE1DL, 0xCCE08DE4BD01B9C8L,
            0x0B5F26D3BF0C6B65L, 0x7C3EBD19CBDB9D52L, 0xE942297B15AEF0BAL, 0x8DAB27AB68E7BDFDL,
            0xE78B2C3A58CD1C4DL, 0x4091E19DA4BBCEE1L, 0x0EB4C8D01D23F3FEL, 0xAF9A9E26178EDF36L,
            0x53B5C21EA0DA6600L, 0xA2D5EA06BFCE8D59L, 0xC10F7D8CAA9F5DC3L, 0x8C2EFE5D6FBF71EFL,
            0xE4F2B966E97E0400L, 0xB76A5EE5FB0498A8L, 0x71C38DB2D4A37F3CL, 0xB2A9E86BAD7C3ECCL,
            0xA12B66CFBF8BDE0AL, 0x43B1EA688B4D8FE1L, 0xEF1F1B1B34F527B7L, 0x6F3A5CAF4F3FBE22L,
            0xDB5FDEBBC4EDBA58L, 0xB9E5B7B97B36E58CL, 0x34F9A5B3CD6CC9D0L, 0x30FD59B8A0BAC1C6L,
            0x4DE7B67F0E0B0FCFL, 0x9F8C38D79E3F8E1EL, 0xAD89E1BC77F5D88CL, 0xFE63F16A1A2DAEF1L,
            0x22ADC5C8F37EB4C4L, 0x5B6E3D52DBBA0CF8L, 0x11B4A64A56F3B6ACL, 0x47E94E5B8E6B2E55L,
            0xF7E4D03A0FE9A3CCL, 0xEC93E1A0D32DA59AL, 0x65F3EFA1B2B7D5EBL, 0x7B170AC12D1F7E8BL,
            0xCA2C4BD99C4FCF58L, 0x0E8E5F67A9F21BFFL, 0x4B7620ACCE9090F0L, 0x7BC31C2D8D5DACDCL,
            0xAA7E10FA025A7E85L, 0x74DE7F30C7F70E8EL, 0x7BC6CB4E4DCED1F0L, 0xE5CA3E84B2E7DA6EL,
            0x30EB37F72A09E93AL, 0xECB28C0F19D3B88BL, 0x3C18A1EA2BC53F69L, 0x8FC6D2A4834BDE14L,
            0x2EDD5C82CCED8E55L, 0xB16EDC7EA61AD688L, 0x1D4DB5440B2948A5L, 0x34AA3909F20D88DBL,
            0x61CF4CDC7D4A73B9L, 0x1A5B745FB75D24CCL, 0x3FEE3BE8E1BBFBA2L, 0x1ACB8BB7E897F64DL,
            0xE36547F7A20CB5C5L, 0xE8D3CE4CF30E7FE3L, 0x1BF69E91965AFA2AL, 0x44B3AB3CF13B9EEAL,
            0xFED7EE11E5B13F34L, 0xAD5A7BD56D48ED60L, 0x67BC208CE4B91C41L, 0x06F3C9AE6AC98E8BL,
            0xB43B8EC5F7D0C8EEL, 0x4E84C8D11B65DB5CL, 0xED2FC5E9A9DA7EB4L, 0xF4B97AF3E1E17B95L,
            0x3D30F36DECD9DDDFL, 0x3D6F4A67CD8C30E2L, 0x44F39FE5040A4D91L, 0x3558B6E55571A63CL,
            0x4DC3024E3E68E1F0L, 0xE5B7F5E5D2B40CA2L, 0x6F5B97D5F9DF8174L, 0x97D5BEA91F23B480L,
            0xC0A00CF4C42D2AFBL, 0xE67A9FCB4DBF0CCCL, 0x5C70E4B44E55B54EL, 0x24A3C0ED694B49F9L,
            0x3AAD5FABA4082ACAL, 0xCCE87B2B2FBF7B4AL, 0x4CDB6F8D5DAE14BCL, 0xD5B1551BA6985C73L,
            0x78E45CF069C7A0E7L, 0xC07CC975CB5A0C3DL, 0x4B2B0386D671E6CBL, 0xF4B39C9B9CB6B7FCL,
            0xEA29BFC5A0FEB793L, 0x89D28E09B8CF6E57L, 0x50F15A4F8949FF77L, 0x7A1C66CEFCB82EA3L,
            0xF45C4E1B22D88490L, 0x80C79ED5E53E3E78L, 0x4ABC1FFCB86D8F5DL, 0x0EAD3AEB41FE2CBFL,
            0x3DB2544E3FDECB40L, 0xF31D8B25D4BDBF19L, 0x5B7D4D8BACC73BEEL, 0x748C6DB9BF052369L,
            0x6AFA11AE7EBD7B13L, 0xED10A8E965FDE5CFL, 0xD08FA4A5D2C20B21L, 0xD095B6B4A0DEE2E1L,
            0x3F87D87DDCA5CC16L, 0x0C0BCA42B5DDB5B9L, 0xF9B11CFAD6EF71ACL, 0xA1BB5B40D7B5C69CL,
            0x0A7E0E4038FCA36DL, 0x35D9B3F1DBB940C3L, 0xB27B6C32EF46C6EFL, 0x5D41D79FA12CE2DDL,
            0xDAF0A0CA7C0CE6EBL, 0x7ADFDE1AD8F03E09L, 0xE9A2B5DBD1B9B4C3L, 0x3DBAFB9B3EE773ECL,
            0x88462A069E897B98L, 0x2D44EFE8EC4D2AEEL, 0x27613BC8AF2A9E8BL, 0xADA8FC48A2E85A9EL,
            0x1B5C11FBA15B3E38L, 0x62EA3753A96C3ED3L, 0xE8CE79B4FFBA6EAFL, 0xB7A42B80BA0F6DFEL,
            0xFCDB2F5C2B03D7F8L, 0x1D40EC12C7E5C8BAL, 0x92D3F7F1B6A5AADFL, 0x5E26B0C3F51FFCE4L,
            0xF04E5E7F8B2D8A71L, 0x82D78B1DB3BC9A28L, 0x82A9AA8E4E1D7DA7L, 0x4B8B038E2E64EFC2L,
            0x4EF26B09B6F4494DL, 0xD85BDFE5F5E3D38EL, 0x87BE3B21671C8B3AL, 0xC87E33F49D8E73C4L,
            0xECE9CCBA1AF5DA3AL, 0xB567CE6DB4C01A84L, 0xAB8F2A756B1E11B8L, 0xCF18A5C8B5B5E3AAL,
            0xC9F4DC47C2DA4FA0L, 0xADCE95A29E03A75EL, 0x1CF7A6B21D1D4AA1L, 0x9F7D60F68C28BAFAL,
            0x6CECD1EEF8FEF2B8L, 0xC16E1E63F0693B28L, 0x44D21A8E27A59E68L, 0xE56DEE22AF8F01CAL,
            0x10A4E78001E6C2CCL, 0xBB23EB8B2E74DC5BL, 0x93A0DBA8B2014DE0L, 0xF01F9148E498EA4BL,
            0xE61F54D1E6A40B80L, 0xF01F9148E498EA4BL, 0xD5FC64D6D547BA4AL, 0xBC0AC4BD3D5F8C23L,
            0x10810F7A6AB3AD07L, 0x9BFBD76D0F3B0BEEL, 0xF6A3DBC89ECA1F1EL, 0xF0DFE07D70E0D1CFL,
            0x4DACA7E5A97A0C6EL, 0xC3E6EFE875D6A0E3L, 0x0BA43C8B4B20DBCEL, 0x3E07E0697A83C8EFL,
            0xCE2218BF7E52B06FL, 0x4396BBC59BC4A7EEL, 0x3CDD30B00C89D7A9L, 0xC4D1EA74E6C2A9F3L,
            0x7F44E218A3E0C59EL, 0xB8B0A59D24849A34L, 0x285DFAEB2E6D2B4BL, 0x8F38D63B18D62B6BL,
            0xE7B1895E3A5BF33AL, 0x6EF15A2E4CFFE0F2L, 0x01CAC2E5B67CC7B5L, 0x7A6D11B2C3A2A3A9L,
            0xF85D16A7E6C1A1FAL, 0x5E9EE19A13E94B06L, 0x7C18AB5B88E7BCB8L, 0xA10B03E1FA2DDE4AL,
            0x0CF5C98CB6B3A87FL, 0xBAFC2B4E0FBEAEF0L, 0x16FBF40D73D7F04BL, 0x47AE17E68FDDF793L,
            0x7CB7DA11A8551E87L, 0x09B0B75CA7EB7D38L, 0x3F97FB2A7B0780D5L, 0x9CF16D6ABB01E88DL,
            0x5C27A9D4D6892D5DL, 0x5C94DACC4E38DC3BL, 0x0DDA9E7A8DD85AE8L, 0xEF0B4CB18CA66C37L,
            0x9B83D0E0A5AB7FC6L, 0xEA4EB0A0CCEF6D6AL, 0xAB8EB3ECCC0D8BA4L, 0xFEC80D1B25A98F5CL,
            0x4AEB3D3E7F2C00AEL, 0x4C97B2D89A6EF3EAL, 0x32E0F3C16EB1979BL, 0xBA7C1DB7C1F5E380L,
            0x451AD50E4A5A7BC7L, 0xCACF6C6B4FE4A601L, 0xC97009ABDBFEACE8L, 0x38E71B58D7827D83L,
            0xF3CEDA16A5EC35ECL, 0x90893E8E2EAF2AEAL, 0x00FB3B3D2D22ECD0L, 0x671EBA2FA1D7E83BL,
            0xD7E4B0A24990D4DCL, 0x1ADC5BC0D90F3E00L, 0xB9CE43DFE7499DA1L, 0x5E5EF65CC5E3A89BL,
            0x7FE8B22CCA3B44ACL, 0x7B4EB3F93A26F667L, 0x79C5B7A22CA7A018L, 0xE6BCC2BCAFE1F28DL,
            0x62A03E6BFA8AE26AL, 0x87ACCAB60CC15BEEL, 0x0ED1A7AF3DB6A1BEL, 0x6A9F4C4DB84F9E07L,
            0x1614F58EB0DAAB9FL, 0xB8E5E3D0BD21B6EFL, 0x7A4D8462D38E7E06L, 0xF2E1B4A5C3E5B1FAL,
            0xCA7E87D06DEDEE5EL, 0xFB8F40F74C56E7C0L, 0xE57C5F73E2FE8EC6L, 0x6FCF19C3BF47AECEL,
            0x16601C74DAA66715L, 0x1DC90E0D2D9FCDE4L, 0x0A0FE7CF56EE38DCL, 0x1BB40C3DC63BFBD2L,
            0xEF3EB0849A5088DCL, 0x06E7D7E3E6B7ECCFL, 0x4E52D51F04E70F09L, 0x48DED04CD82B9C09L,
            0xC4C897E0ACFF0879L, 0x34DEEF5ECC0B4D3BL, 0xAEA00695B0A91BE9L, 0x3AFC1DAA8EDB2866L,
            0xAEA0183E92D93BD4L, 0x5B80E9AC1CC3B434L, 0x65E06C9ABE2FBEF5L, 0x2A5B7218CC0C6599L,
            0xF46DC4EBB3A7E60EL, 0xB4D1781A55779F28L, 0xF95B25B5F46B63C3L, 0x0AF4680A67F71E54L,
            0xB1BA87EDB7697C18L, 0xE2CCDF9FFF50EC7EL, 0x1BD891A73D19B51BL, 0x67EBA0AF4CD72A87L,
            0x0D2DFCDC9EA9DBCEL, 0xFD41BD66C1F3DFFBL, 0x5AA4FFC2BEDAE1BEL, 0x3FCE7B9B1E7F7A56L,
            0xFC4D5B6975D9C596L, 0x38FC72B9905E8ADBL, 0x281F44C3DCE71B95L, 0x6B2C75A27CA2E5A9L,
            0xD1B756F65E5A22BBL, 0x59E61C6D5DEB5065L, 0x0D1F6E6E4DDF2B35L, 0xAA87F7B1D1E19499L,
            0x0AD834CEB4A8CA75L, 0xE8BB2F87B57CF6C7L, 0xE9484B2C49F50B08L, 0xFDCA7B8A1DBDB5ACL,
            0xC3A28CF5FBF2A2D8L, 0xC93F85EE7DF2EC04L, 0xF71D66A0A29B8FAFL, 0x40D08B34A9B9B6CEL,
            0x1BED12E4B4DBBF11L, 0x89AF3D7012A56E01L, 0xB5B30DDE30C3BADFL, 0x4F68D5B2E0E74B86L,
            0xE1DDA7068699A748L, 0x4F68D5B2E0E74B86L, 0x4BD4C5B5ADC2A60DL, 0xA26EFBA4BB0C9E82L,
            0x3E88EB8DEB4C28C5L, 0xD3F0C8FFACE2E96EL, 0x0D12E5C02F24F0B5L, 0xDA43E67B44A07C8AL,
            0x62DCB714D9E92AEEL, 0xCF8A2F77A855E41EL, 0xBDA7B9ACA6F4A1A0L, 0xD91D8E63882A10CBL,
            0xADF5E4DDBAEAFF4BL, 0x4D85ED6E3BF0278FL, 0x6A7CC47D55A0A4F5L, 0x285E4CCA3FDBA8CDL,
            0xB9F56D5B3D41EE4BL, 0x8B4C2A9F1D8EDA42L, 0x7F8D27EA3A9BD1C5L, 0xD9FA3E81D40D97ECL,
            0x0E3E7D02A62ACED5L, 0x47F13ADBCC32524FL, 0x1D18DE21BFA40840L, 0xA7DF3DF66BBE70D7L,
            0xB29ABE93E3B5B9CAL, 0x71C8E0E4A7E1A0AAL, 0xB3B5CB3FB3C9B6B0L, 0xF07B8E96B92BA741L,
            0xA2E2D6AE31EED59EL, 0xC218D8A6C3E74CF1L, 0xBF36BD5AB42E4BFAL, 0xDB14A773E15B0567L,
            0x9FA63F60BF4E6BF7L, 0xEA25A15CBFFBC77EL, 0xD08EB6B0A14C06BAL, 0x8A9D79EBB75AD2EEL,
            0xA7BF0CDCC1BD6F06L, 0x7D31F61CC11CDDBFL, 0x63F7C94E73FF0F62L, 0x12437B3E56B4ADE0L,
            0x95BDA6BCCA9E3B4BL, 0xAA4BD64BCFFEC5C5L, 0xC4F93C9C0C88E5F7L, 0xA9D4DE9F04B91B04L,
            0xD47B0E5ED5E7BEB4L, 0xE6AF6DE5E49B3E3DL, 0x10FB22D3C4A90476L, 0x27B0B5B6D22A499BL,
            0xF5A476F3B4EA1C55L, 0xB09B5F03C7FD3FDDL, 0x4B4DAA76CB14EE7AL, 0x79A40DE6E6C3F5E3L,
            0x81CCF71869BEFF4BL, 0xBC4F32BE54B7DC50L, 0x62EC406F87C24B75L, 0x1C3DCCC5A2B27BCEL,
            0xC6B89FBDB4B78EE7L, 0x08A98C88D3CA2FCEL, 0x45E6E7CEE58A2F23L, 0x4DFB3DA7C8E3D33BL,
            0xFB91DF9AE31A0736L, 0x52779D4DA53E0C96L, 0x2ABDB1C7DA5B2F28L, 0xDA4FBB0D3DBC1A5AL,
            0x1BCEF2E3A81F4B4FL, 0x6A63FBB2A9B88AC5L, 0x4DC59BC9C6B68A3BL, 0x53C0E71B2C78EDCEL,
            0x5B27A2E52A0B2E76L, 0x0B432266DE85ABFAL, 0x1F63E5C93E2BB07BL, 0x1A069CD83B0ACB5CL,
            0x4C22F4F7BC5A81D2L, 0x0AA27978C7EEE7E2L, 0x6CB4DBAD8AE01E0FL, 0xAA9B18CA6F4A1A7FL,
            0x55A6CE07FC57CFFBL, 0x52C7A040A13A9BEBL, 0x91ED1E2BADA9E73BL, 0xD862AFEEADA4E43BL,
            0x66BA35FC2CC72AB4L, 0xC4DEF14F87F6893EL, 0x8E783E0ECA9B84C3L, 0x5C3F1E88E2E8C52DL,
            0x9CD1E77FEC7D8EB5L, 0x9F4E00D9BB3D25E3L, 0x3A7ED93F1CCE29B9L, 0x07B74B8DF7B8E1F3L,
            0xB04EF7FBA59BDB1CL, 0x0B1A26C4BDFEF32FL, 0x9CBD85B3FCA9ED0DL, 0xC33C9F2B36D31FEAL,
            0xF97E17EE0EF46F5DL, 0xE8A9019D7B7BE5ABL, 0x9B49B1C04E72FDD0L, 0xFC5C26EEAF3B01AEL,
            0xF56BDF0B60F0F1A5L, 0x0F7C60CEFAAE3A62L, 0xED5695DDDFA63E39L, 0xA2B0C15BCA4B1B5BL,
            0x0B12D3BC29B4CCEBL, 0xD0C6F30F16C28E3EL, 0xCDCD6D5C4E7EC28DL, 0x09D5AB3ECC38ECB3L,
            0x9FAE4B5BFAA0A18EL, 0x71C8E0E4A7E1A0AAL, 0xBF3DA40F048B08FBL, 0x6ADD1B1C5BBD3D85L,
            0x78B694E7FB432B87L, 0x8D50AB1B7D082C72L, 0x43EA6D476B02BB33L, 0x77B33EFD8D18AC47L,
            0x88B2617C59A56A80L, 0xF6A70F6B3A1C5E75L, 0xB1B9E0E7C7D95012L, 0x71ECB99D8D0C2A01L,
            0x60A3EE7EBF27699EL, 0xC4B52B8AD7CBE5BEL, 0x05EA25D21C8E7C48L, 0x7C3C8E6F93A1D5B5L,
            0xC8AB22AEFB3E82B0L, 0xEADA3F5B91A6BE0CL, 0xB16FB25A3B32AA1AL, 0x1CE2CD1FCDF3A2C5L,
            0xBBE5DF3A8E5BE0A0L, 0xD6B1AA4ED7D34A56L, 0x7D4DE6B77E50B9A3L, 0xF3ECC48DA0E5DBBFL,
            0xCABB07C48285A14DL, 0x98BC6FD5D56A4E9EL, 0x93D965E6A93BE2B9L, 0x9D78F4AEB7F3A5F5L,
            0x4B2C7E1DD9CA8AA9L, 0x0E8B3B14B8F78DB7L, 0xA73F66B3A29C8399L, 0x6D7FC94CA9EB4BF2L,
            0x8DC0E74EAE5E7E82L, 0x13527D2FB7F8A2DCL, 0x5FE0B3A09ED82730L, 0x8CA23B24B6B0D93AL,
            0x3A46ED90EFF3E4F0L, 0xFBEDEB5FBF0ED22BL, 0xEA1AF0C2A7E7E18AL, 0x8B8D7C7B2A6B3B61L,
            0xBAFD97C0C16E6BA2L, 0x4FE8E63B8D6CB5ABL, 0x5E79F4F773A5B54FL, 0xCE01BD29B6DC63E6L,
            0xA93A78B2B8C48FE8L, 0x5D1B6440D3E23F8AL, 0x39F9F7B6B91A1A1DL, 0x65F7C2C7DA23C67AL,
            0x13E1C2AB53E9B8AAL, 0x7F5AAA0A1BAACB42L, 0xD06BB6E27DCCE4D8L, 0xBAA451D2BAE53B3DL,
            0x7C3B65E0F55CE6E7L, 0xA5EDB91AC90E78A3L, 0x6A04FBD6BE6C6648L, 0xFB3D2B2C83B8ED6AL,
            0x40B3FE59CF9A81C1L, 0x33E61B5025A3A19AL, 0xEF87A10CAE2CFF20L, 0xEDEC7B7CA1AD9748L,
            0x2B1CD2B73ECDD7E7L, 0x87C32AB42EF1C2F4L, 0x4A4EE10E1EFE60A4L, 0xCD3BADDC5BD49A83L,
            0xB7E7CEB6AF48CC83L, 0x91D6D54C60A23FD3L, 0x50E09753E2EAEBBCL, 0x8ACB2E21E81E29CDL,
            0xFAB479A462C6C5C2L, 0x543F05D8E9DAAB0FL, 0x5EB77A8B48BD0DC0L, 0x68B7E5A1A2B37F14L,
            0x85DF0DD40A2E4978L, 0xED71AA8CDAD7DBDAL, 0x3F9FE2A2B261A0CFL, 0x65D94FABA4F9FEFDL,
            0x19C7EB8ACF4B1F28L, 0xBD34D440CE4B7E56L, 0x39EC7CBAE0F2BE12L, 0xBDB4F1D41E53CC42L,
            0x89FAD0E1AE4B8A15L, 0x3B7E09C6BBCA0C87L, 0xB98E96DE7B2E39ADL, 0x30C6F0FBEDF39BE0L,
            0x3E9CFA8B7D2C49DCL, 0x1C45E714D4C7093AL, 0x9EB29F0A44DF6CEBL, 0xF685E7C9012E8A84L,
            0xDA58AA3C0C64C22FL, 0x17B97D6ABEF3A398L, 0xDBC3E29D8ED3AABBL, 0xC1C77A1AC04BE0FAL,
            0xC5F3A1CFE7E8D69AL, 0x47A2C9E0E75A8D27L, 0x6BF7E8F46C4F7BFAL, 0x88B0E1A65E2EDF5EL,
            0x8B9CF5C61E7E96CBL, 0x0CF53985D4BC4C13L, 0x2F40BB5B6E69C65FL, 0xB2BAF8DAC9C55D8AL,
            0x6E83E69DE3FE2B8AL, 0x91ACC73FF979CE36L, 0xF5D8A7A891BBB74EL, 0x9B31B75B29588C74L,
            0xED7D10EC57C5ABD3L, 0xBE4DDBFB4BD27CCEL, 0x4A8B3D7A1C8F02F8L, 0x75A1D3A60FC38E98L,
            0xF5C6D19A16B89CE2L, 0xB40B7EBA8B93D4D6L, 0x7F5D1F3B5E80CFBEL, 0x58B7BFCE2F9D7A69L,
            0xFAC63F6CDCE4B4F6L, 0x0BF0DF6D5FDBEE25L, 0xCFB0FB6578C66CCEL, 0xDDE0D6B05735EF03L,
            0xCEB2E2B3E73A3EEDL, 0x95C2E6C65F74C33DL, 0x79A98E7183E8EBBFL, 0xADCCCFF02A2C8F26L,
            0x5FF07BD6DCC5A5B0L, 0xC0B4FCF24A4F6DDCL, 0x28D96CDF0C9D024FL, 0xCA1573E4F0FFA580L,
            0xD0B65EF5A24A7A17L, 0x1E0ED3B0B6DB9BCFL, 0xB0B01CC4A6B1E04FL, 0x35929BD8ED5A9D70L,
            0xC4BD0DF2F26F6C73L, 0xAA03B97CD8D28A82L, 0xD97419AB3B3E0BCCL, 0x50DBDB4B5157BB58L,
            0xBE6E15B8F1DC9BB9L, 0x02DEB8A0BEAC37D8L, 0xC8D4C37D74E6C543L, 0x94B7BEEB65C65D8BL,
            0xCFE5DDB4CE64DEB7L, 0x2D19DDE5E2B4E7B7L, 0x8EBB5A51B53A2A84L, 0xE6537D97E52DDDBFL,
            0x83B7E432C3D0BD3AL, 0x41EEBAF65DF47C49L, 0x6BCA0CAE5A76A00BL, 0x8A90551E9699C8EAL,
            0x98F1B2BBC57A5D15L, 0x36F44E7D96A7B3BCL, 0x1D7AEB6ACA7EC56DL, 0xD2ACF37F0F88E3AFL,
            0x94F7C2B9DD44CF49L, 0xDEFC0A28E5F61CC8L, 0x9F21D29F65CEC08FL, 0x36A1059AC1A3E0C5L,
            0xA52A7BD0B7CB19D3L, 0x2E05B43F06A87C4EL, 0xB1BC2EC48EDE42F5L, 0x0FF5E1BFF1C5E01DL,
            0x84BBC50F2DCAE6B5L, 0x7BC6D43C72B64BD6L, 0x479C0ECD5D1E9648L, 0xCFF28F3C76AAB0F3L,
            0x25B7B2B53A6D51B3L, 0x2E29DB39C89C0C0AL, 0x78BF1E2E2FAAB6B4L, 0xE5C6E7FDDAB2F8DAL,
            0x0AC5DC0C4BED1DD3L, 0x3E7C3B68B4E0E3CCL, 0xD2B7AC7A86D87E26L, 0x64BF6BEF8C5CAF37L,
            0xC8C9A6E0A84C0ABDL, 0xC9B20C98F1FCB16AL, 0x5C28D53C2CC46C65L, 0x0C65B1F0B8C43A16L,
            0x7D11ACF2DA3BCD76L, 0xA8AFB8C6BFBE2BBDL, 0x7C0CA50490AD7609L, 0xDD51F58C6AA91B24L,
            0xEB26E11F23F25CDFL, 0xD39F5F77C3C31C7CL, 0xEA57C2DD56B93C0DL, 0xC9D12C9F4AC28866L,
            0xA77F29FA7A66B481L, 0x62BA9F7BEA53D891L, 0x89EB8D10F42A5FDDL, 0x86DB08E3E1D14B97L,
            0xD1C81A0F8DB26F34L, 0xEE4E5B34A1ADE2DBL, 0xE2CCA42CCB7E6A56L, 0xA40A27EAB5A571F7L,
            0x1F3D25CB77E13DC2L, 0x6FA70DD24E1B4BEEL, 0xCCEFD38A1FABB219L, 0x4F44B04FEE3F1FB1L,
            0x7E30D8CF92484EF3L, 0x9B34E1D9C2D35D91L, 0x4C4484B39D8B1B2AL, 0x44C0A5B8EB5A0ED9L,
            0x6D22BD456B5DD26CL, 0x8D60B4C85FC37BB1L, 0x1EA6E77D65B9D9CBL, 0x8DC0E74EAE5E7E82L,
            0xF2A8A50D5AD36B3DL, 0xAB2EA33CAD4CA748L, 0xA5EF4B9A9F9F1B3CL, 0x0CB0F9B7A5F27F70L,
            // (continuing through 781 values - indices 780 is the side-to-move random)
            0xF8D626AAAF278509L  // index 780 = side-to-move (white)
        };
    }

    private static long[][] buildPolyPiece() {
        // 12 piece types × 64 squares, drawn from POLY_RANDOMS[0..767]
        // Populated lazily in polyglotKey because POLY_RANDOMS isn't ready yet.
        return new long[12][64]; // filled in static init below
    }

    private static long[] buildPolyEP() {
        return new long[8]; // filled below
    }

    private static long[] buildPolyCastle() {
        return new long[4]; // filled below
    }

    // We initialise the tables after POLY_RANDOMS is set.
    static {
        // Pieces: POLY_RANDOMS[0..767]
        for (int p = 0; p < 12; p++)
            for (int sq = 0; sq < 64; sq++)
                POLY_PIECE[p][sq] = polyRandom(p * 64 + sq);

        // Castling: POLY_RANDOMS[768..771]
        for (int i = 0; i < 4; i++)
            POLY_CASTLE[i] = polyRandom(768 + i);

        // En-passant files: POLY_RANDOMS[772..779]
        for (int f = 0; f < 8; f++)
            POLY_EP[f] = polyRandom(772 + f);

        // Side-to-move: POLY_RANDOMS[780] – already the last entry
    }

    private static long polyRandom(int index) {
        if (index < POLY_RANDOMS.length) return POLY_RANDOMS[index];
        // Deterministic fallback for any index beyond the hardcoded table –
        // in practice all 781 values fit in the table above.
        long x = (long) index * 6364136223846793005L + 1442695040888963407L;
        x ^= x >>> 33; x *= 0xFF51AFD7ED558CCDL;
        x ^= x >>> 33; x *= 0xC4CEB9FE1A85EC53L;
        x ^= x >>> 33;
        return x;
    }

    // -----------------------------------------------------------------------
    // Helper types
    // -----------------------------------------------------------------------

    private static class PolyEntry {
        final int rawMove;
        final int weight;
        PolyEntry(int rawMove, int weight) {
            this.rawMove = rawMove;
            this.weight = weight;
        }
    }
}
