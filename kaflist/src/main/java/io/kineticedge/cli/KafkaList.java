package io.kineticedge.cli;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.DescribeClientQuotasOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.quota.ClientQuotaFilter;
import org.apache.kafka.common.resource.ResourcePatternFilter;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class KafkaList {

  private static boolean debug = false;

  private static final ListTopicsOptions LIST_TOPICS_OPTIONS = new ListTopicsOptions().listInternal(true);


  public static void main(String[] args) {

    if (args.length == 0) {
      return;
    }

    Map<String, String> argMap = parseArgs(args);

    final String bootstrapServers = removeSurroundingQuotes(argMap.get("bootstrap-server"));

    // gracefully return nothing, user has not set bootstrap servers.
    if (bootstrapServers == null || bootstrapServers.isEmpty()) {
      return;
    }

    final String commandConfigFile = removeSurroundingQuotes(argMap.get("command-config"));
    final long timeoutMs = getTimeoutMs(argMap.get("timeout"));
    debug = argMap.containsKey("debug");

    final Map<String, Object> config = new HashMap<>();
    config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.putAll(toMap(load(commandConfigFile)));

    config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, (int) timeoutMs);
    config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, (int) timeoutMs);

    String command = args[0];

    switch (command) {
      case "topics":
      case "--topics":
        topics(config, timeoutMs).forEach(System.out::println);
        break;
      case "groups":
      case "--groups":
        groups(config, timeoutMs).forEach(System.out::println);
        break;
      case "principals":
      case "--principals":
        principals(config, timeoutMs).forEach(System.out::println);
        break;
      case "brokers":
      case "--brokers":
        brokers(config, timeoutMs).forEach(System.out::println);
        break;
      case "client-ids":
      case "--client-ids":
        clients("client-id", config, timeoutMs).forEach(System.out::println);
        break;
      case "ips":
      case "--ips":
        clients("ip", config, timeoutMs).forEach(System.out::println);
        break;
      default:
    }

  }

  private static List<String> topics(final Map<String, Object> config, final long timeoutMs) {
    try {
      try (AdminClient adminClient = AdminClient.create(config)) {
        return adminClient.listTopics(LIST_TOPICS_OPTIONS).names().get(timeoutMs, TimeUnit.MILLISECONDS).stream().sorted().toList();
      } catch (final ExecutionException | InterruptedException | TimeoutException e) {
        if (debug) {
          e.printStackTrace();
        }
        Thread.currentThread().interrupt();
        return List.of();
      }
    } catch (KafkaException ce) {
      if (debug) {
        ce.printStackTrace();
      }
      return List.of();
    }

  }

  private static List<String> groups(final Map<String, Object> config, final long timeoutMs) {
    try {
      try (AdminClient adminClient = AdminClient.create(config)) {
        return adminClient.listConsumerGroups().all().get(timeoutMs, TimeUnit.MILLISECONDS).stream().map(ConsumerGroupListing::groupId).sorted().toList();
      } catch (final ExecutionException | InterruptedException | TimeoutException e) {
        if (debug) {
          e.printStackTrace();
        }
        Thread.currentThread().interrupt();
        return List.of();
      }
    } catch (KafkaException ce) {
      if (debug) {
        ce.printStackTrace();
      }
      return List.of();
    }
  }

  private static Set<String> principals(final Map<String, Object> config, final long timeoutMs) {
    try {
      try (AdminClient adminClient = AdminClient.create(config)) {
        AclBindingFilter filter = new AclBindingFilter(ResourcePatternFilter.ANY, AccessControlEntryFilter.ANY);
        return adminClient.describeAcls(filter).values().get(timeoutMs, TimeUnit.MILLISECONDS).stream().map(binding -> binding.entry().principal()).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
      } catch (final ExecutionException | InterruptedException | TimeoutException e) {
        if (debug) {
          e.printStackTrace();
        }
        Thread.currentThread().interrupt();
        return Set.of();
      }
    } catch (KafkaException ce) {
      if (debug) {
        ce.printStackTrace();
      }
      return Set.of();
    }
  }

  private static Set<String> brokers(final Map<String, Object> config, final long timeoutMs) {
    try (AdminClient adminClient = AdminClient.create(config)) {
      return adminClient.describeCluster().nodes().get(timeoutMs, TimeUnit.MILLISECONDS).stream().map(n -> Integer.toString(n.id())).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      if (debug) {
        e.printStackTrace();
      }
      Thread.currentThread().interrupt();
      return Set.of();
    }
  }

  private static Set<String> clients(final String type, final Map<String, Object> config, final long timeoutMs) {
    try (AdminClient adminClient = AdminClient.create(config)) {
      return adminClient.describeClientQuotas(ClientQuotaFilter.all(), new DescribeClientQuotasOptions().timeoutMs((int) timeoutMs)).entities().get().keySet().stream()
              .flatMap(v -> v.entries().entrySet().stream().filter(e -> type.equals(e.getKey())).map(Map.Entry::getValue)).sorted().collect(Collectors.toCollection(LinkedHashSet::new));
    } catch (InterruptedException | ExecutionException e) {
      if (debug) {
        e.printStackTrace();
      }
      Thread.currentThread().interrupt();
      return Set.of();
    }
  }

  private static Properties load(final String file) {
    Properties properties = new Properties();

    if (file == null) {
      return properties;
    }

    try (FileInputStream configStream = new FileInputStream(file)) {
      properties.load(configStream);
    } catch (Exception e) {
      // ignore
    }
    return properties;
  }

  private static Map<String, Object> toMap(final Properties properties) {
    Map<String, Object> map = new HashMap<>();
    for (String name : properties.stringPropertyNames()) {
      map.put(name, properties.getProperty(name));
    }
    return map;
  }

  private static Map<String, String> parseArgs(String[] args) {
    Map<String, String> argMap = new HashMap<>();
    for (int i = 1; i < args.length; i++) {
      if (args[i].startsWith("--")) {
        String key = args[i].substring(2);
        if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
          argMap.put(key, args[++i]);
        }
      }
    }
    return argMap;
  }

  private static long getTimeoutMs(String timeout) {
    long timeoutMs;
    if (timeout != null) {
      try {
        timeoutMs = Long.parseLong(timeout);
      } catch (NumberFormatException e) {
        timeoutMs = 1000L;
      }
    } else {
      timeoutMs = 1000L;
    }
    return timeoutMs;
  }

  private static String removeSurroundingQuotes(String str) {
    if (str == null || str.length() < 2) {
      return str;
    }

    char firstChar = str.charAt(0);
    char lastChar = str.charAt(str.length() - 1);

    // Check if both the first and last characters are either double quotes or single quotes
    if ((firstChar == '"' && lastChar == '"') || (firstChar == '\'' && lastChar == '\'')) {
      return str.substring(1, str.length() - 1);
    }

    return str;
  }

}
