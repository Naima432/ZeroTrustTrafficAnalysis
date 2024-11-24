package edu.metrostate.gui;

import edu.metrostate.model.Alert;
import edu.metrostate.monitor.AlertMonitor;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    @FXML
    public void initialize() {
        // Configure the table columns
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        ipColumn.setCellValueFactory(new PropertyValueFactory<>("sourceIP"));
        portColumn.setCellValueFactory(new PropertyValueFactory<>("port"));
        protocolColumn.setCellValueFactory(new PropertyValueFactory<>("protocol"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));

        // Set up the table data
        trafficTable.setItems(alertData);

        // initial system message
        addSystemAlert("System initializing...");

        // Check Snort status and start mode
        checkSnortAndInitialize();
    }

    private void checkSnortAndInitialize() {
        // detect running Snort instance
        isSnortRunning = isSnortRunning();

        if (isSnortRunning) {
            startSnortMonitoring();
        } else {
            addSystemAlert("Snort is not running. Starting in demo mode...");
            addDemoAlerts();
        }
    }

    private boolean isSnortRunning() {
        try {
            Process process = Runtime.getRuntime().exec("tasklist.exe /FI \"IMAGENAME eq snort.exe\"");
            java.util.Scanner scanner = new java.util.Scanner(process.getInputStream());

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
                new Alert(
                        "192.168.1.100",
                        80,
                        "TCP",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "Demo Alert: HTTP Traffic Detected"
                ),
                new Alert(
                        "10.0.0.15",
                        443,
                        "TCP",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "Demo Alert: HTTPS Connection"
                ),
                new Alert(
                        "172.16.0.50",
                        53,
                        "UDP",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "Demo Alert: DNS Query"
                ),
                new Alert(
                        "192.168.1.200",
                        22,
                        "TCP",
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "Demo Alert: SSH Connection Attempt"
                )
        };

        Platform.runLater(() -> {
            addSystemAlert("Demo Mode Active - Showing sample alerts");
            for (Alert alert : demoAlerts) {
                alertData.add(alert);
            }
        });
    }

    public void addAlert(Alert alert) {
        Platform.runLater(() -> {
            alertData.add(0, alert);

            // Limit the number of displayed alerts to prevent memory issues
            if (alertData.size() > 1000) {
                alertData.remove(1000, alertData.size());
            }
        });
    }

    public void addSystemAlert(String message) {
        Alert systemAlert = new Alert(
                "System",
                0,
                "INFO",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                message
        );
        addAlert(systemAlert);
    }

    @FXML
    private void clearAlerts() {
        alertData.clear();
        addSystemAlert("Alert history cleared");

        // If in demo mode, add new demo alerts
        if (!isSnortRunning) {
            addDemoAlerts();
        } else {
            addSystemAlert("Monitoring for new alerts...");
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