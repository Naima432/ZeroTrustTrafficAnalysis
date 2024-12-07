package edu.metrostate.gui;

import edu.metrostate.config.RuleConfig;
import edu.metrostate.model.Alert;
import edu.metrostate.monitor.AlertMonitor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Scanner;

public class AppController {
    @FXML
    private TableView<Alert> trafficTable;

    @FXML
    private TableColumn<Alert, String> timestampColumn;

    @FXML
    private TableColumn<Alert, String> ipColumn;

    @FXML
    private TableColumn<Alert, Integer> portColumn;

    @FXML
    private TableColumn<Alert, String> protocolColumn;

    @FXML
    private TableColumn<Alert, String> messageColumn;

    private final ObservableList<Alert> alertData = FXCollections.observableArrayList();
    private AlertMonitor alertMonitor;
    private Thread monitorThread;
    private boolean isSnortRunning = false;
    private RuleConfig demoRules;

    @FXML
    public void initialize() {
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        ipColumn.setCellValueFactory(new PropertyValueFactory<>("sourceIP"));
        portColumn.setCellValueFactory(new PropertyValueFactory<>("port"));
        protocolColumn.setCellValueFactory(new PropertyValueFactory<>("protocol"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));

        trafficTable.setItems(alertData);

        addSystemAlert("System initializing...");
        checkSnortAndInitialize();
    }

    private void checkSnortAndInitialize() {
        isSnortRunning = isSnortRunning();

        if (isSnortRunning) {
            startSnortMonitoring();
        } else {
            addSystemAlert("Snort is not running. Starting in demo mode...");

            try {
                String demoRulesPath = "src/main/resources/config/manual_rules.json";
                demoRules = RuleConfig.loadFromFile(demoRulesPath);
                addSystemAlert("Rules loaded in demo mode from " + demoRulesPath);
            } catch (IOException e) {
                addSystemAlert("Failed to load rules in demo mode: " + e.getMessage());
            }

            addDemoAlerts();
        }
    }

