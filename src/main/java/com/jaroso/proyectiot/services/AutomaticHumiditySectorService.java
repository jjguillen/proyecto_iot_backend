package com.jaroso.proyectiot.services;

import com.jaroso.proyectiot.entities.EstadoSensor;
import com.jaroso.proyectiot.entities.Sensor;
import com.jaroso.proyectiot.entities.TipoSensor;
import com.jaroso.proyectiot.repositories.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Servicio de automatización de riego por humedad de suelo.
 * <p>
 * Lógica:
 *  - Si la humedad baja del umbral mínimo configurado en el sensor → se abre la EV principal
 *    del sector (el ActuatorAutomationService arrancará la bomba automáticamente).
 *  - Si la humedad supera el umbral máximo → se cierra la EV (el ActuatorAutomationService
 *    parará la bomba si no quedan otras EVs abiertas en el sector).
 * </p>
 */
@Service
public class AutomaticHumiditySectorService {

    // Mapeo: sensor de humedad → EV principal del sector a la que activa el riego.
    // Sector 1 (id=2): humedad 14, 15  → EV1_1 (id=10)
    // Sector 2 (id=3): humedad 21, 22  → EV2_1 (id=17)
    // Sector 3 (id=4): humedad 28, 29  → EV3_1 (id=24)
    private static final Map<Long, Long> HUMIDITY_SENSOR_TO_VALVE = Map.of(
            14L, 10L,
            15L, 10L,
            21L, 17L,
            22L, 17L,
            28L, 24L,
            29L, 24L
    );

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    @Lazy
    private ActuatorAutomationService actuatorAutomationService;

    @Autowired
    private ConfiguracionService configuracionService;

    private final Logger logger = Logger.getLogger(AutomaticHumiditySectorService.class.getName());

    /**
     * Evalúa si se debe arrancar o parar el riego del sector en función del valor de humedad recibido.
     *
     * @param humiditySensorId id del sensor de humedad que ha publicado la lectura
     * @param currentValue     valor de humedad en % (0-100)
     */
    public void evaluateHumidity(Long humiditySensorId, Double currentValue) {
        if (!configuracionService.isHumedadEnabled()) {
            return;
        }
        Sensor sensorHumedad = sensorRepository.findById(humiditySensorId).orElse(null);
        if (sensorHumedad == null) return;
        if (sensorHumedad.getTipo() != TipoSensor.HUMEDAD) return;

        Integer min = sensorHumedad.getValorMin();
        Integer max = sensorHumedad.getValorMax();

        Long valveId = HUMIDITY_SENSOR_TO_VALVE.get(humiditySensorId);
        if (valveId == null) {
            logger.warning("No hay EV mapeada para el sensor de humedad con id: " + humiditySensorId);
            return;
        }

        Sensor valve = sensorRepository.findById(valveId).orElse(null);
        if (valve == null) return;

        boolean evAbierta = valve.getEstado() == EstadoSensor.ARRANCADO;

        // Si la humedad baja del mínimo y el riego está parado → arrancar
        if (min != null && currentValue < min && !evAbierta) {
            logger.info("Humedad " + currentValue + "% < mínimo " + min + "% en sensor " +
                    humiditySensorId + " → arrancando riego (EV " + valveId + ")");
            actuatorAutomationService.decideAndApply(valveId, EstadoSensor.ARRANCADO);
            return;
        }

        // Si la humedad supera el máximo y el riego está en marcha → parar
        if (max != null && currentValue > max && evAbierta) {
            logger.info("Humedad " + currentValue + "% > máximo " + max + "% en sensor " +
                    humiditySensorId + " → parando riego (EV " + valveId + ")");
            actuatorAutomationService.decideAndApply(valveId, EstadoSensor.PARADO);
        }
    }
}

