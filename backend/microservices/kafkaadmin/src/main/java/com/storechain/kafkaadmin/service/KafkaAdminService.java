package com.storechain.kafkaadmin.service;

import com.storechain.kafkaadmin.dto.PartitionsRequest;
import com.storechain.kafkaadmin.dto.TopicConfigRequest;
import com.storechain.kafkaadmin.dto.TopicRequest;
import com.storechain.kafkaadmin.exception.BusinessRuleException;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
public class KafkaAdminService {

    private static final long TIMEOUT_SECONDS = 10;

    @Autowired
    private Admin admin;

    // ------------------------------------------------------------------ clúster

    public Map<String, Object> cluster() {
        DescribeClusterResult cluster = admin.describeCluster();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("clusterId", await(cluster.clusterId()));
        Node controller = await(cluster.controller());
        result.put("controller", controller == null ? null : controller.id());
        result.put("brokers", await(cluster.nodes()).stream()
                .map(n -> Map.of("id", n.id(), "host", n.host(), "port", n.port()))
                .sorted(Comparator.comparing(m -> (Integer) m.get("id")))
                .toList());
        return result;
    }

    // ------------------------------------------------------------------ tópicos

    public List<Map<String, Object>> listTopics() {
        Set<String> names = await(admin.listTopics().names());
        return names.stream().sorted().map(this::describeTopic).toList();
    }

    public Map<String, Object> describeTopic(String name) {
        TopicDescription description = await(admin.describeTopics(List.of(name)).topicNameValues().get(name));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", description.name());
        result.put("internal", description.isInternal());
        result.put("partitions", description.partitions().size());
        result.put("replicationFactor", description.partitions().isEmpty() ? 0
                : description.partitions().get(0).replicas().size());
        result.put("partitionDetails", description.partitions().stream().map(p -> {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("partition", p.partition());
            detail.put("leader", p.leader() == null ? null : p.leader().id());
            detail.put("replicas", p.replicas().stream().map(Node::id).toList());
            detail.put("isr", p.isr().stream().map(Node::id).toList());
            return detail;
        }).toList());
        result.put("configs", topicConfigs(name));
        return result;
    }

    public Map<String, Object> createTopic(TopicRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessRuleException("7001", HttpStatus.BAD_REQUEST, "El nombre del tópico es obligatorio");
        }
        int partitions = request.getPartitions() == null ? 3 : request.getPartitions();
        short replication = request.getReplicationFactor() == null ? 3 : request.getReplicationFactor();
        if (partitions < 1 || replication < 1) {
            throw new BusinessRuleException("7002", HttpStatus.BAD_REQUEST,
                    "Particiones y factor de replicación deben ser al menos 1");
        }

