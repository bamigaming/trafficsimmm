package core.environment;

import core.constants.Direction;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class TrafficLight {
    private boolean isFiveWay;
    private String phase;   // HORIZONTAL, VERTICAL, DIAGONAL
    private String state;   // GREEN, YELLOW
    private int countdown;
    private int tick;
    private boolean manualMode;

    // --- BIẾN MỚI CHO ẢNH ĐÈN PIXEL ---
    private static BufferedImage redSprite;
    private static BufferedImage yellowSprite;
    private static BufferedImage greenSprite;
    private static boolean spritesLoaded = false;

    public TrafficLight(boolean isFiveWay) {
        this.isFiveWay = isFiveWay;
        this.phase = "HORIZONTAL";
        this.state = "GREEN";
        this.countdown = 4;
        this.tick = 0;
        this.manualMode = false;

        // Load ảnh 1 lần duy nhất cho tất cả các đèn
        if (!spritesLoaded) {
            loadSprites();
        }
    }

    // --- HÀM MỚI: LOAD VÀ CẮT ẢNH TỰ ĐỘNG ---
    private void loadSprites() {
        try {
            BufferedImage sheet = ImageIO.read(new File("assets/traffic_light_sprites.png"));
            // Tự động cắt làm 3 khung hình bằng nhau
            int frameWidth = sheet.getWidth() / 3;
            int frameHeight = sheet.getHeight();

            redSprite = sheet.getSubimage(0, 0, frameWidth, frameHeight);
            yellowSprite = sheet.getSubimage(frameWidth, 0, frameWidth, frameHeight);
            greenSprite = sheet.getSubimage(frameWidth * 2, 0, frameWidth, frameHeight);

            spritesLoaded = true;
        } catch (IOException e) {
            System.err.println("Không tìm thấy ảnh assets/traffic_light_sprites.png! Sẽ dùng đèn vẽ tay cũ.");
            spritesLoaded = false;
        }
    }

    public void update() {
        if (manualMode) return;
        tick++;
        if (tick >= 40) {
            tick = 0;
            countdown--;
            if (countdown <= 0) switchState();
        }
    }

    public void change() {
        if (!manualMode) return;
        switchState();
    }

    private void switchState() {
        if (state.equals("GREEN")) {
            state = "YELLOW";
            countdown = 2;
        } else {
            state = "GREEN";
            if (isFiveWay) {
                switch (phase) {
                    case "HORIZONTAL": phase = "VERTICAL"; break;
                    case "VERTICAL": phase = "DIAGONAL"; break;
                    default: phase = "HORIZONTAL";
                }
            } else {
                phase = phase.equals("HORIZONTAL") ? "VERTICAL" : "HORIZONTAL";
            }
            countdown = 4;
        }
    }

    public boolean canGo(Direction direction) {
        if (!state.equals("GREEN")) return false;
        if (isFiveWay) {
            if (phase.equals("HORIZONTAL")) return direction == Direction.EAST || direction == Direction.WEST;
            if (phase.equals("VERTICAL")) return direction == Direction.NORTH || direction == Direction.SOUTH;
            if (phase.equals("DIAGONAL")) return direction == Direction.SOUTHWEST;
        } else {
            if (phase.equals("HORIZONTAL")) return direction == Direction.EAST || direction == Direction.WEST;
            if (phase.equals("VERTICAL")) return direction == Direction.NORTH || direction == Direction.SOUTH;
        }
        return false;
    }

    public boolean isYellowFor(Direction direction) {
        if (!state.equals("YELLOW")) return false;
        if (isFiveWay) {
            if (phase.equals("HORIZONTAL")) return direction == Direction.EAST || direction == Direction.WEST;
            if (phase.equals("VERTICAL")) return direction == Direction.NORTH || direction == Direction.SOUTH;
            if (phase.equals("DIAGONAL")) return direction == Direction.SOUTHWEST;
        } else {
            if (phase.equals("HORIZONTAL")) return direction == Direction.EAST || direction == Direction.WEST;
            if (phase.equals("VERTICAL")) return direction == Direction.NORTH || direction == Direction.SOUTH;
        }
        return false;
    }

    public int getRemainingRedTime(Direction direction) {
        String[] phasesOrder = isFiveWay ? new String[]{"HORIZONTAL", "VERTICAL", "DIAGONAL"} : new String[]{"HORIZONTAL", "VERTICAL"};
        String targetPhase;
        if (direction == Direction.EAST || direction == Direction.WEST) targetPhase = "HORIZONTAL";
        else if (direction == Direction.NORTH || direction == Direction.SOUTH) targetPhase = "VERTICAL";
        else targetPhase = "DIAGONAL";

        if (phase.equals(targetPhase)) return 0;

        int currentIdx = indexOf(phasesOrder, phase);
        int targetIdx = indexOf(phasesOrder, targetPhase);

        int timeLeft;
        if (state.equals("GREEN")) timeLeft = countdown + 2;
        else timeLeft = countdown;

        int idx = (currentIdx + 1) % phasesOrder.length;
        while (idx != targetIdx) {
            timeLeft += 6; // 4s green + 2s yellow
            idx = (idx + 1) % phasesOrder.length;
        }
        return timeLeft;
    }

    private int indexOf(String[] arr, String val) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(val)) return i;
        return -1;
    }

    public void drawPole(Graphics2D g2d, int x, int y, Direction dir) {
        int width = 45, height = 90;

        boolean isGreen = canGo(dir);
        boolean isYellow = isYellowFor(dir);
        boolean isRed = !isGreen && !isYellow;

        // --- MỚI: VẼ ĐÈN BẰNG ẢNH PIXEL ---
        if (spritesLoaded) {
            BufferedImage spriteToDraw = redSprite; // Mặc định là đỏ
            if (isGreen) spriteToDraw = greenSprite;
            else if (isYellow) spriteToDraw = yellowSprite;

            g2d.drawImage(spriteToDraw, x, y, width, height, null);
        } else {
            // BACKUP: Vẽ kiểu cũ nếu không load được ảnh
            g2d.setColor(new Color(50, 50, 50));
            g2d.fillRoundRect(x, y, width, height, 8, 8);
            g2d.setColor(Color.BLACK);
            g2d.drawRoundRect(x, y, width, height, 8, 8);

            int ballD = 20;
            int ballX = x + (width - ballD)/2;
            int redY = y + 10, yellowY = y + 30, greenY = y + 50;

            g2d.setColor(isRed ? Color.RED : new Color(80,80,80));
            g2d.fillOval(ballX, redY, ballD, ballD);
            g2d.setColor(isYellow ? Color.YELLOW : new Color(80,80,80));
            g2d.fillOval(ballX, yellowY, ballD, ballD);
            g2d.setColor(isGreen ? Color.GREEN : new Color(80,80,80));
            g2d.fillOval(ballX, greenY, ballD, ballD);
        }

        // --- CODE VẼ ĐỒNG HỒ ĐẾM NGƯỢC ---
        Font font = new Font("Arial", Font.BOLD, 16);
        g2d.setFont(font);
        String displayNumber;
        Color textColor;
        if (isRed) {
            displayNumber = String.valueOf(getRemainingRedTime(dir));
            textColor = Color.RED;
        } else if (isYellow) {
            displayNumber = String.valueOf(countdown);
            textColor = Color.YELLOW;
        } else {
            displayNumber = String.valueOf(countdown);
            textColor = Color.GREEN;
        }

        FontMetrics fm = g2d.getFontMetrics();
        int sw = fm.stringWidth(displayNumber);

        // Căn giữa số đếm ngược
        int textX = x + width/2 - sw/2;

        // ĐÃ HẠ TỌA ĐỘ Y XUỐNG SÁT NẮP ĐÈN
        // Nếu bạn muốn nó lún sâu xuống nữa thì tăng số 8 lên (ví dụ + 10, + 12)
        // Nếu nó lẹm vào nắp đèn quá thì giảm xuống (ví dụ + 5, + 3)
        int textY = y + 8;

        g2d.setColor(Color.BLACK); // Đổ bóng viền chữ
        g2d.drawString(displayNumber, textX + 1, textY + 1);

        g2d.setColor(textColor);
        g2d.drawString(displayNumber, textX, textY);
    }

    public void setManualMode(boolean mode) { this.manualMode = mode; }
    public String getState() { return state; }

    public void fastForward(int seconds) {
        if (!manualMode) return;
        int remaining = seconds;
        while (remaining > 0) {
            int reduce = Math.min(remaining, countdown);
            countdown -= reduce;
            remaining -= reduce;
            if (countdown <= 0) {
                switchState();
            }
        }
    }
}