package com.jaroso.proyectiot.services;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.Mqtt3ClientBuilder;
import com.jaroso.proyectiot.entities.OrigenLectura;
import com.jaroso.proyectiot.entities.Sensor;
import com.jaroso.proyectiot.repositories.SensorRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class LoraMqttSubscriberService {

    private final Logger logger = Logger.getLogger(LoraMqttSubscriberService.class.getName());

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String topic;
    private final boolean tlsEnabled;
    private final String username;
    private final String password;
    private final ObjectMapper objectMapper;
    private final SensorRepository sensorRepository;
    private final SensorIngestionService sensorIngestionService;

    private Mqtt3AsyncClient client;

    public LoraMqttSubscriberService(
            @Value("${mqtt.lora.enabled:false}") boolean enabled,
            @Value("${mqtt.lora.host:10.0.0.12}") String host,
            @Value("${mqtt.lora.port:1883}") int port,
            @Value("${mqtt.lora.topic:lora/up}") String topic,
            @Value("${mqtt.lora.tls-enabled:false}") boolean tlsEnabled,
            @Value("${mqtt.lora.username:}") String username,
            @Value("${mqtt.lora.password:}") String password,
            ObjectMapper objectMapper,
            SensorRepository sensorRepository,
            SensorIngestionService sensorIngestionService
    ) {
        this.enabled = enabled;
        this.host = (host == null || host.isBlank()) ? "10.0.0.12" : host;
        this.port = port;
        this.topic = topic;
        this.tlsEnabled = tlsEnabled;
        this.username = username;
        this.password = password;
        this.objectMapper = objectMapper;
        this.sensorRepository = sensorRepository;
        this.sensorIngestionService = sensorIngestionService;
    }

    @PostConstruct
    public void subscribe() {
        if (!enabled) {
            logger.info("LoraMqttSubscriberService deshabilitado (mqtt.lora.enabled=false)");
            return;
        }

        if (topic == null || topic.isBlank()) {
            logger.warning("Topic LoRa vacío. Configura mqtt.lora.topic para poder suscribirte.");
            return;
        }

        if (host != null && host.contains("thethings.network") && (username == null || username.isBlank() || password == null || password.isBlank())) {
            logger.warning("Broker TTN detectado pero faltan credenciales. Configura mqtt.lora.username y mqtt.lora.password (API Key).");
        }

        if (tlsEnabled && port == 1883) {
            logger.warning("Configuración potencialmente inválida: tls-enabled=true con puerto 1883. Para TTN se recomienda 8883.");
        }

        final String brokerHost = Objects.requireNonNull(host);

        Mqtt3ClientBuilder builder = Mqtt3Client.builder()
                .identifier("spring-lora-subscriber-" + UUID.randomUUID())
                .serverHost(brokerHost)
                .serverPort(port);

        if (tlsEnabled) {
            builder = builder.sslWithDefaultConfig();
        }

        client = builder.buildAsync();

        logger.info("Conectando al broker MQTT LoRa en " + host + ":" + port + "...");

        connectClient()
                .thenCompose(connAck -> {
                    logger.info("Conexión MQTT LoRa exitosa. Suscribiendo a topic: " + topic);
                    return client.subscribeWith()
                            .topicFilter(topic)
                            .callback(this::onMessage)
                            .send();
                })
                .thenAccept(subAck -> logger.info("Suscripción LoRa activa en topic: " + topic))
                .exceptionally(throwable -> {
                    logger.severe("Error conectando/suscribiendo MQTT LoRa: " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
                    Throwable cause = throwable.getCause();
                    while (cause != null) {
                        logger.severe("Causa: " + cause.getClass().getSimpleName() + " - " + cause.getMessage());
                        cause = cause.getCause();
                    }
                    return null;
                });
    }

    private java.util.concurrent.CompletableFuture<?> connectClient() {
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            logger.info("Conectando con autenticación de usuario MQTT LoRa");
            return client.connectWith()
                    .simpleAuth()
                    .username(username)
                    .password(password.getBytes(StandardCharsets.UTF_8))
                    .applySimpleAuth()
                    .send();
        }

        logger.info("Conectando sin autenticación de usuario MQTT LoRa");
        return client.connect();
    }

    private void onMessage(com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish msg) {
        String rawPayload = new String(msg.getPayloadAsBytes(), StandardCharsets.UTF_8).trim();
        Optional<JsonNode> decodedOpt = extractDecodedPayload(rawPayload);

        if (decodedOpt.isEmpty()) {
            logger.warning("Mensaje LoRa sin decoded_payload | topic=" + msg.getTopic());
            return;
        }

        JsonNode decoded = decodedOpt.get();
        logger.info("LoRa | decoded_payload = " + decodedOpt.get().toPrettyString());

        for (String key : decoded.propertyNames()) {
            String value = decoded.get(key).asString();

            Optional<Sensor> sensorOpt = sensorRepository.findByNombre(key);
            if (sensorOpt.isEmpty()) {
                logger.warning("LoRa | key=" + key + " | sin sensor en BD, se ignora");
                continue;
            }

            Sensor sensor = sensorOpt.get();
            //logger.fine("LoRa | key=" + key + " | topicMQTT=" + sensor.getTopicMQTT() + " | value=" + value);

            sensorIngestionService.ingest(sensor.getId(), sensor.getTipo(), value, OrigenLectura.LORA);
        }
    }

    private Optional<JsonNode> extractDecodedPayload(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            JsonNode fromTtn = root.path("uplink_message").path("decoded_payload");
            if (!fromTtn.isMissingNode() && !fromTtn.isNull()) {
                return Optional.of(fromTtn);
            }

            JsonNode fromRoot = root.path("decoded_payload");
            if (!fromRoot.isMissingNode() && !fromRoot.isNull()) {
                return Optional.of(fromRoot);
            }

            return Optional.empty();
        } catch (Exception ex) {
            logger.warning("Payload LoRa no es JSON válido: " + ex.getMessage());
            return Optional.empty();
        }
    }

    @PreDestroy
    public void disconnect() {
        if (client != null) {
            client.disconnect()
                    .thenRun(() -> logger.info("Cliente MQTT LoRa desconectado"))
                    .exceptionally(throwable -> {
                        logger.warning("Error al desconectar MQTT LoRa: " + throwable.getMessage());
                        return null;
                    });
        }
    }
}

