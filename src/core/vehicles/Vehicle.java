package core.vehicles;

import core.constants.Direction;
import core.strategies.DriverStrategy;
import core.audio.SoundManager;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public abstract class Vehicle {
    protected double x, y;
    protected Direction direction;
    protected String name;
    protected Color color;
    protected double speed;
    protected final double originalSpeed;
    protected String turnIntent;
    protected boolean hasTurned;
    protected DriverStrategy driverStrategy;
    protected boolean waitingToTurn = false;

    // Kích thước logic dùng để tính toán va chạm và khoảng cách (không đổi)
    protected static final int LENGTH = 55;
    protected static final int WIDTH = 32;
    protected static final Random random = new Random();

    // Cache lưu ảnh để tối ưu hiệu năng
    protected static Map<String, BufferedImage> spriteCache = new HashMap<>();

    public Vehicle(double x, double y, Direction direction, String name, Color color, double speed) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.name = name;
        this.color = color;
        this.speed = speed;
        this.originalSpeed = speed;
        this.hasTurned = false;



        int r = random.nextInt(100);
        if (r < 50) turnIntent = "STRAIGHT";
        else if (r < 65) turnIntent = "RIGHT";
        else if (r < 80) turnIntent = "LEFT";
        else turnIntent = "DIAGONAL";
    }

    protected void loadSprite(String name) {
        if (!spriteCache.containsKey(name)) {
            try {
                BufferedImage img = ImageIO.read(new File("assets/" + name.toLowerCase() + ".png"));
                spriteCache.put(name, img);
            } catch (IOException e) {
                System.err.println("Chưa có ảnh: assets/" + name.toLowerCase() + ".png");
            }
        }
    }

    public void draw(Graphics2D g2d) {
        loadSprite(name);
        BufferedImage sprite = spriteCache.get(name);

        // ĐIỀU CHỈNH KÍCH THƯỚC CHUẨN TỰ ĐỘNG
        int imgW = 32; // Chiều rộng mặc định (Ô tô)
        int imgH = 55; // Chiều dài mặc định (Ô tô)

        if (name.equals("Motor") || name.equals("Bicycle")) {
            imgW = 16; // Xe máy/xe đạp hẹp hơn
            imgH = 40; // và ngắn hơn
        } else if (name.equals("Ambu") || name.equals("Fire")) {
            imgW = 34; // Xe ưu tiên to và bề thế hơn một chút
            imgH = 60;
        }

        // Dự phòng nếu mất file ảnh
        if (sprite == null) {
            g2d.setColor(color);
            g2d.fillRect((int)x, (int)y, getBodyWidth(), getBodyHeight());
            return;
        }

        // Tính góc xoay
        double angle = 0;
        switch (direction) {
            case EAST: angle = 0; break;
            case SOUTH: angle = Math.PI / 2; break;
            case WEST: angle = Math.PI; break;
            case NORTH: angle = -Math.PI / 2; break;
            case NORTHEAST: angle = -Math.PI / 4; break;
            case SOUTHWEST: angle = 3 * Math.PI / 4; break;
        }

        // GÓC BÙ: Mặc định ảnh gốc mũi xe chĩa lên trên (Bắc), cần +90 độ (Math.PI/2) để khớp
        angle += Math.PI / 2;

        AffineTransform old = g2d.getTransform();

        // Căn đúng tâm của phương tiện
        double centerX = x + getBodyWidth() / 2.0;
        double centerY = y + getBodyHeight() / 2.0;

        g2d.translate(centerX, centerY);
        g2d.rotate(angle);

        // Vẽ và Ép size ảnh (Bất kể ảnh bạn tải về to hay nhỏ)
        g2d.drawImage(sprite, -imgW / 2, -imgH / 2, imgW, imgH, null);

        g2d.setTransform(old);
    }

    // Thay thế hàm move cũ của bạn bằng hàm tối ưu âm thanh này:
    public void move(boolean isAllowed) {
        if (driverStrategy != null) {
            driverStrategy.move(this, isAllowed);

            // Chỉ bóp còi nếu:
            // 1. Xe bị dừng (speed == 0 và !isAllowed)
            // 2. TỶ LỆ CHỈ CÒN 20% (thay vì 50%)
            // 3. KHÔNG PHẢI VÌ ĐÈN ĐỎ/VÀNG: Chúng ta kiểm tra nếu xe đang chờ đèn
            // (Bạn có thể thêm điều kiện !isWaitingAtTrafficLight vào đây)
            if (speed == 0 && !isAllowed && !isWaitingAtTrafficLight()) {
                SoundManager.playHornOnNearCollision();
            }
        } else {
            if (!isAllowed) {
                speed = 0;
                // Bóp còi với tỷ lệ 20% và kiểm tra không phải vì đèn giao thông
                if (!isWaitingAtTrafficLight()) {
                    playHornWithLowProbability();
                }
                return;
            }
            speed = originalSpeed;
            updatePosition();
        }
    }

    // --- HÀM BỔ TRỢ MỚI ---
    private boolean isWaitingAtTrafficLight() {
        // Kiểm tra xem xe có đang ở phạm vi ngã tư không.
        // Bạn thay logic này bằng biến trạng thái thực tế của xe bạn đang dùng nhé.
        return this.waitingToTurn;
    }

    private void playHornWithLowProbability() {
        // Tỷ lệ 0.20 tương đương với 20%
        if (Math.random() < 0.20) {
            SoundManager.playHornOnNearCollision();
        }
    }

    public void updatePosition() {
        double diag = Math.sqrt(2) / 2;
        switch (direction) {
            case EAST:  x += speed; break;
            case WEST:  x -= speed; break;
            case SOUTH: y += speed; break;
            case NORTH: y -= speed; break;
            case NORTHEAST: x += speed * diag; y -= speed * diag; break;
            case SOUTHWEST: x -= speed * diag; y += speed * diag; break;
        }
    }

    public int getBodyWidth() {
        if (direction == Direction.EAST || direction == Direction.WEST) return LENGTH;
        if (direction == Direction.NORTH || direction == Direction.SOUTH) return WIDTH;
        return 45;
    }

    public int getBodyHeight() {
        if (direction == Direction.EAST || direction == Direction.WEST) return WIDTH;
        if (direction == Direction.NORTH || direction == Direction.SOUTH) return LENGTH;
        return 45;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public Direction getDirection() { return direction; }
    public void setDirection(Direction d) { this.direction = d; }
    public String getName() { return name; }
    public double getSpeed() { return speed; }
    public void setSpeed(double s) { this.speed = s; }
    public double getOriginalSpeed() { return originalSpeed; }
    public String getTurnIntent() { return turnIntent; }
    public boolean hasTurned() { return hasTurned; }
    public void setHasTurned(boolean t) { hasTurned = t; }
    public void setWaitingToTurn(boolean waiting) { this.waitingToTurn = waiting; }
    public boolean isWaitingToTurn() { return waitingToTurn; }
    public void setDriverStrategy(DriverStrategy strategy) { this.driverStrategy = strategy; }
    public void setTurnIntent(String intent) { this.turnIntent = intent; }
}