package com.jaroso.proyectiot.dtos;

import com.jaroso.proyectiot.entities.EstadoSensor;
import com.jaroso.proyectiot.entities.TipoSensor;

public record SensorCreateDto(String nombre, String descripcion,
                              String ubicacion, TipoSensor tipo, EstadoSensor estado) {
}
