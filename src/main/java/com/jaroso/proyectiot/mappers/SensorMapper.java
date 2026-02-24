package com.jaroso.proyectiot.mappers;

import com.jaroso.proyectiot.dtos.SensorCreateDto;
import com.jaroso.proyectiot.dtos.SensorDto;
import com.jaroso.proyectiot.entities.Sensor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SensorMapper {
    SensorDto toDto(Sensor sensor);

    @Mapping(target = "id", ignore = true)
    Sensor toEntity(SensorCreateDto sensorDto);
}
