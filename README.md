### Zero Trust Network Traffic Analysis
This project provides a Zero Trust network traffic monitoring and alerting system. It integrates with Snort for real-time intrusion detection and can run in a demo mode when Snort is not available. Alerts are displayed in a JavaFX GUI, and custom rules are applied to determine which alerts are blocked or flagged. The system can generate reports and send alert summaries via email.

### Features
Zero Trust Monitoring: Applies custom rules to identify and block suspicious alerts.
Integration with Snort: If Snort is running and producing alerts, the application monitors Snort’s log file in real time.
IDS and IPS Modes:
IDS: Intrusion detection with Snort to monitor traffic passively.
IPS: Intrusion prevention using Snort in inline mode to actively block malicious traffic.
Demo Mode: If Snort is not running, the system falls back to demo mode, showing sample alerts for testing and presentation.
Custom Rules: Loads rules from manual_rules.json, which can specify blacklists, whitelists, ports to monitor, protocol anomalies, and time-based access.
Email Notifications: Send a summary of all current alerts via email by clicking a button in the GUI.
Report Generation: Generate a text-based report of current alerts at any time.
### Prerequisites
Java JDK 20+
Gradle (The project includes a Gradle wrapper, so no separate installation is needed if you use ./gradlew commands.)
JavaFX 20+ (Handled by the org.openjfx.javafxplugin in build.gradle.)
Snort (Optional): To run in real mode with live alerts.
Internet Connection (for Email): If using a real SMTP server for sending emails.
### Directory Structure
Ensure your directory structure looks like this:


ZeroTrustTrafficAnalysis
├── build.gradle
├── gradlew
├── gradlew.bat
├── start_snort_ids.bat        
├── start_snort_ips.bat 
├── src
│   ├── main
│   │   ├── java
│   │   │   └── edu
│   │   │       └── metrostate
│   │   │           ├── config
│   │   │           │   └── RuleConfig.java
│   │   │           ├── gui
│   │   │           │   ├── App.java
│   │   │           │   ├── AppController.java
│   │   │           │   └── fxml
│   │   │           │       └── App.fxml
│   │   │           ├── model
│   │   │           │   └── Alert.java
│   │   │           └── monitor
│   │   │               └── AlertMonitor.java
│   │   └── resources
│   │       ├── config
│   │       │   └── manual_rules.json
│   │       └── edu
│   │           └── metrostate
│   │               └── gui
│   │                   └── fxml
│   │                       └── App.fxml
│   └── test
└── gradle
    └── wrapper
### Setting Up Snort
Install Snort and configure it to log alerts in C:\Snort\log\alert (or another directory specified in the code).

Run Snort from PowerShell:

powershell:
cd C:\Snort\bin
.\snort.exe -c "C:\Snort\etc\snort.conf" -l "C:\Snort\log" -A fast
Replace paths as needed. Once running, Snort will write alerts to C:\Snort\log\alert.

When you start the Java application, it will detect Snort and read alerts from the configured log file.

### Running in Demo Mode
If Snort is not running, the application automatically falls back to demo mode and shows sample alerts. This mode is useful for testing and presentations.

## Batch Scripts for IDS and IPS
Two batch scripts are provided to simplify starting Snort in IDS or IPS mode:

IDS Mode (start_snort_ids.bat):

Runs Snort in intrusion detection mode.
Logs alerts to C:\Snort\log\alert.

@echo off
REM Start Snort in IDS mode
snort.exe -i eth0 -c C:\Snort\etc\snort.conf -A console -l C:\Snort\log
pause
IPS Mode (start_snort_ips.bat):

Runs Snort in intrusion prevention mode.
Uses afpacket to actively block malicious traffic.

@echo off
REM Start Snort in IPS mode
snort.exe -Q --daq afpacket --daq-var device=eth0 -c C:\Snort\etc\snort.conf -A console
pause

## How to Use Batch Scripts
Place the batch files in the root directory of your project (e.g., ZeroTrustTrafficAnalysis).
Ensure Snort's executable is in your PATH or use its full path in the script.
Run the appropriate script by double-clicking it or executing it in the terminal.
Running in IDS Mode
Command:
.\start_snort_ids.bat
This mode passively monitors traffic and logs alerts to C:\Snort\log\alert.
Running in IPS Mode
Command:
.\start_snort_ips.bat
This mode actively blocks malicious traffic while monitoring and logging alerts.

### Building and Running the Application
From the project root directory, run:

./gradlew clean build run
On Windows PowerShell, you may need:

.\gradlew clean build run
This will compile and run the JavaFX application. An 800x600 window will open, showing a table of alerts and buttons at the top for clearing alerts, sending emails, and generating reports.

### Custom Rules
manual_rules.json in src/main/resources/config/ defines your custom rules:

ip_blacklist: Block all alerts from these IPs.
ip_whitelist: Ignore (don’t show) alerts from these IPs.
port_monitoring: Highlight or note activity on these ports.
protocol_anomalies: Flag certain protocols as anomalous.
time_based_access: Define allowed and blocked hours.
traffic_volume_threshold: Set volume-based criteria (simulated in code).
You can edit manual_rules.json to change these rules. The application loads these rules at startup.

### Sending Emails
The email feature is configured in AppController.java in the sendAllAlertsEmail method.

Steps to Enable Real Email Sending:
You can use a SMTP Server:

For Gmail:

Enable "App Passwords" in your Google Account security settings.
In the code, set:
String host = "smtp.gmail.com";
String from = "your_gmail@gmail.com";
String to = "recipient@example.com";
And replace "your_gmail@gmail.com" and "your_app_password" with your actual credentials.
If using another SMTP server, update host, port, and authentication details accordingly.

###  Locally with MailHog (Optional):

Run MailHog on your machine.
Set host = "localhost" and port = "1025".
Remove authentication if not required.
Check MailHog’s web interface to see if the email was "sent."
Once configured correctly, clicking "Send to Email" will send a summary of all current alerts to the specified recipient.

### Generating Reports
Click "Generate Report" to create a timestamped text file in the reports/ directory. This file lists all current alerts at the time of generation.

### Troubleshooting
Failed to Load Rules:
Ensure manual_rules.json is in src/main/resources/config/. After building, it should appear in build/resources/main/config/.

Snort Not Detected:
Check that Snort is actually running, and the process name is snort.exe. Also confirm the log file exists at one of the paths checked by the code.

Email Sending Errors: Verify SMTP settings and credentials. Check the logs for detailed error messages (e.g., "Failed to send email: ...").
