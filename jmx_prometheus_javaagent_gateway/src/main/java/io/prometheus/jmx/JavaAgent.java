package io.prometheus.jmx;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.hotspot.DefaultExports;
import io.prometheus.jmx.gateway.PushGatewayScrape;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaAgent {

    static PushGatewayScrape gatewayScrape;

    public static void agentmain(String agentArgument, Instrumentation instrumentation) throws Exception {
        premain(agentArgument, instrumentation);
    }

    public static void premain(String agentArgument, Instrumentation instrumentation) throws Exception {
        try {
            Config config = parseConfig(agentArgument, "127.0.0.1");

            new BuildInfoCollector().register();
            new JmxCollector(new File(config.file)).register();
            DefaultExports.initialize();
            gatewayScrape = new PushGatewayScrape(CollectorRegistry.defaultRegistry, config.host, config.port);
        }
        catch (IllegalArgumentException e) {
            System.err.println("Usage: -javaagent:/path/to/JavaAgent.jar=[host:]<port>:<yaml configuration file> " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Parse the Java Agent configuration. The arguments are typically specified to the JVM as a javaagent as
     * {@code -javaagent:/path/to/agent.jar=<CONFIG>}. This method parses the {@code <CONFIG>} portion.
     * @param args provided agent args
     * @return configuration to use for our application
     */
    public static Config parseConfig(String args, String defaultHost) {
        Pattern pattern = Pattern.compile(
                "^(?:((?:[\\w.]+)|(?:\\[.+])):)?" +  // host name, or ipv4, or ipv6 address in brackets
                        "(\\d{1,5}):" +              // port
                        "(.+)");                     // config file

        Matcher matcher = pattern.matcher(args);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Malformed arguments - " + args);
        }

        String givenHost = matcher.group(1);
        String givenPort = matcher.group(2);
        String givenConfigFile = matcher.group(3);

        int port = Integer.parseInt(givenPort);

        if (givenHost != null && !givenHost.isEmpty()) {
            // use provided host
        }
        else {
            givenHost = defaultHost;
        }

        return new Config(givenHost, port, givenConfigFile);
    }

    static class Config {
        String host;
        int port;
        String file;

        Config(String host, int port, String file) {
            this.host = host;
            this.port = port;
            this.file = file;
        }
    }
}
