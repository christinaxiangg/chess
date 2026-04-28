
package UI;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import piece.*;

/**
 * Renders chess pieces using actual SVG files from the Cburnett piece set (Lichess style).
 * This provides elegant, professional-looking piece icons by loading SVG files directly.
 */
public class CburnettSVGRenderer {
    private static final int PIECE_SIZE = 80;
    private static final String CBURENETT_PATH = "src/piece/cburnett/";

    // Cache for parsed SVG paths
    private static Map<String, List<SVGElement>> pieceCache = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Initialize the piece renderer by loading all SVG files
     */
    public static synchronized void initialize() {
        if (initialized) return;

        // Load all piece SVGs
        String[] pieces = {"wP", "wN", "wB", "wR", "wQ", "wK", 
                          "bP", "bN", "bB", "bR", "bQ", "bK"};
        for (String piece : pieces) {
            loadPieceSVG(piece);
        }

        initialized = true;
    }

    /**
     * Load and parse an SVG file for a piece
     */
    private static void loadPieceSVG(String pieceCode) {
        try {
            // Try loading from file system with absolute path
            File svgFile = new File(CBURENETT_PATH + pieceCode + ".svg");
            if (!svgFile.exists()) {
                // Try relative path from working directory
                svgFile = new File("src/piece/cburnett/" + pieceCode + ".svg");
            }
            
            if (svgFile.exists()) {
                String svgContent = readFile(svgFile);
                List<SVGElement> elements = parseSVG(svgContent, pieceCode);
                pieceCache.put(pieceCode, elements);
                return;
            }
            
            System.err.println("SVG file not found: " + pieceCode + ".svg (tried: " + CBURENETT_PATH + " and src/piece/cburnett/)");
        } catch (Exception e) {
            System.err.println("Error loading SVG for " + pieceCode + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Read file content
     */
    private static String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }
    
    /**
     * Read content from InputStream
     */
    private static String readFromStream(InputStream is) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    /**
     * Parse SVG content into a list of SVG elements
     */
    private static List<SVGElement> parseSVG(String svgContent, String pieceCode) {
        List<SVGElement> elements = new ArrayList<>();

        // Determine default fill color based on piece code
        boolean isBlackPiece = pieceCode.startsWith("b");
        Color defaultFill = isBlackPiece ? Color.BLACK : Color.WHITE;

        // Extract all path elements, including those inside <g> groups
        Pattern pathPattern = Pattern.compile("<path[^>]*>");
        Matcher pathMatcher = pathPattern.matcher(svgContent);

        // Extract group-level attributes (fill, stroke, stroke-width, stroke-linecap, stroke-linejoin)
        String groupFill = null;
        String groupStroke = null;
        String groupStrokeWidth = null;
        String groupStrokeLinecap = null;
        String groupStrokeLinejoin = null;
        
        Pattern groupPattern = Pattern.compile("<g[^>]*>");
        Matcher groupMatcher = groupPattern.matcher(svgContent);
        while (groupMatcher.find()) {
            String groupElement = groupMatcher.group();
            String fill = extractAttribute(groupElement, "fill");
            String stroke = extractAttribute(groupElement, "stroke");
            String strokeWidth = extractAttribute(groupElement, "stroke-width");
            String strokeLinecap = extractAttribute(groupElement, "stroke-linecap");
            String strokeLinejoin = extractAttribute(groupElement, "stroke-linejoin");

            System.out.println("Found group element: " + groupElement);
            System.out.println("Group fill: " + fill + ", Group stroke: " + stroke);

            // Use the most recent non-null values (inner groups override outer groups)
            if (fill != null) groupFill = fill;
            if (stroke != null) groupStroke = stroke;
            if (strokeWidth != null) groupStrokeWidth = strokeWidth;
            if (strokeLinecap != null) groupStrokeLinecap = strokeLinecap;
            if (strokeLinejoin != null) groupStrokeLinejoin = strokeLinejoin;
        }

        System.out.println("Final group fill: " + groupFill + ", Final group stroke: " + groupStroke);

        while (pathMatcher.find()) {
            String pathElement = pathMatcher.group();
            SVGElement element = new SVGElement();

            System.out.println("Found path element: " + pathElement);

            // Extract fill (use path-level if available, otherwise group-level, otherwise default)
            String fill = extractAttribute(pathElement, "fill");
            if (fill == null) {
                fill = groupFill;
            }
            if (fill == null) {
                // No fill specified at path or group level, use default based on piece color
                element.fill = defaultFill;
                System.out.println("Using default fill for path: " + defaultFill);
            } else if (!fill.equals("none")) {
                element.fill = parseColor(fill);
                System.out.println("Using fill from attribute: " + fill);
            } else {
                System.out.println("Fill is 'none', not filling");
            }
            // If fill is "none", element.fill remains null (no fill)

            // Extract stroke (use path-level if available, otherwise group-level)
            String stroke = extractAttribute(pathElement, "stroke");
            if (stroke == null) {
                stroke = groupStroke;
            }
            if (stroke != null && !stroke.equals("none")) {
                element.stroke = parseColor(stroke);
            }

            // Extract stroke width (use path-level if available, otherwise group-level)
            String strokeWidth = extractAttribute(pathElement, "stroke-width");
            if (strokeWidth == null) {
                strokeWidth = groupStrokeWidth;
            }
            if (strokeWidth != null) {
                element.strokeWidth = Float.parseFloat(strokeWidth);
            }

            // Extract stroke-linecap (use path-level if available, otherwise group-level)
            String strokeLinecap = extractAttribute(pathElement, "stroke-linecap");
            if (strokeLinecap == null) {
                strokeLinecap = groupStrokeLinecap;
            }
            if (strokeLinecap != null) {
                switch (strokeLinecap) {
                    case "butt": element.strokeCap = BasicStroke.CAP_BUTT; break;
                    case "round": element.strokeCap = BasicStroke.CAP_ROUND; break;
                    case "square": element.strokeCap = BasicStroke.CAP_SQUARE; break;
                }
            }

            // Extract stroke-linejoin (use path-level if available, otherwise group-level)
            String strokeLinejoin = extractAttribute(pathElement, "stroke-linejoin");
            if (strokeLinejoin == null) {
                strokeLinejoin = groupStrokeLinejoin;
            }
            if (strokeLinejoin != null) {
                switch (strokeLinejoin) {
                    case "miter": element.strokeJoin = BasicStroke.JOIN_MITER; break;
                    case "round": element.strokeJoin = BasicStroke.JOIN_ROUND; break;
                    case "bevel": element.strokeJoin = BasicStroke.JOIN_BEVEL; break;
                }
            }

            // Extract path data
            String d = extractAttribute(pathElement, "d");
            if (d != null) {
                System.out.println("Path data: " + d);
                element.path = parseSVGPath(d);
                elements.add(element);
            }
        }

        // Extract all circle elements
        Pattern circlePattern = Pattern.compile("<circle[^>]*>");
        Matcher circleMatcher = circlePattern.matcher(svgContent);
        while (circleMatcher.find()) {
            String circleElement = circleMatcher.group();
            SVGElement element = new SVGElement();

            System.out.println("Found circle element: " + circleElement);

            // Extract fill (use circle-level if available, otherwise group-level, otherwise default)
            String fill = extractAttribute(circleElement, "fill");
            if (fill == null) {
                fill = groupFill;
            }
            if (fill == null) {
                // No fill specified at circle or group level, use default based on piece color
                element.fill = defaultFill;
            } else if (!fill.equals("none")) {
                element.fill = parseColor(fill);
            }
            // If fill is "none", element.fill remains null (no fill)

            // Extract stroke (use circle-level if available, otherwise group-level)
            String stroke = extractAttribute(circleElement, "stroke");
            if (stroke == null) {
                stroke = groupStroke;
            }
            if (stroke != null && !stroke.equals("none")) {
                element.stroke = parseColor(stroke);
            }

            // Extract stroke width (use circle-level if available, otherwise group-level)
            String strokeWidth = extractAttribute(circleElement, "stroke-width");
            if (strokeWidth == null) {
                strokeWidth = groupStrokeWidth;
            }
            if (strokeWidth != null) {
                element.strokeWidth = Float.parseFloat(strokeWidth);
            }

            // Extract circle attributes and convert to path
            String cx = extractAttribute(circleElement, "cx");
            String cy = extractAttribute(circleElement, "cy");
            String r = extractAttribute(circleElement, "r");
            if (cx != null && cy != null && r != null) {
                double cxVal = Double.parseDouble(cx);
                double cyVal = Double.parseDouble(cy);
                double rVal = Double.parseDouble(r);
                // Convert circle to path using arc commands
                Path2D circlePath = new Path2D.Double();
                circlePath.moveTo(cxVal + rVal, cyVal);
                drawArc(circlePath, cxVal + rVal, cyVal, cxVal - rVal, cyVal, rVal, rVal, 0, false, false);
                drawArc(circlePath, cxVal - rVal, cyVal, cxVal + rVal, cyVal, rVal, rVal, 0, false, false);
                element.path = circlePath;
                elements.add(element);
            }
        }

        return elements;
    }

    /**
     * Extract an attribute value from an SVG element
     */
    private static String extractAttribute(String element, String attrName) {
        Pattern pattern = Pattern.compile(attrName + "=\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(element);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Parse color string
     */
    private static Color parseColor(String colorStr) {
        if (colorStr.startsWith("#")) {
            String hex = colorStr.substring(1);
            // Handle 3-digit hex colors (#fff -> #ffffff)
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0) + 
                       hex.charAt(1) + hex.charAt(1) + 
                       hex.charAt(2) + hex.charAt(2);
            }
            int rgb = Integer.parseInt(hex, 16);
            return new Color(rgb);
        }
        return Color.BLACK;
    }

    /**
     * Parse SVG path data
     */
    private static Path2D parseSVGPath(String pathData) {
        Path2D path = new Path2D.Double();

        // Remove extra whitespace and normalize
        pathData = pathData.replaceAll("\\s+", " ").trim();

        String[] tokens = pathData.split("(?=[MmLlHhVvCcSsQqTtAaZz])");
        double lastX = 0, lastY = 0;
        double subpathStartX = 0, subpathStartY = 0;  // Start of current subpath for Z command
        double lastCPX = 0, lastCPY = 0;  // Last control point from C/S commands
        boolean lastWasCOrS = false;  // Whether last command was C, c, S, or s
        boolean lastWasQOrT = false;  // Whether last command was Q, q, T, or t

        for (String token : tokens) {
            if (token.isEmpty()) continue;

            char command = token.charAt(0);
            String params = token.substring(1).trim();

            if (params.isEmpty() && command != 'Z' && command != 'z') continue;

            // Parse numbers, handling negative values correctly
            // This regex handles numbers like .5, 0.5, -0.5, 5, -5
            List<Double> valuesList = new ArrayList<>();
            Pattern numberPattern = Pattern.compile("-?\\d*\\.?\\d+");
            Matcher numberMatcher = numberPattern.matcher(params);
            while (numberMatcher.find()) {
                String numStr = numberMatcher.group();
                valuesList.add(Double.parseDouble(numStr));
            }
            double[] values = valuesList.stream().mapToDouble(d -> d).toArray();
            
            if (command == 'a' || command == 'A') {
                System.out.println("Arc command: " + command + ", params: " + params);
                System.out.println("Parsed values: " + Arrays.toString(values));
            }

            switch (command) {
                case 'M':
                    lastWasCOrS = false;
                    lastWasQOrT = false;
                    if (values.length >= 2) {
                        path.moveTo(values[0], values[1]);
                        lastX = values[0];
                        lastY = values[1];
                        subpathStartX = values[0];
                        subpathStartY = values[1];
                        // Handle additional coordinate pairs
                        for (int i = 2; i <= values.length - 2; i += 2) {
                            path.lineTo(values[i], values[i + 1]);
                            lastX = values[i];
                            lastY = values[i + 1];
                        }
                    }
                    break;
                case 'm':
                    lastWasCOrS = false;
                    lastWasQOrT = false;
                    if (values.length >= 2) {
                        lastX += values[0];
                        lastY += values[1];
                        path.moveTo(lastX, lastY);
                        subpathStartX = lastX;
                        subpathStartY = lastY;
                        // Handle additional coordinate pairs
                        for (int i = 2; i <= values.length - 2; i += 2) {
                            lastX += values[i];
                            lastY += values[i + 1];
                            path.lineTo(lastX, lastY);
                        }
                    }
                    break;
                case 'L':
                    lastWasCOrS = false;
                    lastWasQOrT = false;
                    for (int i = 0; i <= values.length - 2; i += 2) {
                        path.lineTo(values[i], values[i + 1]);
                        lastX = values[i];
                        lastY = values[i + 1];
                    }
                    break;
                case 'l':
                    lastWasCOrS = false;
                    lastWasQOrT = false;
                    for (int i = 0; i <= values.length - 2; i += 2) {
                        lastX += values[i];
                        lastY += values[i + 1];
                        path.lineTo(lastX, lastY);
                    }
                    break;
                case 'H':
                    lastWasCOrS = false;
                    lastWasQOrT = false;
                    for (double value : values) {
                        path.lineTo(value, lastY);
                        lastX = value;
                    }
                    break;
                case 'h':
                    lastWasCOrS = false;
                    lastWasQOrT = false;
                    for (double value : values) {
                        lastX += value;
                        path.lineTo(lastX, lastY);
                    }
                    break;
                case 'V':
                    lastWasCOrS = false;
                    lastWasQOrT = false;
                    for (double value : values) {
                        path.lineTo(lastX, value);
                        lastY = value;
                    }
                    break;
                case 'v':
                    lastWasCOrS = false;
                    lastWasQOrT = false;
                    for (double value : values) {
                        lastY += value;
                        path.lineTo(lastX, lastY);
                    }
                    break;
                case 'C':
                    for (int i = 0; i < values.length - 5; i += 6) {
                        path.curveTo(values[i], values[i + 1],
                                   values[i + 2], values[i + 3],
                                   values[i + 4], values[i + 5]);
                        lastCPX = values[i + 2];
                        lastCPY = values[i + 3];
                        lastX = values[i + 4];
                        lastY = values[i + 5];
                        lastWasCOrS = true;
                    }
                    break;
                case 'c':
                    for (int i = 0; i < values.length - 5; i += 6) {
                        path.curveTo(lastX + values[i], lastY + values[i + 1],
                                   lastX + values[i + 2], lastY + values[i + 3],
                                   lastX + values[i + 4], lastY + values[i + 5]);
                        lastCPX = lastX + values[i + 2];
                        lastCPY = lastY + values[i + 3];
                        lastX += values[i + 4];
                        lastY += values[i + 5];
                        lastWasCOrS = true;
                    }
                    break;
                case 'S':
                    for (int i = 0; i < values.length - 3; i += 4) {
                        // First control point is reflection of second control point of previous C/S command
                        double cp1x, cp1y;
                        if (lastWasCOrS) {
                            cp1x = 2 * lastX - lastCPX;
                            cp1y = 2 * lastY - lastCPY;
                        } else {
                            cp1x = lastX;
                            cp1y = lastY;
                        }
                        path.curveTo(
                            cp1x, cp1y,
                            values[i], values[i + 1],
                            values[i + 2], values[i + 3]
                        );
                        lastCPX = values[i];
                        lastCPY = values[i + 1];
                        lastX = values[i + 2];
                        lastY = values[i + 3];
                        lastWasCOrS = true;
                    }
                    break;
                case 's':
                    for (int i = 0; i < values.length - 3; i += 4) {
                        // First control point is reflection of second control point of previous C/S command
                        double cp1x, cp1y;
                        if (lastWasCOrS) {
                            cp1x = 2 * lastX - lastCPX;
                            cp1y = 2 * lastY - lastCPY;
                        } else {
                            cp1x = lastX;
                            cp1y = lastY;
                        }
                        path.curveTo(
                            cp1x, cp1y,
                            lastX + values[i], lastY + values[i + 1],
                            lastX + values[i + 2], lastY + values[i + 3]
                        );
                        lastCPX = lastX + values[i];
                        lastCPY = lastY + values[i + 1];
                        lastX += values[i + 2];
                        lastY += values[i + 3];
                        lastWasCOrS = true;
                    }
                    break;
                case 'Z':
                case 'z':
                    lastWasCOrS = false;
                    lastWasQOrT = false;
                    path.closePath();
                    // Reset current position to start of subpath
                    lastX = subpathStartX;
                    lastY = subpathStartY;
                    break;
                case 'Q':
                    for (int i = 0; i < values.length - 3; i += 4) {
                        path.quadTo(values[i], values[i + 1],
                                   values[i + 2], values[i + 3]);
                        lastCPX = values[i];
                        lastCPY = values[i + 1];
                        lastX = values[i + 2];
                        lastY = values[i + 3];
                        lastWasQOrT = true;
                        lastWasCOrS = false;
                    }
                    break;
                case 'q':
                    for (int i = 0; i < values.length - 3; i += 4) {
                        path.quadTo(lastX + values[i], lastY + values[i + 1],
                                   lastX + values[i + 2], lastY + values[i + 3]);
                        lastCPX = lastX + values[i];
                        lastCPY = lastY + values[i + 1];
                        lastX += values[i + 2];
                        lastY += values[i + 3];
                        lastWasQOrT = true;
                        lastWasCOrS = false;
                    }
                    break;
                case 'T':
                    for (int i = 0; i <= values.length - 2; i += 2) {
                        // First control point is reflection of previous Q command's control point
                        double cp1x, cp1y;
                        if (lastWasQOrT) {
                            cp1x = 2 * lastX - lastCPX;
                            cp1y = 2 * lastY - lastCPY;
                        } else {
                            cp1x = lastX;
                            cp1y = lastY;
                        }
                        path.quadTo(cp1x, cp1y, values[i], values[i + 1]);
                        lastCPX = cp1x;
                        lastCPY = cp1y;
                        lastX = values[i];
                        lastY = values[i + 1];
                        lastWasQOrT = true;
                        lastWasCOrS = false;
                    }
                    break;
                case 't':
                    for (int i = 0; i <= values.length - 2; i += 2) {
                        // First control point is reflection of previous Q command's control point
                        double cp1x, cp1y;
                        if (lastWasQOrT) {
                            cp1x = 2 * lastX - lastCPX;
                            cp1y = 2 * lastY - lastCPY;
                        } else {
                            cp1x = lastX;
                            cp1y = lastY;
                        }
                        path.quadTo(cp1x, cp1y, lastX + values[i], lastY + values[i + 1]);
                        lastCPX = cp1x;
                        lastCPY = cp1y;
                        lastX += values[i];
                        lastY += values[i + 1];
                        lastWasQOrT = true;
                        lastWasCOrS = false;
                    }
                    break;
                case 'A':
                    for (int i = 0; i < values.length - 6; i += 7) {
                        double rx = values[i];
                        double ry = values[i + 1];
                        double rotation = values[i + 2];
                        boolean largeArc = values[i + 3] != 0;
                        boolean sweep = values[i + 4] != 0;
                        double endX = values[i + 5];
                        double endY = values[i + 6];
                        drawArc(path, lastX, lastY, endX, endY, rx, ry, rotation, largeArc, sweep);
                        lastX = endX;
                        lastY = endY;
                        lastWasCOrS = false;
                        lastWasQOrT = false;
                    }
                    break;
                case 'a':
                    for (int i = 0; i < values.length - 6; i += 7) {
                        double rx = values[i];
                        double ry = values[i + 1];
                        double rotation = values[i + 2];
                        boolean largeArc = values[i + 3] != 0;
                        boolean sweep = values[i + 4] != 0;
                        double endX = lastX + values[i + 5];
                        double endY = lastY + values[i + 6];
                        drawArc(path, lastX, lastY, endX, endY, rx, ry, rotation, largeArc, sweep);
                        lastX = endX;
                        lastY = endY;
                        lastWasCOrS = false;
                        lastWasQOrT = false;
                    }
                    break;
            }
        }

        return path;
    }

    /**
     * Draw an elliptical arc segment
     */
    private static void drawArc(Path2D path, double x1, double y1, double x2, double y2,
                                 double rx, double ry, double phi, boolean largeArc, boolean sweep) {
        System.out.println("Drawing arc: (" + x1 + "," + y1 + ") -> (" + x2 + "," + y2 + "), rx=" + rx + ", ry=" + ry + ", phi=" + phi + ", largeArc=" + largeArc + ", sweep=" + sweep);
        // Handle degenerate cases
        if (rx == 0 || ry == 0) {
            path.lineTo(x2, y2);
            return;
        }

        // Convert to center parameterization
        double cosPhi = Math.cos(phi * Math.PI / 180.0);
        double sinPhi = Math.sin(phi * Math.PI / 180.0);

        double dx = (x1 - x2) / 2.0;
        double dy = (y1 - y2) / 2.0;
        double x1p = cosPhi * dx + sinPhi * dy;
        double y1p = -sinPhi * dx + cosPhi * dy;

        // Correct out-of-range radii
        double rxsq = rx * rx;
        double rysq = ry * ry;
        double x1psq = x1p * x1p;
        double y1psq = y1p * y1p;
        double lambda = x1psq / rxsq + y1psq / rysq;
        if (lambda > 1.0) {
            double sqrtLambda = Math.sqrt(lambda);
            rx *= sqrtLambda;
            ry *= sqrtLambda;
            rxsq = rx * rx;
            rysq = ry * ry;
        }

        // Compute center point
        double factor = (rxsq * rysq - rxsq * y1psq - rysq * x1psq) /
                      (rxsq * y1psq + rysq * x1psq);
        factor = Math.max(0, factor);
        double sq = Math.sqrt(factor);
        if (largeArc == sweep) sq = -sq;

        double cxp = sq * rx * y1p / ry;
        double cyp = -sq * ry * x1p / rx;

        double cx = cosPhi * cxp - sinPhi * cyp + (x1 + x2) / 2.0;
        double cy = sinPhi * cxp + cosPhi * cyp + (y1 + y2) / 2.0;

        // Compute angles
        double theta1 = angle(1.0, 0.0, (x1p - cxp) / rx, (y1p - cyp) / ry);
        double dtheta = angle((x1p - cxp) / rx, (y1p - cyp) / ry,
                            (-x1p - cxp) / rx, (-y1p - cyp) / ry);

        if (!sweep && dtheta > 0) {
            dtheta -= 2 * Math.PI;
        } else if (sweep && dtheta < 0) {
            dtheta += 2 * Math.PI;
        }

        // Draw the arc using multiple curve segments for accuracy
        int segments = Math.max(1, (int)(Math.abs(dtheta) * 2 / Math.PI));
        double delta = dtheta / segments;

        for (int i = 0; i < segments; i++) {
            double t1 = theta1 + i * delta;
            double t2 = theta1 + (i + 1) * delta;
            drawArcSegment(path, cx, cy, rx, ry, phi, t1, t2);
        }
    }

    /**
     * Draw a single arc segment using a cubic Bezier curve approximation
     */
    private static void drawArcSegment(Path2D path, double cx, double cy, double rx, double ry,
                                      double phi, double theta1, double theta2) {
        double cosPhi = Math.cos(phi * Math.PI / 180.0);
        double sinPhi = Math.sin(phi * Math.PI / 180.0);

        // Calculate start and end points
        double x1 = cx + rx * Math.cos(theta1) * cosPhi - ry * Math.sin(theta1) * sinPhi;
        double y1 = cy + rx * Math.cos(theta1) * sinPhi + ry * Math.sin(theta1) * cosPhi;
        double x2 = cx + rx * Math.cos(theta2) * cosPhi - ry * Math.sin(theta2) * sinPhi;
        double y2 = cy + rx * Math.cos(theta2) * sinPhi + ry * Math.sin(theta2) * cosPhi;

        // Calculate control points for Bezier approximation
        double dtheta = theta2 - theta1;
        double t = Math.tan(dtheta / 4) * 4 / 3;
        double alpha1 = Math.atan2(ry * Math.sin(theta1), rx * Math.cos(theta1));
        double alpha2 = Math.atan2(ry * Math.sin(theta2), rx * Math.cos(theta2));

        double cp1x = x1 - t * rx * Math.sin(alpha1) * cosPhi - t * ry * Math.cos(alpha1) * sinPhi;
        double cp1y = y1 - t * rx * Math.sin(alpha1) * sinPhi + t * ry * Math.cos(alpha1) * cosPhi;
        double cp2x = x2 + t * rx * Math.sin(alpha2) * cosPhi + t * ry * Math.cos(alpha2) * sinPhi;
        double cp2y = y2 + t * rx * Math.sin(alpha2) * sinPhi - t * ry * Math.cos(alpha2) * cosPhi;

        path.curveTo(cp1x, cp1y, cp2x, cp2y, x2, y2);
    }

    /**
     * Calculate the angle between two vectors
     */
    private static double angle(double ux, double uy, double vx, double vy) {
        double n = Math.sqrt(ux * ux + uy * uy) * Math.sqrt(vx * vx + vy * vy);
        double c = (ux * vx + uy * vy) / n;
        c = Math.max(-1.0, Math.min(1.0, c));
        double cross = ux * vy - uy * vx;
        double angle = Math.acos(c);
        if (cross < 0) {
            angle = -angle;
        }
        return angle;
    }

    /**
     * Draw a piece at the specified position using piece code string with opacity
     */
    public static void drawPiece(Graphics2D g2d, String pieceCode, int x, int y, int size, float opacity) {
        if (!initialized) {
            initialize();
        }

        List<SVGElement> elements = pieceCache.get(pieceCode);

        System.out.println("Drawing piece " + pieceCode + " at (" + x + ", " + y + "), elements: " + (elements != null ? elements.size() : "null"));

        if (elements == null || elements.isEmpty()) {
            // Fallback to EnhancedSVGPieces if SVG not available
            System.out.println("Using fallback for " + pieceCode);
            EnhancedSVGPieces.drawPiece(g2d, pieceCode, x, y, size, opacity);
            return;
        }

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Set opacity
        Composite originalComposite = g2d.getComposite();
        if (opacity < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        }

        // Calculate scale to fit in square (45 is the viewBox size)
        double scale = size / 45.0;

        // Draw each SVG element
        for (SVGElement element : elements) {
            AffineTransform transform = new AffineTransform();
            transform.translate(x, y);
            transform.scale(scale, scale);

            Path2D transformedPath = (Path2D) element.path.createTransformedShape(transform);

            // Fill if specified
            if (element.fill != null) {
                g2d.setColor(element.fill);
                g2d.fill(transformedPath);
            }

            // Stroke if specified
            if (element.stroke != null) {
                g2d.setColor(element.stroke);
                float strokeWidth = element.strokeWidth * (float)scale;
                g2d.setStroke(new BasicStroke(strokeWidth, element.strokeCap, element.strokeJoin));
                g2d.draw(transformedPath);
            }
        }

        // Restore composite
        if (opacity < 1.0f) {
            g2d.setComposite(originalComposite);
        }
    }

    /**
     * Draw a piece at the specified position using piece code string
     */
    public static void drawPiece(Graphics2D g2d, String pieceCode, int x, int y, int size) {
        if (!initialized) {
            initialize();
        }

        List<SVGElement> elements = pieceCache.get(pieceCode);

        if (elements == null || elements.isEmpty()) {
            // Fallback to EnhancedSVGPieces if SVG not available
            EnhancedSVGPieces.drawPiece(g2d, pieceCode, x, y, size);
            return;
        }

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Calculate scale to fit in square (45 is the viewBox size)
        double scale = size / 45.0;

        // Draw each SVG element
        for (SVGElement element : elements) {
            AffineTransform transform = new AffineTransform();
            transform.translate(x, y);
            transform.scale(scale, scale);

            Path2D transformedPath = (Path2D) element.path.createTransformedShape(transform);

            // Fill if specified
            if (element.fill != null) {
                g2d.setColor(element.fill);
                g2d.fill(transformedPath);
            }

            // Stroke if specified
            if (element.stroke != null) {
                g2d.setColor(element.stroke);
                float strokeWidth = element.strokeWidth * (float)scale;
                g2d.setStroke(new BasicStroke(strokeWidth, element.strokeCap, element.strokeJoin));
                g2d.draw(transformedPath);
            }
        }
    }

    /**
     * Draw a piece at the specified position using Piece object
     */
    public static void drawPiece(Graphics2D g2d, Piece piece, int x, int y) {
        if (!initialized) {
            initialize();
        }

        String pieceCode = getPieceCode(piece);
        List<SVGElement> elements = pieceCache.get(pieceCode);

        System.out.println("Drawing piece " + pieceCode + " at (" + x + ", " + y + "), elements: " + (elements != null ? elements.size() : "null"));

        if (elements == null || elements.isEmpty()) {
            // Fallback to text rendering if SVG not available
            System.out.println("Using fallback for " + pieceCode);
            drawFallbackPiece(g2d, piece, x, y);
            return;
        }

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Calculate scale to fit in square (45 is the viewBox size)
        double scale = PIECE_SIZE / 45.0;

        // Draw each SVG element
        for (SVGElement element : elements) {
            AffineTransform transform = new AffineTransform();
            transform.translate(x, y);
            transform.scale(scale, scale);

            Path2D transformedPath = (Path2D) element.path.createTransformedShape(transform);

            // Fill if specified
            if (element.fill != null) {
                g2d.setColor(element.fill);
                g2d.fill(transformedPath);
            }

            // Stroke if specified
            if (element.stroke != null) {
                g2d.setColor(element.stroke);
                float strokeWidth = element.strokeWidth * (float)scale;
                g2d.setStroke(new BasicStroke(strokeWidth, element.strokeCap, element.strokeJoin));
                g2d.draw(transformedPath);
            }
        }
    }

    /**
     * Get piece code for a Piece object
     */
    private static String getPieceCode(Piece piece) {
        String color = piece.isWhite() ? "w" : "b";
        String type = switch (piece.getType()) {
            case PAWN -> "P";
            case KNIGHT -> "N";
            case BISHOP -> "B";
            case ROOK -> "R";
            case QUEEN -> "Q";
            case KING -> "K";
            default -> "P";
        };
        return color + type;
    }

    /**
     * Fallback to text rendering if SVG not available
     */
    private static void drawFallbackPiece(Graphics2D g2d, Piece piece, int x, int y) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String symbol = getPieceSymbol(piece);
        g2d.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 64));

