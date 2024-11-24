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
import java.util.List;
import java.util.logging.Logger;

public class AlertMonitor implements Runnable {

    private final String alertFilePath;
    private final AppController controller;
    private boolean running = true;
    private RuleConfig rules;
    private static final Logger logger = Logger.getLogger(AlertMonitor.class.getName());

    public AlertMonitor(String alertFilePath, AppController controller) {
        this.alertFilePath = alertFilePath;
        this.controller = controller;

        // Load custom rules from the RuleConfig
        try {
            this.rules = RuleConfig.loadFromFile("src/main/resources/config/manual_rules.json");
            controller.addSystemAlert("Custom rules loaded successfully from manual_rules.json");
        } catch (IOException e) {
            controller.addSystemAlert("Failed to load custom rules: " + e.getMessage());
            logger.severe("Failed to load custom rules: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            monitorAlerts();
        } catch (IOException | InterruptedException e) {
            controller.addSystemAlert("Error monitoring alerts: " + e.getMessage());
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
                if (alert != null && isAlertValid(alert)) {
                    controller.addAlert(alert);
                }
            }
            return file.length();
        } catch (IOException e) {
            controller.addSystemAlert("Error processing alerts: " + e.getMessage());
            logger.severe("Error processing alerts: " + e.getMessage());
            return lastSize;
        }
    }

    private Alert parseAlert(String line) {
        // Parse Snort alert line into an Alert object
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

    private boolean isAlertValid(Alert alert) {
        // Validate the alert based on custom rules

        // Check IP Blacklist
        if (rules.ipBlacklist != null && rules.ipBlacklist.contains(alert.getSourceIP())) {
            controller.addSystemAlert("Blocked alert from blacklisted IP: " + alert.getSourceIP());
            return true;
        }

        // Check IP Whitelist
        if (rules.ipWhitelist != null && rules.ipWhitelist.contains(alert.getSourceIP())) {
            return false; // Ignore alerts from whitelisted IPs
        }

        // Check Port Monitoring
        if (rules.portMonitoring != null && rules.portMonitoring.contains(alert.getPort())) {
            controller.addSystemAlert("Monitored port activity detected: " + alert.getPort());
            return true;
        }

        // Check Protocol Anomalies
        if (rules.protocolAnomalies != null && rules.protocolAnomalies.contains(alert.getProtocol().toLowerCase())) {
            controller.addSystemAlert("Anomalous protocol detected: " + alert.getProtocol());
            return true;
        }

        // Placeholder for Traffic Volume Threshold (if needed later)
        // This requires maintaining state for observed traffic, which is not yet implemented

        // Time-Based Access Checks (if applicable)
        if (rules.timeBasedAccess != null) {
            LocalDateTime now = LocalDateTime.now();
            String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));

            if (rules.timeBasedAccess.containsKey("blocked_hours")) {
                List<String> blockedHours = rules.timeBasedAccess.get("blocked_hours");
                for (String range : blockedHours) {
                    String[] hours = range.split("-");
                    if (currentTime.compareTo(hours[0]) >= 0 && currentTime.compareTo(hours[1]) <= 0) {
                        controller.addSystemAlert("Blocked time-based access for alert at: " + currentTime);
                        return true;
                    }
                }
            }

            if (rules.timeBasedAccess.containsKey("allowed_hours")) {
                List<String> allowedHours = rules.timeBasedAccess.get("allowed_hours");
                boolean allowed = false;
                for (String range : allowedHours) {
                    String[] hours = range.split("-");
                    if (currentTime.compareTo(hours[0]) >= 0 && currentTime.compareTo(hours[1]) <= 0) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    controller.addSystemAlert("Alert blocked outside allowed hours: " + currentTime);
                    return true;
                }
            }
        }

        // If no custom rules matched, consider the alert valid
        return true;
    }

    public void stop() {
        running = false;
    }
}
