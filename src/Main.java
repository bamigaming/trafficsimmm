import core.controller.TrafficController;
import core.environment.Intersection;
import core.environment.TrafficLight;
import core.renderer.Renderer;
import core.vehicles.Vehicle;
import core.audio.SoundManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

public class Main extends JPanel implements ActionListener {
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 900;
    private List<Intersection> intersections;
    private List<Vehicle> vehicles;
    private TrafficController controller;
    private Renderer renderer;
    private Timer timer;
    private boolean manualMode = false;
    private boolean highTraffic = false;

    // Tọa độ click của các nút bấm
    private Rectangle modeButtonRect;
    private Rectangle trafficButtonRect;

    private Image mapImage;
    private JSlider volumeSlider;

    public Main() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(163, 206, 113));
        setLayout(null); // Sử dụng vị trí tuyệt đối để dàn hàng ngang

        // Load ảnh nền bản đồ
        try {
            mapImage = ImageIO.read(new File("assets/city_map.png"));
        } catch (IOException e) {
            System.err.println("VẪN KHÔNG TÌM THẤY ẢNH!");
        }

        vehicles = new ArrayList<>();
        intersections = new ArrayList<>();

        // Tạo ngã tư
        TrafficLight light3 = new TrafficLight(false);
        TrafficLight light4 = new TrafficLight(false);
        TrafficLight light5 = new TrafficLight(true);
        intersections.add(new Intersection(150, 200, "3way", light3));
        intersections.add(new Intersection(600, 200, "4way", light4));
        intersections.add(new Intersection(1200, 200, "5way", light5));

        controller = new TrafficController(intersections, vehicles);
        renderer = new Renderer(WIDTH, HEIGHT);

        // --- CẬP NHẬT: DÀN HÀNG NGANG CÁC NÚT ĐIỀU KHIỂN (CÙNG Y = 20, CAO 45) ---
        // Nút Mode ở vị trí đầu tiên
        modeButtonRect = new Rectangle(20, 20, 160, 45);

        // Nút Traffic dịch sang bên phải nút Mode (X = 200)
        trafficButtonRect = new Rectangle(200, 20, 160, 45);

        // Thanh Slider Volume dịch sang bên phải nút Traffic (X = 490, nằm trong khung lót)
        volumeSlider = new JSlider(0, 100, 70);
        volumeSlider.setBounds(490, 27, 120, 30);
        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);
        volumeSlider.addChangeListener(e -> {
            float vol = volumeSlider.getValue() / 100f;
            SoundManager.setVolume(vol);
        });
        add(volumeSlider);

        // Xử lý sự kiện click chuột
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int mx = e.getX();
                int my = e.getY();

                if (modeButtonRect.contains(mx, my)) {
                    manualMode = !manualMode;
                    for (Intersection inter : intersections) {
                        inter.light.setManualMode(manualMode);
                    }
                    repaint();
                    return;
                }

                if (trafficButtonRect.contains(mx, my)) {
                    highTraffic = !highTraffic;
                    repaint();
                    return;
                }

                if (manualMode) {
                    for (Intersection inter : intersections) {
                        inter.light.fastForward(4);
                    }
                    repaint();
                }
            }
        });

        timer = new Timer(25, this);
        timer.start();

        for (int i = 0; i < 10; i++) controller.spawnVehicle();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (Intersection inter : intersections) {
            inter.light.update();
        }

        double spawnRate = highTraffic ? 0.06 : 0.03;
        if (Math.random() < spawnRate) {
            controller.spawnVehicle();
        }
        controller.updateAll();

        boolean ambulancePresent = false;
        boolean firetruckPresent = false;
        for (Vehicle v : vehicles) {
            if (v.getName().equals("Ambu")) ambulancePresent = true;
            if (v.getName().equals("Fire")) firetruckPresent = true;
        }

        SoundManager.updateEmergencySiren("Ambu", "src/resources/sounds/ambulance.wav", ambulancePresent);
        SoundManager.updateEmergencySiren("Fire", "src/resources/sounds/firetruck.wav", firetruckPresent);

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Bật chế độ khử răng cưa cho đồ họa mượt mà
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (mapImage != null) {
            g2d.drawImage(mapImage, 0, 0, getWidth(), getHeight(), null);
        }

        renderer.drawTrafficLights(g2d, intersections);
        for (Vehicle v : vehicles) {
            v.draw(g2d);
        }

        // --- CẬP NHẬT: VẼ KHUNG NỀN VOLUME DÀN NGANG CÙNG HÀNG NÚT BẤM ---
        if (volumeSlider != null) {
            // Hộp đen mờ lót nền cao cấp (X bắt đầu từ 380, rộng 240 để bọc cả chữ và slider)
            g2d.setColor(new Color(0, 0, 0, 160));
            g2d.fillRoundRect(380, 20, 240, 45, 12, 12);

            // Chữ hiển thị phần trăm màu trắng nằm gọn bên trái thanh kéo
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2d.drawString("Vol: " + volumeSlider.getValue() + "%", 395, 47);
        }

        // --- VẼ 2 NÚT CHỨC NĂNG FLAT DESIGN THEO HÀNG NGANG ---
        drawStyledButton(g2d, modeButtonRect, "Mode: " + (manualMode ? "MANUAL" : "AUTO"),
                manualMode ? new Color(231, 76, 60) : new Color(46, 204, 113));

        drawStyledButton(g2d, trafficButtonRect, "Traffic: " + (highTraffic ? "HIGH" : "LOW"),
                highTraffic ? new Color(230, 126, 34) : new Color(52, 152, 219));
    }

    private void drawStyledButton(Graphics2D g2d, Rectangle rect, String text, Color statusColor) {
        // Hiệu ứng bóng đổ
        g2d.setColor(new Color(0, 0, 0, 50));
        g2d.fillRoundRect(rect.x + 3, rect.y + 3, rect.width, rect.height, 12, 12);

        // Thân nút trắng
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 12, 12);

        // Viền nút
        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 12, 12);

        // Chấm tròn LED báo trạng thái
        g2d.setColor(statusColor);
        g2d.fillOval(rect.x + 15, rect.y + 17, 12, 12);

        // Chữ text hiển thị
        g2d.setColor(new Color(44, 62, 80));
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2d.drawString(text, rect.x + 35, rect.y + 28);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Smart City Traffic Simulation");
        Main panel = new Main();
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}