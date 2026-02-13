package com.jaroso.proyectiot.dtos;

import com.jaroso.proyectiot.entities.EstadoSensor;
import com.jaroso.proyectiot.entities.TipoSensor;

public record SensorDto(Long id, String nombre, TipoSensor tipo, EstadoSensor estado) {
}
