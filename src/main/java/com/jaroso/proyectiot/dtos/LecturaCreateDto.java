package com.jaroso.proyectiot.dtos;

import com.jaroso.proyectiot.entities.OrigenLectura;

import java.time.LocalDateTime;

public record LecturaCreateDto(Long sensorId, Double valor, String unidad, OrigenLectura origen) {
}
