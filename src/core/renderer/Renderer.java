package core.renderer;

import core.constants.Direction;
import core.environment.Intersection;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Renderer {
    private int width, height;
    private int roadWidth = 240;
    private int roadStartY = 220;

    // --- CÁC CHẤT LIỆU PIXEL & VẬT THỂ ---
    private TexturePaint grassTexture;
    private TexturePaint roadTexture;
    private TexturePaint curbTexture;
    private BufferedImage treeSprite; // Lưu trữ ảnh cây tự vẽ

    private List<Point> treePositions = new ArrayList<>();
    private boolean treesGenerated = false; // Cờ đánh dấu đã rải cây chưa

    public Renderer(int width, int height) {
        this.width = width;
        this.height = height;
        generatePixelTiles(); // Tạo chất liệu đường/cỏ ngay khi chạy
    }

    private void generatePixelTiles() {
        int TILE_SIZE = 32;
        Random rand = new Random(12345);

        // 1. TẠO GẠCH CỎ
        BufferedImage grassImg = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = grassImg.createGraphics();
        g.setColor(new Color(85, 170, 85));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.setColor(new Color(65, 150, 65));
        for(int i=0; i<15; i++) g.fillRect(rand.nextInt(TILE_SIZE), rand.nextInt(TILE_SIZE), 2, 2);
        g.setColor(new Color(105, 190, 105));
        for(int i=0; i<10; i++) g.fillRect(rand.nextInt(TILE_SIZE), rand.nextInt(TILE_SIZE), 2, 2);
        g.setColor(new Color(220, 220, 80));
        for(int i=0; i<2; i++) g.fillRect(rand.nextInt(TILE_SIZE), rand.nextInt(TILE_SIZE), 2, 2);
        g.dispose();
        grassTexture = new TexturePaint(grassImg, new Rectangle(0, 0, TILE_SIZE, TILE_SIZE));

        // 2. TẠO GẠCH ĐƯỜNG NHỰA
        BufferedImage roadImg = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        g = roadImg.createGraphics();
        g.setColor(new Color(65, 70, 75));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.setColor(new Color(55, 60, 65));
        for(int i=0; i<25; i++) g.fillRect(rand.nextInt(TILE_SIZE), rand.nextInt(TILE_SIZE), 2, 2);
        g.dispose();
        roadTexture = new TexturePaint(roadImg, new Rectangle(0, 0, TILE_SIZE, TILE_SIZE));

        // 3. TẠO GẠCH VỈA HÈ
        BufferedImage curbImg = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        g = curbImg.createGraphics();
        g.setColor(new Color(190, 190, 190));
        g.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        g.setColor(new Color(140, 140, 140));
        g.drawRect(0, 0, TILE_SIZE-1, TILE_SIZE-1);
        g.dispose();
        curbTexture = new TexturePaint(curbImg, new Rectangle(0, 0, TILE_SIZE, TILE_SIZE));

        // 4. TẠO SPRITE CÂY (PIXEL TREE) - Nền Trong Suốt (ARGB)
        treeSprite = new BufferedImage(48, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gt = treeSprite.createGraphics();

        // Vẽ thân cây (Nâu)
        gt.setColor(new Color(90, 60, 40));
        gt.fillRect(20, 40, 8, 24);
        gt.setColor(new Color(110, 80, 50)); // Highlight thân
        gt.fillRect(20, 40, 3, 24);

        // Vẽ tán lá - Lớp bóng tối (Dưới cùng)
        gt.setColor(new Color(30, 90, 30));
        gt.fillRect(8, 16, 32, 28);
        gt.fillRect(4, 20, 40, 20);
        gt.fillRect(16, 8, 16, 36);

        // Vẽ tán lá - Lớp xanh vừa (Giữa)
        gt.setColor(new Color(45, 115, 40));
        gt.fillRect(12, 12, 24, 24);
        gt.fillRect(8, 16, 32, 16);

        // Vẽ tán lá - Lớp xanh sáng (Trên cùng/Bắt sáng)
        gt.setColor(new Color(60, 140, 50));
        gt.fillRect(16, 8, 16, 16);
        gt.fillRect(12, 12, 8, 8);
        gt.fillRect(28, 16, 8, 8);

        // Chấm bóng râm tạo khối
        gt.setColor(new Color(20, 70, 20));
        gt.fillRect(32, 28, 8, 8);
        gt.fillRect(12, 32, 8, 4);
        gt.dispose();
    }

    // =========================================================
    // HÀM RẢI CÂY THÔNG MINH (NÉ ĐƯỜNG VÀ VỈA HÈ)
    // =========================================================
    private void generateTrees(List<Intersection> intersections) {
        Random rand = new Random(42); // Seed cố định để cây mọc lại y hệt mỗi lần bật app
        int curb = 40; // Vùng đệm cấm cây mọc sát mép đường

        for (int i = 0; i < 400; i++) { // Thử gieo 400 hạt giống
            int tx = rand.nextInt(width - 48);
            int ty = rand.nextInt(height - 64);
            boolean isValid = true;

            // 1. Cấm mọc đè trục đường ngang
            if (ty + 64 > roadStartY - curb && ty < roadStartY + roadWidth + curb) isValid = false;

            // 2. Cấm mọc đè trục đường dọc
            for (Intersection inter : intersections) {
                if (tx + 48 > inter.x - curb && tx < inter.x + roadWidth + curb) {
                    if (inter.type.equals("3way")) {
                        if (ty + 64 > roadStartY - curb) isValid = false;
                    } else {
                        isValid = false;
                    }
                }
            }

            // 3. Quét quang khu vực ngã 5 (Góc trên cùng bên phải)
            if (tx > 1100 && ty < 400) isValid = false;

            // 4. Chống mọc đè lên nhau quá sát (Khoảng cách tối thiểu)
            if (isValid) {
                for (Point p : treePositions) {
                    if (Math.hypot(p.x - tx, p.y - ty) < 45) {
                        isValid = false; break;
                    }
                }
            }

            if (isValid) treePositions.add(new Point(tx, ty));
        }
    }

    // =========================================================
    // CÁC HÀM VẼ GIỮ NGUYÊN
    // =========================================================
    private void drawFillet(Graphics2D g2d, int cx, int cy, int quadrant) {
        int R = 35, C = 6;
        Shape oldClip = g2d.getClip();
        if (quadrant == 1) {
            g2d.setClip(new Rectangle(cx, cy - R, R, R));
            g2d.setPaint(roadTexture);
            g2d.fillRect(cx, cy - R, R, R);
            g2d.setPaint(curbTexture);
            g2d.fillOval(cx + R - R, cy - R - R, R*2, R*2);
            g2d.setPaint(grassTexture);
            g2d.fillOval(cx + R - (R-C), cy - R - (R-C), (R-C)*2, (R-C)*2);
        } else if (quadrant == 2) {
            g2d.setClip(new Rectangle(cx - R, cy - R, R, R));
            g2d.setPaint(roadTexture);
            g2d.fillRect(cx - R, cy - R, R, R);
            g2d.setPaint(curbTexture);
            g2d.fillOval(cx - R - R, cy - R - R, R*2, R*2);
            g2d.setPaint(grassTexture);
            g2d.fillOval(cx - R - (R-C), cy - R - (R-C), (R-C)*2, (R-C)*2);
        } else if (quadrant == 3) {
            g2d.setClip(new Rectangle(cx - R, cy, R, R));
            g2d.setPaint(roadTexture);
            g2d.fillRect(cx - R, cy, R, R);
            g2d.setPaint(curbTexture);
            g2d.fillOval(cx - R - R, cy + R - R, R*2, R*2);
            g2d.setPaint(grassTexture);
            g2d.fillOval(cx - R - (R-C), cy + R - (R-C), (R-C)*2, (R-C)*2);
        } else if (quadrant == 4) {
            g2d.setClip(new Rectangle(cx, cy, R, R));
            g2d.setPaint(roadTexture);
            g2d.fillRect(cx, cy, R, R);
            g2d.setPaint(curbTexture);
            g2d.fillOval(cx + R - R, cy + R - R, R*2, R*2);
            g2d.setPaint(grassTexture);
            g2d.fillOval(cx + R - (R-C), cy + R - (R-C), (R-C)*2, (R-C)*2);
        }
        g2d.setClip(oldClip);
    }

    public void drawBackgroundAndRoads(Graphics2D g2d, List<Intersection> intersections) {
        // Tự động gieo hạt trồng cây 1 lần duy nhất ở Frame đầu tiên
        if (!treesGenerated) {
            generateTrees(intersections);
            treesGenerated = true;
        }

        g2d.setPaint(grassTexture);
        g2d.fillRect(0, 0, width, height);
        int curb = 6;

        g2d.setPaint(curbTexture);
        g2d.fillRect(0, roadStartY - curb, width, roadWidth + curb*2);
        for (Intersection inter : intersections) {
            if (inter.type.equals("3way")) {
                g2d.fillRect(inter.x - curb, roadStartY, roadWidth + curb*2, height - roadStartY);
            } else if (inter.type.equals("4way") || inter.type.equals("5way")) {
                g2d.fillRect(inter.x - curb, 0, roadWidth + curb*2, height);
            }
        }

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

        g2d.setPaint(roadTexture);
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

        for (Intersection inter : intersections) {
            drawFillet(g2d, inter.x, roadStartY + roadWidth, 3);
            drawFillet(g2d, inter.x + roadWidth, roadStartY + roadWidth, 4);
            if (inter.type.equals("4way")) {
                drawFillet(g2d, inter.x, roadStartY, 2);
                drawFillet(g2d, inter.x + roadWidth, roadStartY, 1);
            } else if (inter.type.equals("5way")) {
                drawFillet(g2d, inter.x, roadStartY, 2);
            }
        }

        // --- VẼ CÂY CỐI LÊN TRÊN CỎ ---
        for (Point p : treePositions) {
            // Bóng râm dưới gốc cây
            g2d.setColor(new Color(0, 0, 0, 40));
            g2d.fillOval(p.x + 8, p.y + 54, 32, 12);
            // Ảnh cây pixel
            g2d.drawImage(treeSprite, p.x, p.y, null);
        }

        g2d.setColor(new Color(240, 240, 240));
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

        if (inter5 != null) {
            g2d.setStroke(new BasicStroke(4));
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
            g2d.setStroke(new BasicStroke(1));
        }

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