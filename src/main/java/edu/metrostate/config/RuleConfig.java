package edu.metrostate.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RuleConfig {

    @JsonProperty("port_monitoring")
    public List<Integer> portMonitoring;

    @JsonProperty("ip_blacklist")
    public List<String> ipBlacklist;

    @JsonProperty("ip_whitelist")
    public List<String> ipWhitelist;

    @JsonProperty("traffic_volume_threshold")
    public Map<String, Object> trafficVolumeThreshold;

    @JsonProperty("protocol_anomalies")
    public List<String> protocolAnomalies;

    @JsonProperty("time_based_access")
    public Map<String, List<String>> timeBasedAccess;

    @JsonProperty("geolocation")
    public Map<String, List<String>> geolocation;

    public static RuleConfig loadFromFile(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(path), RuleConfig.class);
    }
}
