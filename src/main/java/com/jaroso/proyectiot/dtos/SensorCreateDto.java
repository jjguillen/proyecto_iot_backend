package com.jaroso.proyectiot.dtos;

import com.jaroso.proyectiot.entities.EstadoSensor;
import com.jaroso.proyectiot.entities.TipoSensor;

public record SensorCreateDto(Long sectorId, String nombre, String descripcion,
                              String ubicacion, String topicMQTT, Boolean isActuador,
                              TipoSensor tipo, EstadoSensor estado) {
}
