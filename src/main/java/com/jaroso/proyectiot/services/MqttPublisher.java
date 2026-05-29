package com.jaroso.proyectiot.services;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.jaroso.proyectiot.entities.TipoSensor;
import com.jaroso.proyectiot.repositories.SensorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class MqttPublisher {

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private SensorIngestionService sensorIngestionService;

    private final Mqtt3AsyncClient client;
    private final String host;
    private final int port;

    private final Logger logger = Logger.getLogger(MqttPublisher.class.getName());

    /** Tipos de sensor que el publisher gestiona vía MQTT directo */
    private static final Set<TipoSensor> TIPOS_MQTT = Set.of(
            TipoSensor.HUMEDAD,
            TipoSensor.CAUDAL,
            TipoSensor.PRESION,
            TipoSensor.NIVEL,
            TipoSensor.ELECTROVALVULA,
            TipoSensor.BOMBA
    );

    public MqttPublisher(@Value("${mqtt.host:localhost}") String host,
                         @Value("${mqtt.port:1883}") int port) {
        this.host = host;
        this.port = port;
        client = Mqtt3Client.builder()
                .identifier("springSubscriber-" + UUID.randomUUID())
                .serverHost(host)
                .serverPort(port)
                .buildAsync();
    }

    public void publish(String topic, String payload) {
        logger.info("Publicando en " + topic + ": " + payload);
        client.publishWith()
                .topic(topic)
                .payload(payload.getBytes())
                .send();
    }

    @PostConstruct
    public void conectarYSuscribir() {
        logger.info("Conectando al broker MQTT en " + host + ":" + port + "...");

        client.connect()
                .thenAccept(connAck -> {
                    logger.info("Conexión exitosa al broker MQTT");

                    sensorRepository.findAll().stream()
                            .filter(sensor -> TIPOS_MQTT.contains(sensor.getTipo()))
                            .forEach(sensor -> {
                                logger.info("Suscribiéndose a " + sensor.getTopicMQTT()
                                        + " [" + sensor.getTipo() + "]");
                                client.subscribeWith()
                                        .topicFilter(sensor.getTopicMQTT())
                                        .callback(msg -> sensorIngestionService.ingest(
                                                sensor.getId(),
                                                sensor.getTipo(),
                                                new String(msg.getPayloadAsBytes()).trim()
                                        ))
                                        .send();
                            });
                })
                .exceptionally(throwable -> {
                    logger.severe("Error conectando al broker MQTT: " + throwable.getMessage());
                    return null;
                });
    }
}