        NewTopic topic = new NewTopic(request.getName(), partitions, replication);
        if (request.getConfigs() != null && !request.getConfigs().isEmpty()) {
            topic.configs(request.getConfigs());
        }
        await(admin.createTopics(List.of(topic)).all());
        return describeTopic(request.getName());
    }

    /** Modifica configuraciones (retención, cleanup.policy, etc.) sin tocar el resto de las existentes. */
    public Map<String, Object> updateTopicConfig(String name, TopicConfigRequest request) {
        if (request.getConfigs() == null || request.getConfigs().isEmpty()) {
            throw new BusinessRuleException("7003", HttpStatus.BAD_REQUEST, "Debes indicar al menos una configuración");
        }
        describeTopic(name); // falla con 404 si no existe

        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        List<AlterConfigOp> ops = request.getConfigs().entrySet().stream()
                .map(e -> new AlterConfigOp(new ConfigEntry(e.getKey(), e.getValue()), AlterConfigOp.OpType.SET))
                .toList();
        await(admin.incrementalAlterConfigs(Map.of(resource, ops)).all());
        return describeTopic(name);
    }

    /** Aumenta las particiones del tópico (Kafka no permite reducirlas). */
    public Map<String, Object> increasePartitions(String name, PartitionsRequest request) {
        if (request.getPartitions() == null || request.getPartitions() < 1) {
            throw new BusinessRuleException("7004", HttpStatus.BAD_REQUEST, "Indica la cantidad total de particiones");
        }
        await(admin.createPartitions(Map.of(name, NewPartitions.increaseTo(request.getPartitions()))).all());
        return describeTopic(name);
    }

    public void deleteTopic(String name) {
        await(admin.deleteTopics(List.of(name)).all());
    }

    private Map<String, String> topicConfigs(String name) {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);
        Config config = await(admin.describeConfigs(List.of(resource)).values().get(resource));
        // Solo las configuraciones definidas explícitamente para el tópico (no los valores por defecto del broker)
        return config.entries().stream()
                .filter(e -> e.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG)
                .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value, (a, b) -> a, TreeMap::new));
    }

    // ------------------------------------------------------------------ consumer groups

    public List<Map<String, Object>> listConsumerGroups() {
        return await(admin.listConsumerGroups().all()).stream()
                .sorted(Comparator.comparing(ConsumerGroupListing::groupId))
                .map(g -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("groupId", g.groupId());
                    m.put("state", g.groupState().map(Enum::name).orElse("UNKNOWN"));
                    return m;
                }).toList();
    }

    /** Detalle de un grupo: miembros, particiones asignadas y lag (mensajes pendientes) por partición. */
    public Map<String, Object> describeConsumerGroup(String groupId) {
        ConsumerGroupDescription group = await(
                admin.describeConsumerGroups(List.of(groupId)).describedGroups().get(groupId));
        if (group.groupState() == GroupState.DEAD) {
            throw new BusinessRuleException("7005", HttpStatus.NOT_FOUND, "Consumer group no encontrado: " + groupId);
        }

        Map<TopicPartition, OffsetAndMetadata> committed =
                await(admin.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata());
        Map<TopicPartition, OffsetSpec> latestSpec = committed.keySet().stream()
                .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));
        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latest =
                latestSpec.isEmpty() ? Map.of() : await(admin.listOffsets(latestSpec).all());

        List<Map<String, Object>> offsets = committed.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<TopicPartition, OffsetAndMetadata> e) -> e.getKey().topic())
                        .thenComparingInt(e -> e.getKey().partition()))
                .map(e -> {
                    long end = latest.containsKey(e.getKey()) ? latest.get(e.getKey()).offset() : e.getValue().offset();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("topic", e.getKey().topic());
                    m.put("partition", e.getKey().partition());
                    m.put("committedOffset", e.getValue().offset());
                    m.put("endOffset", end);
                    m.put("lag", Math.max(0, end - e.getValue().offset()));
                    return m;
                }).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", group.groupId());
        result.put("state", group.groupState().name());
        result.put("partitionAssignor", group.partitionAssignor());
        result.put("members", group.members().stream().map(member -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("memberId", member.consumerId());
            m.put("clientId", member.clientId());
            m.put("host", member.host());
            m.put("assignedPartitions", member.assignment().topicPartitions().stream()
                    .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                    .map(tp -> tp.topic() + "-" + tp.partition()).toList());
            return m;
        }).toList());
        result.put("offsets", offsets);
        return result;
    }

    public void deleteConsumerGroup(String groupId) {
        await(admin.deleteConsumerGroups(List.of(groupId)).all());
    }

    // ------------------------------------------------------------------ utilidades

    /** Espera el resultado de Kafka y traduce sus errores a respuestas HTTP con sentido. */
    private <T> T await(KafkaFuture<T> future) {
        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessRuleException("7099", HttpStatus.SERVICE_UNAVAILABLE, "Operación interrumpida");
        } catch (TimeoutException e) {
            throw new BusinessRuleException("7098", HttpStatus.SERVICE_UNAVAILABLE,
                    "Kafka no respondió en " + TIMEOUT_SECONDS + " segundos");
        } catch (ExecutionException e) {
            throw translate(e.getCause());
        }
    }

    private BusinessRuleException translate(Throwable cause) {
        if (cause instanceof TopicExistsException) {
            return new BusinessRuleException("7010", HttpStatus.CONFLICT, "El tópico ya existe");
        }
        if (cause instanceof UnknownTopicOrPartitionException) {
            return new BusinessRuleException("7011", HttpStatus.NOT_FOUND, "El tópico no existe");
        }
        if (cause instanceof GroupIdNotFoundException) {
            return new BusinessRuleException("7012", HttpStatus.NOT_FOUND, "El consumer group no existe");
        }
        if (cause instanceof GroupNotEmptyException) {
            return new BusinessRuleException("7013", HttpStatus.CONFLICT,
                    "El consumer group tiene consumidores activos, no se puede eliminar");
        }
        if (cause instanceof InvalidReplicationFactorException || cause instanceof InvalidPartitionsException
                || cause instanceof InvalidConfigurationException || cause instanceof InvalidRequestException
                || cause instanceof InvalidTopicException) {
            return new BusinessRuleException("7014", HttpStatus.BAD_REQUEST, cause.getMessage());
        }
        return new BusinessRuleException("7090", HttpStatus.BAD_GATEWAY,
                "Error comunicándose con Kafka: " + cause.getMessage());
    }
}
