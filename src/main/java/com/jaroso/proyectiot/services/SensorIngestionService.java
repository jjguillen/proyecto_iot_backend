package com.jaroso.proyectiot.services;

import com.jaroso.proyectiot.entities.Lectura;
import com.jaroso.proyectiot.entities.OrigenLectura;
import com.jaroso.proyectiot.entities.Sensor;
import com.jaroso.proyectiot.entities.TipoSensor;
import com.jaroso.proyectiot.repositories.LecturaRepository;
import com.jaroso.proyectiot.repositories.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Servicio centralizado de ingesta de lecturas de sensores.
 * Recibe el payload raw (String) + el sensor y aplica la conversión
 * correcta según el tipo. También dispara las automatizaciones.
 * Puede ser llamado desde MqttPublisher y desde LoraMqttSubscriberService.
 */
@Service
public class SensorIngestionService {

    private final Logger logger = Logger.getLogger(SensorIngestionService.class.getName());

    @Autowired
    private LecturaRepository lecturaRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private AutomaticTankLevelService automaticTankLevelService;

    @Autowired
    private AutomaticHumiditySectorService automaticHumiditySectorService;

    // -------------------------------------------------------------------------
    // Punto de entrada principal
    // -------------------------------------------------------------------------

    /**
     * Procesa el payload raw de un sensor según su tipo y almacena la lectura.
     *
     * @param sensorId  id del sensor en BD
     * @param tipo      tipo del sensor (para seleccionar la conversión)
     * @param rawValue  payload raw recibido (String)
     * @param origen    canal por el que llegó el dato (MQTT o LORA)
     */
    public void ingest(long sensorId, TipoSensor tipo, String rawValue, OrigenLectura origen) {
        switch (tipo) {
            case HUMEDAD      -> procesarHumedad(sensorId, rawValue, origen);
            case NIVEL        -> procesarNivel(sensorId, rawValue, origen);
            case PRESION      -> procesarPresion(sensorId, rawValue, origen);
            case CAUDAL       -> procesarCaudal(sensorId, rawValue, origen);
            case BOMBA, ELECTROVALVULA -> procesarActuador(sensorId, rawValue, origen);
            default           -> logger.warning("Tipo de sensor sin procesador registrado: " + tipo);
        }
    }

    /**
     * Sobrecarga de conveniencia para MQTT (origen por defecto).
     */
    public void ingest(long sensorId, TipoSensor tipo, String rawValue) {
        ingest(sensorId, tipo, rawValue, OrigenLectura.MQTT);
    }

    // -------------------------------------------------------------------------
    // Procesadores por tipo
    // -------------------------------------------------------------------------

    /**
     * Convierte el dato mqtt a humedad relativa (0-100 %).
     */
    private void procesarHumedad(long sensorId, String payload, OrigenLectura origen) {
        logger.info("Procesando humedad sensorId=" + sensorId + " payload=" + payload + " origen=" + origen);

        int valor;
        try {
            valor = Integer.parseInt(payload);
        } catch (Exception ex) {
            logger.warning("Error en lectura humedad [" + payload + "]: " + ex.getMessage());
            return;
        }

        var cadMin = 330;   // valor en seco
        var cadMax = 715;   // valor totalmente húmedo

        var humedadRH = 100 - ((100.0 / (cadMax - cadMin)) * (valor - cadMin));

        if ((humedadRH > 100) || (humedadRH < 0)) {
            logger.warning("Valor de humedad fuera de rango, no se guarda: " + humedadRH);
        } else {
            saveLectura(humedadRH, sensorId, origen);
            automaticHumiditySectorService.evaluateHumidity(sensorId, humedadRH);
        }
    }

