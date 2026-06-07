package core.renderer;

import core.constants.Direction;
import core.environment.Intersection;

import java.awt.*;
import java.util.List;

public class Renderer {
    private int width, height;
    private int roadWidth = 240;
    private int roadStartY = 220;
    private Color bgColor = new Color(163, 206, 113);
    private Color roadColor = new Color(109, 125, 139);
    private Color curbColor = Color.WHITE;
    private Color lineColor = Color.WHITE;

    public Renderer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    private void drawFillet(Graphics2D g2d, int cx, int cy, int quadrant) {
        int R = 35, C = 6;
        Shape oldClip = g2d.getClip();
        if (quadrant == 1) {
            g2d.setClip(new Rectangle(cx, cy - R, R, R));
            g2d.setColor(roadColor);
            g2d.fillRect(cx, cy - R, R, R);
            g2d.setColor(curbColor);
            g2d.fillOval(cx + R - R, cy - R - R, R*2, R*2);
            g2d.setColor(bgColor);
            g2d.fillOval(cx + R - (R-C), cy - R - (R-C), (R-C)*2, (R-C)*2);
        } else if (quadrant == 2) {
            g2d.setClip(new Rectangle(cx - R, cy - R, R, R));
            g2d.setColor(roadColor);
            g2d.fillRect(cx - R, cy - R, R, R);
            g2d.setColor(curbColor);
            g2d.fillOval(cx - R - R, cy - R - R, R*2, R*2);
            g2d.setColor(bgColor);
            g2d.fillOval(cx - R - (R-C), cy - R - (R-C), (R-C)*2, (R-C)*2);
        } else if (quadrant == 3) {
            g2d.setClip(new Rectangle(cx - R, cy, R, R));
            g2d.setColor(roadColor);
            g2d.fillRect(cx - R, cy, R, R);
            g2d.setColor(curbColor);
            g2d.fillOval(cx - R - R, cy + R - R, R*2, R*2);
            g2d.setColor(bgColor);
            g2d.fillOval(cx - R - (R-C), cy + R - (R-C), (R-C)*2, (R-C)*2);
        } else if (quadrant == 4) {
            g2d.setClip(new Rectangle(cx, cy, R, R));
            g2d.setColor(roadColor);
            g2d.fillRect(cx, cy, R, R);
            g2d.setColor(curbColor);
            g2d.fillOval(cx + R - R, cy + R - R, R*2, R*2);
            g2d.setColor(bgColor);
            g2d.fillOval(cx + R - (R-C), cy + R - (R-C), (R-C)*2, (R-C)*2);
        }
        g2d.setClip(oldClip);
    }

    public void drawBackgroundAndRoads(Graphics2D g2d, List<Intersection> intersections) {
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, width, height);
        int curb = 6;
        // Vỉa hè trắng
        g2d.setColor(curbColor);
        g2d.fillRect(0, roadStartY - curb, width, roadWidth + curb*2);
        for (Intersection inter : intersections) {
            if (inter.type.equals("3way")) {
                g2d.fillRect(inter.x - curb, roadStartY, roadWidth + curb*2, height - roadStartY);
            } else if (inter.type.equals("4way") || inter.type.equals("5way")) {
                g2d.fillRect(inter.x - curb, 0, roadWidth + curb*2, height);
            }
        }
        // Đường chéo ngã 5
        Intersection inter5 = intersections.stream().filter(i -> i.type.equals("5way")).findFirst().orElse(null);
        if (inter5 != null) {
            int cx5 = inter5.x + roadWidth/2;
            int cy5 = roadStartY + roadWidth/2;
            int length = 1000;
            int endX = cx5 + length, endY = cy5 - length;
            double curbHw = (roadWidth/2.0) + curb;
            double dxc = curbHw * 0.707, dyc = curbHw * 0.707;
            Polygon curbPoly = new Polygon();
            curbPoly.addPoint((int)(cx5 - dxc), (int)(cy5 - dyc));
            curbPoly.addPoint((int)(cx5 + dxc), (int)(cy5 + dyc));
            curbPoly.addPoint((int)(endX + dxc), (int)(endY + dyc));
            curbPoly.addPoint((int)(endX - dxc), (int)(endY - dyc));
            g2d.fillPolygon(curbPoly);
        }

        // Mặt đường xám
        g2d.setColor(roadColor);
        g2d.fillRect(0, roadStartY, width, roadWidth);
        for (Intersection inter : intersections) {
            if (inter.type.equals("3way")) {
                g2d.fillRect(inter.x, roadStartY, roadWidth, height - roadStartY);
            } else if (inter.type.equals("4way") || inter.type.equals("5way")) {
                g2d.fillRect(inter.x, 0, roadWidth, height);
            }
        }
        if (inter5 != null) {
            int cx5 = inter5.x + roadWidth/2;
            int cy5 = roadStartY + roadWidth/2;
            int length = 1000;
            int endX = cx5 + length, endY = cy5 - length;
            double roadHw = roadWidth/2.0;
            double dxr = roadHw * 0.707, dyr = roadHw * 0.707;
            Polygon roadPoly = new Polygon();
            roadPoly.addPoint((int)(cx5 - dxr), (int)(cy5 - dyr));
            roadPoly.addPoint((int)(cx5 + dxr), (int)(cy5 + dyr));
            roadPoly.addPoint((int)(endX + dxr), (int)(endY + dyr));
            roadPoly.addPoint((int)(endX - dxr), (int)(endY - dyr));
            g2d.fillPolygon(roadPoly);
        }

