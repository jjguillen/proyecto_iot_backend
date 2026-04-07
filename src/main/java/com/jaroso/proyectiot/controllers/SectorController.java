package com.jaroso.proyectiot.controllers;

import com.jaroso.proyectiot.dtos.SectorCreateDto;
import com.jaroso.proyectiot.dtos.SectorDto;
import com.jaroso.proyectiot.entities.Sector;
import com.jaroso.proyectiot.mappers.SectorMapper;
import com.jaroso.proyectiot.repositories.SectorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class SectorController {

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private SectorMapper mapper;

    @GetMapping("/sectors")
    public ResponseEntity<List<SectorDto>> getAll(){
        return ResponseEntity.ok(sectorRepository.findAll().stream().map(mapper::toDto).toList());
    }

    @GetMapping("/sectors/{id}")
    public ResponseEntity<SectorDto> getById(@PathVariable Long id){
        Optional<SectorDto> sensor = sectorRepository.findById(id).map(mapper::toDto);
        return sensor.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/sectors")
    public ResponseEntity<SectorDto> createSensor(@RequestBody SectorCreateDto sector){
        Sector sectorEntity = mapper.toEntity(sector);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(mapper.toDto(sectorRepository.save(sectorEntity)));
    }

    @DeleteMapping("/sectors/{id}")
    public ResponseEntity<Void> deleteSensor(@PathVariable Long id){
        sectorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }


}
