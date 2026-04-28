package com.jaroso.proyectiot.dtos;

import com.jaroso.proyectiot.entities.EstadoSensor;

public record AutomationActuatorRequestDto(Long actuadorId, EstadoSensor targetState) {
}

