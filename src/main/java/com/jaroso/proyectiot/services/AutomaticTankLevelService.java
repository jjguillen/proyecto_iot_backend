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

@Service
public class AutomaticTankLevelService {
    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    @Lazy
    private ActuatorAutomationService actuatorAutomationService;

    Logger logger = Logger.getLogger(ActuatorAutomationService.class.getName());

    public void evaluateLevel(Long levelSensorId, Double currentValue) {
        //logger.info("Evaluando nivel para sensor " + levelSensorId + " con valor actual: " + currentValue);

        Sensor sensorNivel = sensorRepository.findById(levelSensorId).orElse(null);
        if (sensorNivel == null) return;

        if (sensorNivel.getTipo() != TipoSensor.NIVEL) return;

        Integer min = sensorNivel.getValorMin();
        Integer max = sensorNivel.getValorMax();

        Long valveId = mapLevelSensorToFillValve(levelSensorId);
        if (valveId == null) return;

        //logger.info("La EV del sensor de nivel " + levelSensorId + " es: " + valveId);

        Sensor valve = sensorRepository.findById(valveId).orElse(null);
        if (valve == null) return;

        boolean abierta = valve.getEstado() == EstadoSensor.ARRANCADO;

        if (min != null && currentValue < min && !abierta) {
            actuatorAutomationService.decideAndApply(valveId, EstadoSensor.ARRANCADO);
            return;
        }

        if (max != null && currentValue > max && abierta) {
            actuatorAutomationService.decideAndApply(valveId, EstadoSensor.PARADO);
        }
    }

    /**
     * Devuelve el sensor de la EV correspondiente al sensor de nivel pasado
     * @param levelSensorId
     * @return
     */
    public Long mapLevelSensorToFillValve(Long levelSensorId) {
        //Mapeo sensores nivel con ev correspondiente
        Map<Long, Long> mapping = Map.of(
                6L, 2L, // Sensor nivel 1 -> EV 1
                7L, 3L, // Sensor nivel 2 -> EV 2
                8L, 4L // Sensor nivel 3 -> EV 3
        );
        return mapping.get(levelSensorId);
    }
}
