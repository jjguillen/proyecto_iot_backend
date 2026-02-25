package com.jaroso.proyectiot.services;

import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.jaroso.proyectiot.controllers.LecturaController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class MqttPublisher {

    private final Mqtt3BlockingClient client;

    Logger logger = Logger.getLogger(LecturaController.class.getName());

    public MqttPublisher(@Value("${mqtt.host:localhost}") String host,
                         @Value("${mqtt.port:1883}") int port) {
        client = Mqtt3Client.builder()
                .identifier("springClient")
                .serverHost(host)
                .serverPort(port)
                .buildBlocking();
        client.connect();
    }

    public void publish(String topic, String payload) {
        logger.info("Publicando en " + topic + ": " + payload);
        client.publishWith()
                .topic(topic)
                .payload(payload.getBytes())
                .send();
    }
}
