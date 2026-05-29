package com.jaroso.proyectiot.dtos;

import com.jaroso.proyectiot.entities.OrigenLectura;

import java.time.LocalDateTime;

public record LecturaDto(Long id, Double valor, String unidad, LocalDateTime fechaHora, Long sensorId, OrigenLectura origen) {
}