        // Bo góc
        for (Intersection inter : intersections) {
            drawFillet(g2d, inter.x, roadStartY + roadWidth, 3);
            drawFillet(g2d, inter.x + roadWidth, roadStartY + roadWidth, 4);
            if (inter.type.equals("4way")) {
                drawFillet(g2d, inter.x, roadStartY, 2);
                drawFillet(g2d, inter.x + roadWidth, roadStartY, 1);
            } else if (inter.type.equals("5way")) {
                drawFillet(g2d, inter.x, roadStartY, 2);
                // Bỏ qua góc 1 (trên-phải) của ngã 5
            }
        }

        // Vạch kẻ đường
        g2d.setColor(lineColor);
        int laneMidY = roadStartY + roadWidth/2;
        int segStartX = 0;
        for (Intersection inter : intersections) {
            int segEndX = inter.x - 45;
            if (segEndX > segStartX) {
                for (int x = segStartX; x < segEndX; x += 40) {
                    if (x + 20 <= segEndX) g2d.fillRect(x, laneMidY - 2, 20, 4);
                }
            }
            segStartX = inter.x + roadWidth + 45;
        }
        if (segStartX < width) {
            for (int x = segStartX; x < width; x += 40) {
                g2d.fillRect(x, laneMidY - 2, 20, 4);
            }
        }
        for (Intersection inter : intersections) {
            int laneMidX = inter.x + roadWidth/2;
            int startY = roadStartY + roadWidth + 45;
            for (int y = startY; y < height; y += 40) {
                if (y + 20 <= height) g2d.fillRect(laneMidX - 2, y, 4, 20);
            }
            if (inter.type.equals("4way") || inter.type.equals("5way")) {
                int endY = roadStartY - 45;
                for (int y = 0; y < endY; y += 40) {
                    if (y + 20 <= endY) g2d.fillRect(laneMidX - 2, y, 4, 20);
                }
            }
        }
        // Vạch chéo
        if (inter5 != null) {
            int cx5 = inter5.x + roadWidth/2;
            int cy5 = roadStartY + roadWidth/2;
            double currentDist = roadWidth/2.0 + 45;
            double maxDist = 1000;
            while (currentDist < maxDist) {
                int dashStartX = (int)(cx5 + currentDist * 0.707);
                int dashStartY = (int)(cy5 - currentDist * 0.707);
                int dashEndX = (int)(cx5 + (currentDist + 20) * 0.707);
                int dashEndY = (int)(cy5 - (currentDist + 20) * 0.707);
                g2d.drawLine(dashStartX, dashStartY, dashEndX, dashEndY);
                currentDist += 40;
            }
        }
        // Zebra crossing
        for (Intersection inter : intersections) {
            for (int off = 10; off < roadWidth - 10; off += 25) {
                g2d.fillRect(inter.x + off, roadStartY + roadWidth + 10, 14, 30);
            }
            if (inter.type.equals("4way") || inter.type.equals("5way")) {
                for (int off = 10; off < roadWidth - 10; off += 25) {
                    g2d.fillRect(inter.x + off, roadStartY - 40, 14, 30);
                }
            }
            for (int off = 10; off < roadWidth - 10; off += 25) {
                g2d.fillRect(inter.x - 40, roadStartY + off, 30, 14);
                g2d.fillRect(inter.x + roadWidth + 10, roadStartY + off, 30, 14);
            }
        }
    }

    public void drawTrafficLights(Graphics2D g2d, List<Intersection> intersections) {
        for (Intersection inter : intersections) {
            java.util.List<Direction> dirs = new java.util.ArrayList<>();
            dirs.add(Direction.EAST); dirs.add(Direction.WEST);
            if (inter.type.equals("3way") || inter.type.equals("4way") || inter.type.equals("5way")) dirs.add(Direction.SOUTH);
            if (inter.type.equals("4way") || inter.type.equals("5way")) dirs.add(Direction.NORTH);
            for (Direction dir : dirs) {
                int px = 0, py = 0;
                switch (dir) {
                    case EAST: px = inter.x - 50; py = roadStartY + roadWidth + 25; break;
                    case WEST:
                        if (inter.type.equals("5way")) { px = inter.x + roadWidth + 90; py = roadStartY - 140; }
                        else { px = inter.x + roadWidth + 20; py = roadStartY - 110; }
                        break;
                    case SOUTH: px = inter.x - 50; py = roadStartY - 110; break;
                    case NORTH: px = inter.x + roadWidth + 20; py = roadStartY + roadWidth + 25; break;
                }
                inter.light.drawPole(g2d, px, py, dir);
            }
        }
    }
}