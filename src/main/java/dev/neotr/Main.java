package dev.neotr;

import org.jnativehook.GlobalScreen;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;

public class Main implements NativeKeyListener {
    private Clip currentClip = null; // Keep a reference to the currently playing clip
    private Random random = new Random(); // Random number generator
    static String appDataDir;
    static String soundFilePath;
    private boolean isKeyPressed = false;

    public static void main(String[] args) throws IOException {
        // Get the logger for "org.jnativehook" and set the level to off
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
        }
        catch (Exception ex) {
            System.err.println("There was a problem registering the native hook.");
            System.err.println(ex.getMessage());
            System.exit(1);
        }

        // Get the %appdata% directory
        appDataDir = System.getenv("APPDATA");

        System.out.println("Welcome to BetterKeys! This program will play a random sound every time you press a key. To add your own sounds navigate to %appdata%\\BetterKeys and place your .wav files there. \n\nMade by NeoTR");

        try {
            Path betterKeysDir = Paths.get(appDataDir, "BetterKeys");
            Files.createDirectories(betterKeysDir);

            String soundFileUrl = "https://files.catbox.moe/fb09ad.wav";

            Path soundFilePath = Paths.get(System.getenv("APPDATA"), "BetterKeys", "sound2.wav");

            ProcessBuilder processBuilder = new ProcessBuilder("curl", "-o", soundFilePath.toString(), soundFileUrl);
            Process process = processBuilder.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Sound file downloaded to: " + soundFilePath);
            } else {
                System.out.println("Failed to download sound file. curl exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        List<Path> wavFiles = Files.list(Paths.get(appDataDir, "BetterKeys"))
                .filter(path -> path.toString().endsWith(".wav"))
                .toList();

        for (int i = 0; i < wavFiles.size(); i++) {
            System.out.println((i + 1) + ": " + wavFiles.get(i));
        }

        System.out.print("Enter the number of the file you want to pick: ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        int fileNumber = Integer.parseInt(reader.readLine());

        Path selectedFile = wavFiles.get(fileNumber - 1);
        soundFilePath = selectedFile.toString();

        GlobalScreen.addNativeKeyListener(new Main());
    }

    public void nativeKeyPressed(NativeKeyEvent e) {
        if (!isKeyPressed) {
            isKeyPressed = true;
            try {
                if (currentClip != null && currentClip.isRunning()) {
                    currentClip.stop();
                }

                AudioInputStream audioIn = AudioSystem.getAudioInputStream(new File(soundFilePath));

                // Get a sound clip resource.
                currentClip = AudioSystem.getClip();

                currentClip.open(audioIn);

                // Get the volume control and randomize its value
                FloatControl volumeControl = (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);
                float currentVolume = volumeControl.getValue();

                float volumeChange = (random.nextFloat() - 0.5f) * 0.5f;
                float newVolume = currentVolume + volumeChange;
                newVolume = Math.max(volumeControl.getMinimum(), newVolume); // Ensure volume isn't too low
                newVolume = Math.min(volumeControl.getMaximum(), newVolume); // Ensure volume isn't too high
                volumeControl.setValue(newVolume);

                currentClip.start();
            } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void nativeKeyReleased(NativeKeyEvent e) {
        isKeyPressed = false;
    }

    public void nativeKeyTyped(NativeKeyEvent e) {
        // Handle key typed
    }
}