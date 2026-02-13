package com.jaroso.proyectiot.dtos;

import java.time.LocalDateTime;

public record LecturaCreateDto(Long sensorId, Double valor, String unidad) {
}
