package com.jaroso.proyectiot.mappers;


import com.jaroso.proyectiot.dtos.LecturaCreateDto;
import com.jaroso.proyectiot.dtos.LecturaDto;
import com.jaroso.proyectiot.entities.Lectura;
import com.jaroso.proyectiot.entities.Sensor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LecturaMapper {
    @Mapping(target = "sensorId", source = "sensor.id")
    LecturaDto toDto(Lectura lectura);

    Lectura toEntity(LecturaCreateDto lecturaCreateDto);
}
