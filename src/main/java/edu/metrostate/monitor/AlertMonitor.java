package edu.metrostate.monitor;

import edu.metrostate.config.RuleConfig;
import edu.metrostate.gui.AppController;
import edu.metrostate.model.Alert;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public class AlertMonitor implements Runnable {

    private final String alertFilePath;
    private final AppController controller;
    private boolean running = true;
    private RuleConfig rules; // expose via getter
    private static final Logger logger = Logger.getLogger(AlertMonitor.class.getName());

    public AlertMonitor(String alertFilePath, AppController controller) {
        this.alertFilePath = alertFilePath;
        this.controller = controller;

        try {
            String rulesPath = "src/main/resources/config/manual_rules.json";
            this.rules = RuleConfig.loadFromFile(rulesPath);
            controller.addAlert(new Alert("System", 0, "INFO",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "Custom rules loaded successfully from " + rulesPath));
        } catch (IOException e) {
            controller.addAlert(new Alert("System", 0, "INFO",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "Failed to load custom rules: " + e.getMessage()));
            logger.severe("Failed to load custom rules: " + e.getMessage());
        }
    }

    public RuleConfig getRules() {
        return this.rules;
    }

    @Override
    public void run() {
        try {
            monitorAlerts();
        } catch (IOException | InterruptedException e) {
            controller.addAlert(new Alert("System", 0, "INFO",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "Error monitoring alerts: " + e.getMessage()));
            logger.severe("Error monitoring alerts: " + e.getMessage());
        }
    }

    private void monitorAlerts() throws IOException, InterruptedException {
        Path alertFile = Paths.get(alertFilePath).getParent();
        String fileName = Paths.get(alertFilePath).getFileName().toString();

        WatchService watchService = FileSystems.getDefault().newWatchService();
        alertFile.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

        long lastSize = new File(alertFilePath).length();

        while (running) {
            WatchKey key = watchService.take();

            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                    Path changed = (Path) event.context();
                    if (changed.endsWith(fileName)) {
                        lastSize = processAlerts(lastSize);
                    }
                }
            }
            key.reset();
        }
    }

    private long processAlerts(long lastSize) {
        try (RandomAccessFile file = new RandomAccessFile(alertFilePath, "r")) {
            file.seek(lastSize);
            String line;
            while ((line = file.readLine()) != null) {
                Alert alert = parseAlert(line);
                // Validation handled by controller using getRules()
                if (alert != null) {
                    controller.addAlert(alert);
                }
            }
            return file.length();
        } catch (IOException e) {
            controller.addAlert(new Alert("System", 0, "INFO",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "Error processing alerts: " + e.getMessage()));
            logger.severe("Error processing alerts: " + e.getMessage());
            return lastSize;
        }
    }

    private Alert parseAlert(String line) {
        line = line.replace("\"", "");
        String[] fields = line.split(",");
        if (fields.length >= 7) {
            String timestamp = fields[0];
            String msg = fields[1];
            String protocol = fields[2];
            String srcIP = fields[3];
            int srcPort = Integer.parseInt(fields[4]);
            return new Alert(srcIP, srcPort, protocol, timestamp, msg);
        }
        return null;
    }

    public void stop() {
        running = false;
    }
}
