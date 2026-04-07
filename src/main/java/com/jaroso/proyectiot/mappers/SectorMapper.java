package com.jaroso.proyectiot.mappers;

import com.jaroso.proyectiot.dtos.SectorCreateDto;
import com.jaroso.proyectiot.dtos.SectorDto;
import com.jaroso.proyectiot.entities.Sector;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SectorMapper {
    SectorDto toDto(Sector sector);

    @Mapping(target = "id", ignore = true)
    Sector toEntity(SectorCreateDto sectorCreateDto);
}
