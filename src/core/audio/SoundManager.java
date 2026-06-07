package core.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SoundManager {
    private static final Map<String, Clip> loopingSirens = new HashMap<>();
    private static long lastHornTime = 0;


    // Quản lý âm lượng toàn cầu (0.0 đến 1.0)
    public static float volume = 0.7f;

    public static void setVolume(float newVolume) {
        volume = newVolume;
        if (volume < 0.0f) volume = 0.0f;
        if (volume > 1.0f) volume = 1.0f;
        updateLoopVolumes();
    }

    private static void applyVolume(Clip clip) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float db = (volume <= 0.0f) ? gainControl.getMinimum() : (float) (Math.log10(volume) * 20.0f);
                gainControl.setValue(db);
            }
        } catch (Exception e) {}
    }

    private static void updateLoopVolumes() {
        for (Clip clip : loopingSirens.values()) {
            if (clip != null && clip.isRunning()) {
                applyVolume(clip);
            }
        }
    }

    public static void updateEmergencySiren(String type, String filePath, boolean shouldPlay) {
        try {
            if (shouldPlay) {
                if (!loopingSirens.containsKey(type) || !loopingSirens.get(type).isRunning()) {
                    File soundFile = new File(filePath);
                    if (soundFile.exists()) {
                        AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
                        Clip clip = AudioSystem.getClip();
                        clip.open(audioInput);
                        applyVolume(clip);
                        clip.loop(Clip.LOOP_CONTINUOUSLY);
                        clip.start();
                        loopingSirens.put(type, clip);
                    }
                }
            } else {
                if (loopingSirens.containsKey(type)) {
                    Clip clip = loopingSirens.get(type);
                    if (clip != null && clip.isRunning()) {
                        clip.stop();
                        clip.close();
                    }
                    loopingSirens.remove(type);
                }
            }
        } catch (Exception e) {}
    }

    public static void playHornOnNearCollision() {
        long now = System.currentTimeMillis();
        if (now - lastHornTime < 1500) return;

        if (Math.random() < 0.50) {
            lastHornTime = now;
            playOneShot("src/resources/sounds/horn.wav");
        }
    }

    // Tiếng xi nhan (Được bảo vệ bằng khóa đồng bộ hóa)

    private static void playOneShot(String filePath) {
        new Thread(() -> {
            try {
                File soundFile = new File(filePath);
                if (soundFile.exists()) {
                    AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundFile);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioInput);
                    applyVolume(clip);
                    clip.start();
                    clip.addLineListener(event -> {
                        if (event.getType() == LineEvent.Type.STOP) clip.close();
                    });
                }
            } catch (Exception e) {}
        }).start();
    }
}