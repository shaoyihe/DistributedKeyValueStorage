package ru.markovnikov.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by nikita on 07.05.16.
 */
public class PropertiesParser {
    private Map<Integer, String> addresses;
    private long timeout;
    public final static String CONFIG_PROPERTIES_FILE = "resources/dkvs.properties";

    public PropertiesParser(Map<Integer, String> addresses, long timeout) {
        this.addresses = addresses;
        this.timeout = timeout;
    }

    public int port(int id) {
        if (!addresses.containsKey(id)) return -1;
        return Integer.parseInt(addresses.get(id).split(":")[1]);
    }

    public String address(int id) {
        if (!addresses.containsKey(id)) return null;
        return addresses.get(id).split(":")[0];
    }

    public int nodesCount() {
        return addresses.size();
    }

    public static List<Integer> range(int min, int max) {
        return IntStream.range(min, max).boxed().collect(Collectors.toList());
    }

    public List<Integer> ids() {
        return range(0, nodesCount() - 1);
    }

    public static PropertiesParser readProperties() throws IOException {
        InputStream is = PropertiesParser.class.getClassLoader().getResourceAsStream(CONFIG_PROPERTIES_FILE);
        if (is == null)
            throw new FileNotFoundException("properties file '" + CONFIG_PROPERTIES_FILE + "' not found");
        Properties properties = new Properties();
        properties.load(is);
        long timeout = Long.parseLong(properties.getProperty("timeout"));
        Map<Integer, String> map = new HashMap<>();
        properties.entrySet().stream().filter(entry -> (entry.getKey() instanceof String && entry.getValue() instanceof String)).forEach(entry -> {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key.startsWith("node")) {
                int id = Integer.parseInt(key.split("\\.")[1]);
                map.put(id, value);
            }
        });
        return new PropertiesParser(Collections.unmodifiableMap(map), timeout);
    }


    public Map<Integer, String> getAddresses() {
        return addresses;
    }

    public void setAddresses(Map<Integer, String> addresses) {
        this.addresses = addresses;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}
