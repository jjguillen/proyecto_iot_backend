package com.jaroso.proyectiot.services;

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;
import com.jaroso.proyectiot.entities.Lectura;
import com.jaroso.proyectiot.entities.Sensor;
import com.jaroso.proyectiot.entities.TipoSensor;
import com.jaroso.proyectiot.repositories.LecturaRepository;
import com.jaroso.proyectiot.repositories.SensorRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class MqttPublisher {

    @Autowired
    private LecturaRepository lecturaRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private AutomaticTankLevelService automaticTankLevelService;

    @Autowired
    private ObjectMapper objectMapper;

    private final Mqtt3AsyncClient client;
    private final String host;
    private final int port;

    private final Logger logger = Logger.getLogger(MqttPublisher.class.getName());

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

                    //SUSCRIBIRNOS A TODOS LOS SENSORES QUE TENEMOS

                    //Humedad
                    sensorRepository.findAll().stream()
                                    .filter(sensor -> sensor.getTipo().equals(TipoSensor.HUMEDAD))
                            .forEach(sensor -> {
                                logger.info("Suscribiéndose a " + sensor.getTopicMQTT());
                                client.subscribeWith()
                                        .topicFilter(sensor.getTopicMQTT())
                                        .callback(msg -> procesarHumedad(msg, sensor.getId()))
                                        .send();
                            });

                    //Caudal
                    sensorRepository.findAll().stream()
                            .filter(sensor -> sensor.getTipo().equals(TipoSensor.CAUDAL))
                            .forEach(sensor -> {
                                logger.info("Suscribiéndose a " + sensor.getTopicMQTT());
                                client.subscribeWith()
                                        .topicFilter(sensor.getTopicMQTT())
                                        .callback(msg -> procesarCaudal(msg, sensor.getId()))
                                        .send();
                            });

                    //Presión
                    sensorRepository.findAll().stream()
                            .filter(sensor -> sensor.getTipo().equals(TipoSensor.PRESION))
                            .forEach(sensor -> {
                                logger.info("Suscribiéndose a " + sensor.getTopicMQTT());
                                client.subscribeWith()
                                        .topicFilter(sensor.getTopicMQTT())
                                        .callback(msg -> procesarPresion(msg, sensor.getId()))
                                        .send();
                            });

                    //Nivel
                    sensorRepository.findAll().stream()
                            .filter(sensor -> sensor.getTipo().equals(TipoSensor.NIVEL))
                            .forEach(sensor -> {
                                logger.info("Suscribiéndose a " + sensor.getTopicMQTT());
                                client.subscribeWith()
                                        .topicFilter(sensor.getTopicMQTT())
                                        .callback(msg -> procesarNivel(msg, sensor.getId()))
                                        .send();
                            });


                    //Actuadores: BOMBA Y ELECTROVALVULA
                    sensorRepository.findAll().stream()
                            .filter(sensor -> sensor.getTipo().equals(TipoSensor.ELECTROVALVULA)
                                    ||  sensor.getTipo().equals(TipoSensor.BOMBA))
                            .forEach(sensor -> {
                                logger.info("Suscribiéndose a " + sensor.getTopicMQTT());
                                client.subscribeWith()
                                        .topicFilter(sensor.getTopicMQTT())
                                        .callback(msg -> procesarActuador(msg, sensor.getId()))
                                        .send();
                            });




                })
                .exceptionally(throwable -> {
                    logger.severe("Error conectando al broker MQTT: " + throwable.getMessage());
                    //throwable.printStackTrace();
                    return null;
                });
    }

    /**
     * Convierte el valor recibido mqtt a un caudal de litros por minuto
     * @param msg
     * @param sensorId
     */
    private void procesarCaudal(Mqtt3Publish msg, long sensorId) {
        logger.info("Recibiendo mensaje presion/nivel de: " + msg.getTopic());
        String payload = new String(msg.getPayloadAsBytes(), StandardCharsets.UTF_8).trim();
        int valor = Integer.parseInt(payload);

        //Corrección sobre el valor enviado, ya que al enviar se le suma 100
        int caudalRawInt = valor - 100;

        double caudalLMin = (caudalRawInt / 75.0); //ecuación de conversión

        if ((caudalLMin > 10) || (caudalLMin < 0)) {
            logger.info("No se guarda el valor de caudal -> fuera de rango: " + caudalLMin);
        } else {
            saveLectura(caudalLMin, sensorId);
        }
    }

    /**
     * Convierte el dato mqtt primero a litros de agua que hay y luego guarda un porcentaje
     * y llama a la automatización de activar o para EVs para llenar o vaciar la balsa si corresponde
     * @param msg
     * @param sensorId
     */
    private void procesarNivel(Mqtt3Publish msg, long sensorId) {
        logger.info("Recibiendo mensaje nivel de: " + msg.getTopic());
        String payload = new String(msg.getPayloadAsBytes(), StandardCharsets.UTF_8).trim();
        double valor = Double.parseDouble(payload);

        var areaDm2 = 1.45 * 1.45; //(el área de la balsa)
        var capacidadLitros = 8.0;
        var volumenL = areaDm2 * (valor / 10);

        var litros = capacidadLitros - volumenL; //lo que mide es la parte vacía, por lo que hay que restar al máximo (8dm) el volumen convertido a dm
        double porcentaje = (litros / capacidadLitros) * 100.0;
        porcentaje = Math.clamp(porcentaje, 0.0, 100.0);

        //MÁS FÁCIL SI SABEMOS ALTURA DE LA BALSA, LO QUE GUARDAMOS ES UN PORCENTAJE
        //porcentaje = ((alturaTotalCm - alturaVaciaCmSensor) / alturaTotalCm) * 100

        saveLectura(porcentaje, sensorId);
        automaticTankLevelService.evaluateLevel(sensorId, porcentaje);
    }

    /**
     * Convierte el dato de mqtt en bruto a un valor de presión en Kgf/cm2
     * @param msg
     * @param sensorId
     */
    private void procesarPresion(Mqtt3Publish msg, long sensorId) {
        logger.info("Recibiendo mensaje presion de: " + msg.getTopic());
        String payload = new String(msg.getPayloadAsBytes(), StandardCharsets.UTF_8).trim();

        double valorRaw = Double.parseDouble(payload) - 450;

        if (valorRaw < 0.0)
            valorRaw = 0.0;

        double convCadMv = 1;       //de cad a mV
        double convMvBar = 0.003;   //de mV a kgf/cm2

        double presionKgfCm2 = valorRaw * convCadMv * convMvBar;

        if ( presionKgfCm2 > 0.4) {
            logger.info("No se guarda el valor de presión -> fuera de rango: " + presionKgfCm2);
        } else {
            saveLectura(presionKgfCm2, sensorId);
        }
    }

    /**
     * Convierte el dato de mqtt a un dato de humedad relativa (0-100)
     * @param msg
     * @param sensorId
     */
    private void procesarHumedad(Mqtt3Publish msg, long sensorId) {
        logger.info("Recibiendo mensaje humedad de: " + msg.getTopic());
        String payload = new String(msg.getPayloadAsBytes(), StandardCharsets.UTF_8).trim();
        var valor = Integer.parseInt(payload);

        var cadMin = 330;   //valor en seco
        var cadMax = 715;   //valor totalmente húmedo

        //Queremos dar un valor en el rango 0-100
        var humedadRH = 100 - ((100.0 / (cadMax - cadMin)) * (valor - cadMin));

        if ((humedadRH > 100) || (humedadRH < 0)) {
            logger.info("No se guarda el valor de humedad -> fuera de rango: " + humedadRH);
        } else {
            saveLectura(humedadRH, sensorId);
        }
    }

    private void procesarActuador(Mqtt3Publish msg, long sensorId) {
        logger.info("Recibiendo mensaje actuador de: " + msg.getTopic());
        String payload = new String(msg.getPayloadAsBytes(), StandardCharsets.UTF_8).trim();
        String normalizado = payload.toLowerCase();

        double valor = switch (normalizado) {
            case "on", "true", "1" -> 1.0;
            case "off", "false", "0" -> 0.0;
            default -> throw new IllegalArgumentException("Payload de actuador no soportado: " + payload);
        };

        //Guardar la lectura en BBDD
        saveLectura(valor, sensorId);
    }

    /**
     * Guarda una lectura en la BBDD
     * @param valor
     * @param sensorId
     */
    private void saveLectura(Double valor, long sensorId) {
        Lectura lectura = new Lectura();
        lectura.setValor(valor);
        Optional<Sensor> sensor = sensorRepository.findById(sensorId);
        if (sensor.isEmpty()) {
            logger.info("Sensor incorrecto, no se puede grabar lectura: " + sensorId);
        } else {
            //logger.info("Grabando lectura de: " + sensorId);
            lectura.setSensor(sensor.get());
            lecturaRepository.save(lectura);
        }
    }

}
