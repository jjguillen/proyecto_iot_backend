package com.jaroso.proyectiot.dtos;

import com.jaroso.proyectiot.entities.EstadoSensor;

public record AutomationActuatorActionDto(Long actuadorId, EstadoSensor state) {
}

