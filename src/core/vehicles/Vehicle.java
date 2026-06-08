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

    protected static final int LENGTH = 55;
    protected static final int WIDTH = 32;
    protected static final Random random = new Random();
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

    // --- LOGIC DI CHUYỂN VÀ "ĐÒI" BÓP CÒI ---
    public void move(boolean isAllowed) {
        if (driverStrategy != null) {
            driverStrategy.move(this, isAllowed);

            // Nếu xe kẹt (tốc độ 0) VÀ không phải do đang đỗ đèn đỏ -> "Đòi" bóp còi
            if (speed == 0 && !isAllowed && !isWaitingAtTrafficLight()) {
                SoundManager.playHornOnNearCollision();
            }
        } else {
            if (!isAllowed) {
                speed = 0;
                // Nếu xe kẹt VÀ không phải do đang đỗ đèn đỏ -> "Đòi" bóp còi
                if (!isWaitingAtTrafficLight()) {
                    SoundManager.playHornOnNearCollision();
                }
                return;
            }
            speed = originalSpeed;
            updatePosition();
        }
    }

    private boolean isWaitingAtTrafficLight() {
        return this.waitingToTurn;
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
        int imgW = 32, imgH = 55;
        if (name.equals("Motor") || name.equals("Bicycle")) { imgW = 16; imgH = 40; }
        else if (name.equals("Ambu") || name.equals("Fire")) { imgW = 34; imgH = 60; }

        if (sprite == null) {
            g2d.setColor(color);
            g2d.fillRect((int)x, (int)y, getBodyWidth(), getBodyHeight());
            return;
        }

        double angle = 0;
        switch (direction) {
            case EAST: angle = 0; break;
            case SOUTH: angle = Math.PI / 2; break;
            case WEST: angle = Math.PI; break;
            case NORTH: angle = -Math.PI / 2; break;
            case NORTHEAST: angle = -Math.PI / 4; break;
            case SOUTHWEST: angle = 3 * Math.PI / 4; break;
        }
        angle += Math.PI / 2;
        AffineTransform old = g2d.getTransform();
        double centerX = x + getBodyWidth() / 2.0;
        double centerY = y + getBodyHeight() / 2.0;
        g2d.translate(centerX, centerY);
        g2d.rotate(angle);
        g2d.drawImage(sprite, -imgW / 2, -imgH / 2, imgW, imgH, null);
        g2d.setTransform(old);
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

    public int getBodyWidth() { return (direction == Direction.EAST || direction == Direction.WEST) ? LENGTH : WIDTH; }
    public int getBodyHeight() { return (direction == Direction.NORTH || direction == Direction.SOUTH) ? LENGTH : WIDTH; }
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