    private boolean isSnortRunning() {
        try {
            Process process = Runtime.getRuntime().exec("tasklist.exe /FI \"IMAGENAME eq snort.exe\"");
            Scanner scanner = new Scanner(process.getInputStream());
            while (scanner.hasNextLine()) {
                if (scanner.nextLine().toLowerCase().contains("snort.exe")) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private void startSnortMonitoring() {
        String[] possibleLogPaths = {
                "C:\\Snort\\log\\alert",
                "C:\\Snort\\log\\alert.ids",
                "C:\\Snort\\log\\snort.alert",
                System.getProperty("user.home") + "\\Snort\\log\\alert"
        };

        String logPath = null;
        for (String path : possibleLogPaths) {
            File logFile = new File(path);
            if (logFile.exists() || logFile.getParentFile().exists()) {
                logPath = path;
                break;
            }
        }

        if (logPath != null) {
            try {
                alertMonitor = new AlertMonitor(logPath, this);
                monitorThread = new Thread(alertMonitor);
                monitorThread.setDaemon(true);
                monitorThread.start();
                addSystemAlert("Connected to Snort logs at: " + logPath);
                addSystemAlert("Monitoring for network alerts...");
            } catch (Exception e) {
                addSystemAlert("Failed to start Snort monitoring: " + e.getMessage());
                addSystemAlert("Switching to demo mode...");
                addDemoAlerts();
            }
        } else {
            addSystemAlert("Snort log path not found. Starting in demo mode...");
            addDemoAlerts();
        }
    }

    private void addDemoAlerts() {
        Alert[] demoAlerts = {
                new Alert("192.168.1.100", 80, "TCP",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "Demo Alert: HTTP Traffic Detected"),
                new Alert("10.0.0.15", 443, "TCP",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "Demo Alert: HTTPS Connection"),
                new Alert("172.16.0.50", 22, "TCP",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "Demo Alert: SSH Connection Attempt")
        };

        Platform.runLater(() -> {
            addSystemAlert("Demo Mode Active - Showing sample alerts");
            for (Alert alert : demoAlerts) {
                if (demoRules == null || isAlertValidForDemo(alert)) {
                    alertData.add(alert);
                }
            }
        });
    }

    private boolean isAlertValidForDemo(Alert alert) {
        if (demoRules == null) return true;

        if (demoRules.ipBlacklist != null && demoRules.ipBlacklist.contains(alert.getSourceIP())) {
            addSystemAlert("Blocked alert from blacklisted IP: " + alert.getSourceIP());
            return false;
        }

        if (demoRules.ipWhitelist != null && demoRules.ipWhitelist.contains(alert.getSourceIP())) {
            addSystemAlert("Alert ignored from whitelisted IP: " + alert.getSourceIP());
            return false;
        }

        if (demoRules.portMonitoring != null && demoRules.portMonitoring.contains(alert.getPort())) {
            addSystemAlert("Monitored port activity detected: " + alert.getPort());
        }

        if (demoRules.protocolAnomalies != null && demoRules.protocolAnomalies.contains(alert.getProtocol().toLowerCase())) {
            addSystemAlert("Anomalous protocol detected: " + alert.getProtocol());
        }

        return true;
    }

    public void addAlert(Alert alert) {
        Platform.runLater(() -> {
            alertData.add(0, alert);
            if (alertData.size() > 1000) {
                alertData.remove(1000, alertData.size());
            }
        });
    }

    private void addSystemAlert(String message) {
        Alert systemAlert = new Alert("System", 0, "INFO",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), message);
        Platform.runLater(() -> alertData.add(0, systemAlert));
    }

    @FXML
    private void clearAlerts() {
        alertData.clear();
        addSystemAlert("Alert history cleared");

        if (!isSnortRunning) {
            addDemoAlerts();
        } else {
            addSystemAlert("Monitoring for new alerts...");
        }
    }

    @FXML
    private void SendToEmailButtonClicked() {
        addSystemAlert("Send to Email button clicked. Attempting to send all alerts.");

        if (alertData.isEmpty()) {
            addSystemAlert("No alerts to send.");
            return;
        }

        StringBuilder emailContent = new StringBuilder("Current Alerts:\n");
        for (Alert a : alertData) {
            emailContent.append(a.getTimestamp()).append(" - ")
                    .append(a.getSourceIP()).append(":").append(a.getPort())
                    .append(" (").append(a.getProtocol()).append(") ")
                    .append(a.getMessage()).append("\n");
        }

        sendAllAlertsEmail(emailContent.toString());
    }

    private void sendAllAlertsEmail(String content) {
        // UPDATE THIS SECTION WITH REAL SMTP SETTINGS
        // Example: Using Gmail SMTP with an app password
        String to = "recipient@example.com";  // Replace with the recipient email
        String from = "your_gmail@gmail.com"; // Replace with your Gmail address
        String host = "smtp.gmail.com";

        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.starttls.enable", "true");

        // Provide your Gmail username and an App Password
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // Replace "your_gmail@gmail.com" and "your_app_password" with actual credentials
                return new PasswordAuthentication("your_gmail@gmail.com", "your_app_password");
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            // You can add multiple recipients if needed
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("Zero Trust Network Alert Summary");
            message.setText(content);

            Transport.send(message);
            addSystemAlert("All alerts email sent successfully.");
        } catch (MessagingException mex) {
            addSystemAlert("Failed to send email: " + mex.getMessage());
        }
    }

    @FXML
    private void generateReportClicked() {
        addSystemAlert("Generating report...");

        File reportDir = new File("reports");
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }

        String filename = "report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
        File reportFile = new File(reportDir, filename);

        try (PrintWriter out = new PrintWriter(new FileWriter(reportFile))) {
            out.println("Zero Trust Network Traffic Analysis Report");
            out.println("Generated at: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            out.println("\nCurrent Alerts:");
            for (Alert a : alertData) {
                out.println(a.getTimestamp() + " - " + a.getSourceIP() + ":" + a.getPort()
                        + " (" + a.getProtocol() + ") " + a.getMessage());
            }
            addSystemAlert("Report generated: " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            addSystemAlert("Failed to generate report: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (alertMonitor != null) {
            alertMonitor.stop();
        }
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
        addSystemAlert("System shutting down...");
    }
}
