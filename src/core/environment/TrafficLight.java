package core.environment;

import core.constants.Direction;
import java.awt.*;

public class TrafficLight {
    private boolean isFiveWay;
    private String phase;   // HORIZONTAL, VERTICAL, DIAGONAL
    private String state;   // GREEN, YELLOW
    private int countdown;
    private int tick;
    private boolean manualMode;

    public TrafficLight(boolean isFiveWay) {
        this.isFiveWay = isFiveWay;
        this.phase = "HORIZONTAL";
        this.state = "GREEN";
        this.countdown = 4;
        this.tick = 0;
        this.manualMode = false;
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

    // =========================================================
    // VẼ CỘT ĐÈN PROCEDURAL PIXEL ART (Không cần load ảnh)
    // =========================================================
    public void drawPole(Graphics2D g2d, int x, int y, Direction dir) {
        boolean isGreen = canGo(dir);
        boolean isYellow = isYellowFor(dir);
        boolean isRed = !isGreen && !isYellow;

        // --- 1. VẼ CỘT SẮT (POLE) CHÌM NỔI ---
        g2d.setColor(new Color(105, 110, 115)); // Xám kim loại
        g2d.fillRect(x + 6, y + 42, 6, 24);
        g2d.setColor(new Color(75, 80, 85));    // Đổ bóng tạo khối
        g2d.fillRect(x + 10, y + 42, 2, 24);

        // --- 2. VẼ HỘP ĐÈN (BOX) ---
        g2d.setColor(new Color(35, 40, 45));    // Nhựa nhám đen
        g2d.fillRect(x, y, 18, 42);
        g2d.setColor(new Color(70, 75, 80));    // Viền nổi
        g2d.drawRect(x, y, 17, 41);

        // --- 3. BẢNG MÀU PIXEL NEON ---
        Color offRed = new Color(60, 15, 15);
        Color offYellow = new Color(60, 60, 15);
        Color offGreen = new Color(15, 60, 15);

        Color onRed = new Color(255, 50, 50);
        Color onYellow = new Color(255, 230, 0);
        Color onGreen = new Color(50, 255, 50);

        // --- 4. VẼ BÓNG ĐÈN ---
        g2d.setColor(isRed ? onRed : offRed);
        g2d.fillOval(x + 4, y + 4, 10, 10);

        g2d.setColor(isYellow ? onYellow : offYellow);
        g2d.fillOval(x + 4, y + 16, 10, 10);

        g2d.setColor(isGreen ? onGreen : offGreen);
        g2d.fillOval(x + 4, y + 28, 10, 10);

        // --- 5. VẼ SỐ ĐẾM NGƯỢC (PIXEL FONT) ---
        String displayNumber;
        Color textColor;

        if (isRed) {
            displayNumber = String.valueOf(getRemainingRedTime(dir));
            textColor = onRed;
        } else if (isYellow) {
            displayNumber = String.valueOf(countdown);
            textColor = onYellow;
        } else {
            displayNumber = String.valueOf(countdown);
            textColor = onGreen;
        }

        // Font Monospaced kết hợp với lệnh tắt Anti-aliasing ở Main sẽ cho ra chữ pixel vuông sắc
        g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
        FontMetrics fm = g2d.getFontMetrics();

        // Căn giữa mặt hộp, nhảy lên ngay trên đỉnh hộp đèn
        int textX = x + (18 - fm.stringWidth(displayNumber)) / 2;
        int textY = y - 4;

        // Vẽ lớp bóng đen ở dưới để số nổi bật, dễ đọc
        g2d.setColor(Color.BLACK);
        g2d.drawString(displayNumber, textX + 1, textY + 1);

        // Vẽ lớp màu dạ quang đè lên trên
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