        FontMetrics fm = g2d.getFontMetrics();
        int symbolWidth = fm.stringWidth(symbol);
        int symbolHeight = fm.getAscent();

        int drawX = x + (PIECE_SIZE - symbolWidth) / 2;
        int drawY = y + (PIECE_SIZE + symbolHeight) / 2 - fm.getDescent();

        g2d.setColor(piece.isWhite() ? Color.WHITE : Color.BLACK);
        g2d.drawString(symbol, drawX, drawY);
    }

    /**
     * Get Unicode symbol for a piece
     */
    private static String getPieceSymbol(Piece piece) {
        return switch (piece.getType()) {
            case KING -> piece.isWhite() ? "♔" : "♚";
            case QUEEN -> piece.isWhite() ? "♕" : "♛";
            case ROOK -> piece.isWhite() ? "♖" : "♜";
            case BISHOP -> piece.isWhite() ? "♗" : "♝";
            case KNIGHT -> piece.isWhite() ? "♘" : "♞";
            case PAWN -> piece.isWhite() ? "♙" : "♟";
            default -> "";
        };
    }

    /**
     * Inner class to represent SVG element properties
     */
    private static class SVGElement {
        Path2D path;
        Color fill;
        Color stroke;
        float strokeWidth = 1.0f;
        int strokeCap = BasicStroke.CAP_BUTT;
        int strokeJoin = BasicStroke.JOIN_MITER;
    }
}