    /**
     * Convierte el dato mqtt de nivel a porcentaje de llenado (0-100 %).
     */
    private void procesarNivel(long sensorId, String payload, OrigenLectura origen) {
        logger.info("Procesando nivel sensorId=" + sensorId + " payload=" + payload + " origen=" + origen);

        int valor;
        try {
            valor = Integer.parseInt(payload) - 100;
        } catch (Exception ex) {
            logger.warning("Error en lectura nivel [" + payload + "]: " + ex.getMessage());
            return;
        }

        var areaDm2 = 1.45 * 1.45;
        var capacidadLitros = 8.0;
        var volumenL = areaDm2 * (valor / 10.0);

        var litros = capacidadLitros - volumenL;
        double porcentaje = (litros / capacidadLitros) * 100.0;
        porcentaje = Math.clamp(porcentaje, 0.0, 100.0);

        logger.info("Nivel calculado: valor=" + valor + " litros=" + litros + " porcentaje=" + porcentaje);

        if (litros < 0 || litros > 8) {
            logger.warning("Litros fuera de rango, no se guarda: " + litros);
            return;
        }

        saveLectura(porcentaje, sensorId, origen);
        automaticTankLevelService.evaluateLevel(sensorId, porcentaje);
    }

    /**
     * Convierte el dato mqtt bruto a presión en Kgf/cm².
     */
    private void procesarPresion(long sensorId, String payload, OrigenLectura origen) {
        logger.info("Procesando presión sensorId=" + sensorId + " payload=" + payload + " origen=" + origen);

        double valorRaw;
        try {
            valorRaw = Double.parseDouble(payload) - 450;
        } catch (Exception ex) {
            logger.warning("Error en lectura presión [" + payload + "]: " + ex.getMessage());
            return;
        }

        if (valorRaw <= 0.0) valorRaw = 0.0;

        double convCadMv = 1;
        double convMvBar = 0.003;
        double presionKgfCm2 = valorRaw * convCadMv * convMvBar;

        if (presionKgfCm2 > 0.4) {
            logger.warning("Valor de presión fuera de rango, no se guarda: " + presionKgfCm2);
        } else {
            saveLectura(presionKgfCm2, sensorId, origen);
        }
    }

    /**
     * Convierte el dato mqtt a caudal en litros/minuto.
     */
    private void procesarCaudal(long sensorId, String payload, OrigenLectura origen) {
        logger.info("Procesando caudal sensorId=" + sensorId + " payload=" + payload + " origen=" + origen);

        int valor;
        try {
            valor = Integer.parseInt(payload);
        } catch (Exception ex) {
            logger.warning("Error en lectura caudal [" + payload + "]: " + ex.getMessage());
            return;
        }

        int caudalRawInt = valor - 100;
        double caudalLMin = caudalRawInt <= 0 ? 0.0 : (caudalRawInt / 75.0);

        if (caudalLMin > 10 || caudalLMin < 0) {
            logger.warning("Valor de caudal fuera de rango, no se guarda: " + caudalLMin);
        } else {
            saveLectura(caudalLMin, sensorId, origen);
        }
    }

    /**
     * Convierte el estado ON/OFF de un actuador a 1.0/0.0.
     */
    private void procesarActuador(long sensorId, String payload, OrigenLectura origen) {
        logger.info("Procesando actuador sensorId=" + sensorId + " payload=" + payload + " origen=" + origen);

        String normalizado;
        try {
            normalizado = String.valueOf(payload.toLowerCase().trim().charAt(0));
        } catch (Exception ex) {
            logger.warning("Error en lectura actuador [" + payload + "]: " + ex.getMessage());
            return;
        }

        double valor = switch (normalizado) {
            case "o" -> payload.toLowerCase().trim().startsWith("on") ? 1.0 : 0.0; // "on" vs "off"
            case "t" -> 1.0;  // "true"
            case "f" -> 0.0;  // "false"
            case "1" -> 1.0;
            case "0" -> 0.0;
            default -> {
                logger.warning("Payload de actuador no reconocido: " + payload);
                yield -1.0;
            }
        };

        if (valor >= 0) {
            saveLectura(valor, sensorId, origen);
        }
    }

    // -------------------------------------------------------------------------
    // Persistencia
    // -------------------------------------------------------------------------

    private void saveLectura(Double valor, long sensorId, OrigenLectura origen) {
        Optional<Sensor> sensor = sensorRepository.findById(sensorId);
        if (sensor.isEmpty()) {
            logger.warning("Sensor no encontrado, no se guarda lectura: sensorId=" + sensorId);
            return;
        }
        Lectura lectura = new Lectura();
        lectura.setValor(valor);
        lectura.setSensor(sensor.get());
        lectura.setOrigen(origen);
        lecturaRepository.save(lectura);
    }
}

