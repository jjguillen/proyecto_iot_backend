package com.jaroso.proyectiot.dtos;

import com.jaroso.proyectiot.entities.EstadoSensor;
import com.jaroso.proyectiot.entities.TipoSensor;

public record SectorCreateDto(String nombre, String cultivo, String parcela, Double superficie, Double latitud, Double longitud) {
}
