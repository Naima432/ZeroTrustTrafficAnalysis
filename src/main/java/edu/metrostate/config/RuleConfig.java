package edu.metrostate.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RuleConfig {
    public List<Integer> portMonitoring;
    public List<String> ipBlacklist;
    public List<String> ipWhitelist;
    public Map<String, Object> trafficVolumeThreshold;
    public List<String> protocolAnomalies;
    public Map<String, List<String>> timeBasedAccess;
    public Map<String, List<String>> geolocation;

    public static RuleConfig loadFromFile(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(path), RuleConfig.class);
    }
}
