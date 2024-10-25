package edu.metrostate.monitor;

import edu.metrostate.gui.AppController;
import edu.metrostate.model.Alert;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlertMonitor implements Runnable {
    private final String alertFilePath;
    private final AppController controller;
    private volatile boolean running = true;
    private static final Pattern ALERT_PATTERN = Pattern.compile(
            "\\[(\\d+:\\d+:\\d+)\\] \\[([^\\]]+)\\] \"([^\"]+)\" \\{([^}]+)\\} ([\\d.]+):(\\d+) -> ([\\d.]+):(\\d+)"
    );

    public AlertMonitor(String alertFilePath, AppController controller) {
        this.alertFilePath = alertFilePath;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            Path path = Paths.get(alertFilePath).getParent();
            WatchService watchService = FileSystems.getDefault().newWatchService();
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            // Initial read of existing content
            processExistingAlerts();

            // Watch for new alerts
            while (running) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Path changed = (Path) event.context();
                        if (changed.toString().contains("alert")) {
                            processNewAlerts();
                        }
                    }
                }
                key.reset();
            }
        } catch (Exception e) {
            controller.addAlert(createErrorAlert("Monitoring error: " + e.getMessage()));
        }
    }

    private void processExistingAlerts() {
        try {
            File file = new File(alertFilePath);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        processAlertLine(line);
                    }
                }
            }
        } catch (IOException e) {
            controller.addAlert(createErrorAlert("Error reading existing alerts: " + e.getMessage()));
        }
    }

    private void processNewAlerts() {
        try {
            // Read only new content since last read
            try (RandomAccessFile file = new RandomAccessFile(alertFilePath, "r")) {
                file.seek(Math.max(0, file.length() - 4096)); // Read last 4KB of file
                String line;
                while ((line = file.readLine()) != null) {
                    processAlertLine(line);
                }
            }
        } catch (IOException e) {
            controller.addAlert(createErrorAlert("Error reading new alerts: " + e.getMessage()));
        }
    }

    private void processAlertLine(String line) {
        try {
            Matcher matcher = ALERT_PATTERN.matcher(line);
            if (matcher.find()) {
                Alert alert = new Alert(
                        matcher.group(5), // source IP
                        Integer.parseInt(matcher.group(6)), // source port
                        matcher.group(4), // protocol
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        matcher.group(3) // message
                );
                controller.addAlert(alert);
            }
        } catch (Exception e) {
            // skip malformed lines
        }
    }

    private Alert createErrorAlert(String message) {
        return new Alert(
                "System",
                0,
                "ERROR",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                message
        );
    }

    public void stop() {
        running = false;
    }
}