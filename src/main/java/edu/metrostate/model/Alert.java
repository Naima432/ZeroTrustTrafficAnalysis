package edu.metrostate.model;

public class Alert {
    private String sourceIP;
    private int port;
    private String protocol;
    private String timestamp;
    private String message;

    public Alert() {
    }

    public Alert(String sourceIP, int port, String protocol, String timestamp, String message) {
        this.sourceIP = sourceIP;
        this.port = port;
        this.protocol = protocol;
        this.timestamp = timestamp;
        this.message = message;
    }

    public String getSourceIP() {
        return sourceIP;
    }

    public void setSourceIP(String sourceIP) {
        this.sourceIP = sourceIP;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "Alert{" +
                "timestamp='" + timestamp + '\'' +
                ", sourceIP='" + sourceIP + '\'' +
                ", port=" + port +
                ", protocol='" + protocol + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}