package com.jaroso.proyectiot.services;

import com.jaroso.proyectiot.dtos.AutomationActuatorActionDto;
import com.jaroso.proyectiot.dtos.AutomationDecisionResponseDto;
import com.jaroso.proyectiot.entities.EstadoSensor;
import com.jaroso.proyectiot.entities.Sensor;
import com.jaroso.proyectiot.repositories.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ActuatorAutomationService {

    // Ids de bomba principal y EV principales. Son fijos y corresponden a los actuadores definidos en BBDD.
    private static final Long MAIN_PUMP_ID = 1L;
    private static final List<Long> MAIN_VALVE_IDS = List.of(2L, 3L, 4L);

    // Mapping fijo de actuadores por sector (ids de BBDD).
    private static final List<SectorActuatorGroup> SECTOR_GROUPS = List.of(
            // Sector1 con id 2
            new SectorActuatorGroup(9L, List.of(10L, 11L)),
            // Sector2 con id 3
            new SectorActuatorGroup(16L, List.of(17L, 18L)),
            // Sector3 con id 4
            new SectorActuatorGroup(23L, List.of(24L, 25L))
    );

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private MqttPublisher mqttPublisher;

    @Transactional
    public AutomationDecisionResponseDto decideAndApply(Long actuatorId, EstadoSensor targetState) {
        if (actuatorId == null || targetState == null) {
            return denied("Solicitud incompleta: actuadorId y targetState son obligatorios");
        }

        if (!isSupportedActuatorState(targetState)) {
            return denied("Estado no soportado para actuadores. Usa ARRANCADO o PARADO");
        }

        Optional<Sensor> requestedOpt = sensorRepository.findById(actuatorId);
        if (requestedOpt.isEmpty()) {
            return denied("Actuador no encontrado: " + actuatorId);
        }

        Sensor requested = requestedOpt.get();
        if (!Boolean.TRUE.equals(requested.getIsActuador())) {
            return denied("El sensor indicado no es un actuador: " + actuatorId);
        }

        Map<Long, EstadoSensor> plannedActions = new LinkedHashMap<>();
        plannedActions.put(actuatorId, targetState);

        // Regla 1: no abrir bomba principal si todas las EV principales están cerradas.
        if (actuatorId.equals(MAIN_PUMP_ID) && targetState == EstadoSensor.ARRANCADO &&
                allMainValvesClosed(plannedActions)) {
            return denied("No se puede abrir la bomba principal con todas las EV principales cerradas");
        }

        // Regla 2: abrir alguna EV principal -> abrir también bomba principal.
        if (MAIN_VALVE_IDS.contains(actuatorId) && targetState == EstadoSensor.ARRANCADO) {
            plannedActions.put(MAIN_PUMP_ID, EstadoSensor.ARRANCADO);
        }

        // Regla adicional: si se cierra una EV principal y ya no queda ninguna abierta,
        // se cierra la bomba principal.
        if (MAIN_VALVE_IDS.contains(actuatorId)
                && targetState == EstadoSensor.PARADO
                && allMainValvesClosed(plannedActions)) {
            plannedActions.put(MAIN_PUMP_ID, EstadoSensor.PARADO);
        }

        // Regla adicional 2: si cierro la bomba principal y hay alguna EV abierta,
        // cierro todas las que estén abiertas, manda la bomba principal
        if (actuatorId.equals(MAIN_PUMP_ID) && targetState == EstadoSensor.PARADO) {
            for(Long valveId : MAIN_VALVE_IDS) {
                if (resolveEffectiveState(valveId, plannedActions) == EstadoSensor.ARRANCADO) {
                    plannedActions.put(valveId, EstadoSensor.PARADO);
                }
            }
        }

        // Reglas dentro de los Sectores
        Optional<SectorActuatorGroup> sectorGroupOpt = findSectorGroupForActuator(actuatorId);
        if (sectorGroupOpt.isPresent()) {
            SectorActuatorGroup group = sectorGroupOpt.get();

            // Regla 3: abrir bomba de sector requiere al menos una EV de su sector abierta.
            if (group.pumpId().equals(actuatorId)
                    && targetState == EstadoSensor.ARRANCADO
                    && allSectorValvesClosed(group, plannedActions)) {
                return denied("No se puede abrir la bomba del sector con ambas EV de sector cerradas");
            }

            // Regla 4: abrir EV de sector -> abrir bomba del sector.
            if (group.valveIds().contains(actuatorId) && targetState == EstadoSensor.ARRANCADO) {
                plannedActions.put(group.pumpId(), EstadoSensor.ARRANCADO);
            }

            // Regla 5: cerrar EV de sector -> cerrar bomba del sector.
            if (group.valveIds().contains(actuatorId) && targetState == EstadoSensor.PARADO) {
                plannedActions.put(group.pumpId(), EstadoSensor.PARADO);
            }

            // Regla 6: parar bomba de sector -> cerrar EV del sector que estén abiertas.
            if (group.pumpId().equals(actuatorId) && targetState == EstadoSensor.PARADO) {
                for (Long valveId : group.valveIds()) {
                    if (resolveEffectiveState(valveId, plannedActions) == EstadoSensor.ARRANCADO) {
                        plannedActions.put(valveId, EstadoSensor.PARADO);
                    }
                }
            }
        }

        List<AutomationActuatorActionDto> appliedActions = applyActions(plannedActions);
        return new AutomationDecisionResponseDto(true, null, appliedActions);
    }

    private boolean allMainValvesClosed(Map<Long, EstadoSensor> plannedActions) {
        for (Long valveId : MAIN_VALVE_IDS) {
            EstadoSensor effectiveState = resolveEffectiveState(valveId, plannedActions);
            if (effectiveState == EstadoSensor.ARRANCADO) {
                return false;
            }
        }
        return true;
    }

    private boolean allSectorValvesClosed(SectorActuatorGroup group, Map<Long, EstadoSensor> plannedActions) {
        for (Long valveId : group.valveIds()) {
            EstadoSensor effectiveState = resolveEffectiveState(valveId, plannedActions);
            if (effectiveState == EstadoSensor.ARRANCADO) {
                return false;
            }
        }
        return true;
    }

    private EstadoSensor resolveEffectiveState(Long actuatorId, Map<Long, EstadoSensor> plannedActions) {
        if (plannedActions.containsKey(actuatorId)) {
            return plannedActions.get(actuatorId);
        }

        return sensorRepository.findById(actuatorId)
                .map(Sensor::getEstado)
                .orElse(EstadoSensor.PARADO);
    }

    private Optional<SectorActuatorGroup> findSectorGroupForActuator(Long actuatorId) {
        return SECTOR_GROUPS.stream()
                .filter(group -> group.pumpId().equals(actuatorId) || group.valveIds().contains(actuatorId))
                .findFirst();
    }

    private List<AutomationActuatorActionDto> applyActions(Map<Long, EstadoSensor> plannedActions) {
        List<AutomationActuatorActionDto> applied = new ArrayList<>();

        for (Map.Entry<Long, EstadoSensor> entry : plannedActions.entrySet()) {
            Long id = entry.getKey();
            EstadoSensor state = entry.getValue();

            Optional<Sensor> sensorOpt = sensorRepository.findById(id);
            if (sensorOpt.isEmpty()) {
                continue;
            }

            Sensor sensor = sensorOpt.get();
            sensor.setEstado(state);
            sensorRepository.save(sensor);

            // Los comandos de actuador siempre salen por topicMQTTAct (nunca por topicMQTT).
            if (sensor.getTopicMQTTAct() != null && !sensor.getTopicMQTTAct().isBlank()) {
                mqttPublisher.publish(sensor.getTopicMQTTAct(), toActuatorCommand(state));
            }

            applied.add(new AutomationActuatorActionDto(id, state));
        }

        return applied;
    }

    private String toActuatorCommand(EstadoSensor state) {
        return state == EstadoSensor.ARRANCADO ? "ON" : "OFF";
    }

    private AutomationDecisionResponseDto denied(String reason) {
        return new AutomationDecisionResponseDto(false, reason, List.of());
    }

    private boolean isSupportedActuatorState(EstadoSensor state) {
        return state == EstadoSensor.ARRANCADO || state == EstadoSensor.PARADO;
    }

    private record SectorActuatorGroup(Long pumpId, List<Long> valveIds) {
    }
}
