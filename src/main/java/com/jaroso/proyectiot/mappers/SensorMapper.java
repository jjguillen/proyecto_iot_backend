package com.jaroso.proyectiot.mappers;

import com.jaroso.proyectiot.dtos.SensorCreateDto;
import com.jaroso.proyectiot.dtos.SensorDto;
import com.jaroso.proyectiot.entities.Sensor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SensorMapper {
    SensorDto toDto(Sensor sensor);
    Sensor toEntity(SensorCreateDto sensorDto);
}
