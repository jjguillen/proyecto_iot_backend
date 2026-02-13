package com.jaroso.proyectiot.controllers;

import com.jaroso.proyectiot.dtos.SensorCreateDto;
import com.jaroso.proyectiot.dtos.SensorDto;
import com.jaroso.proyectiot.dtos.SensorUpdateDto;
import com.jaroso.proyectiot.entities.Sensor;
import com.jaroso.proyectiot.mappers.SensorMapper;
import com.jaroso.proyectiot.repositories.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class SensorController {

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private SensorMapper mapper;

    @GetMapping("/sensors")
    public ResponseEntity<List<SensorDto>> getAll(){
        return ResponseEntity.ok(sensorRepository.findAll().stream().map(mapper::toDto).toList());
    }

    @GetMapping("/sensors/{id}")
    public ResponseEntity<SensorDto> getById(@PathVariable Long id){
        Optional<SensorDto> sensor = sensorRepository.findById(id).map(mapper::toDto);
        return sensor.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/sensors")
    public ResponseEntity<SensorDto> createSensor(@RequestBody SensorCreateDto sensor){
        Sensor sensorEntity = mapper.toEntity(sensor);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(mapper.toDto(sensorRepository.save(sensorEntity)));
    }

    @PutMapping("/sensors/{id}")
    public ResponseEntity<SensorDto> updateSensor(@PathVariable Long id, @RequestBody SensorUpdateDto sensorUpdateDto){
        Optional<Sensor> sensor = sensorRepository.findById(id);
        if (sensor.isPresent()){
            sensor.get().setEstado(sensorUpdateDto.estado());
            return ResponseEntity.ok(mapper.toDto(sensorRepository.save(sensor.get())));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/sensors/{id}")
    public ResponseEntity<Void> deleteSensor(@PathVariable Long id){
        sensorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }


}
