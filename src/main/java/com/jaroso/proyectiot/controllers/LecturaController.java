package com.jaroso.proyectiot.controllers;

import com.jaroso.proyectiot.dtos.IntervaloFechasDto;
import com.jaroso.proyectiot.dtos.LecturaCreateDto;
import com.jaroso.proyectiot.dtos.LecturaDto;
import com.jaroso.proyectiot.entities.Lectura;
import com.jaroso.proyectiot.entities.Sensor;
import com.jaroso.proyectiot.mappers.LecturaMapper;
import com.jaroso.proyectiot.repositories.LecturaRepository;
import com.jaroso.proyectiot.repositories.SensorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
public class LecturaController {

    Logger logger = Logger.getLogger(LecturaController.class.getName());

    @Autowired
    private LecturaRepository lecturaRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private LecturaMapper lecturaMapper;

    /**
     * Devuelve todas las lecturas de la BBDD
     */
    @GetMapping("/lecturas")
    public ResponseEntity<List<LecturaDto>> getAllLecturas() {
        return ResponseEntity.ok(lecturaRepository.findAll().stream().map(lecturaMapper::toDto).toList());
    }

    /**
     * Devuelve las lecturas de un sensor en un intervalo de fechas
     * @param idSensor
     * @param intervaloFechasDto (Json)
     */
    @GetMapping("/lecturas/{idSensor}")
    public ResponseEntity<List<LecturaDto>> getLecturasIntervalo(@PathVariable Long idSensor,
                                                                 @RequestBody IntervaloFechasDto intervaloFechasDto) {
        List<Lectura> lecturas = lecturaRepository
                .findAllBySensorIdAndFechaHoraBetween(idSensor,
                        intervaloFechasDto.inicio(),
                        intervaloFechasDto.fin());
        return ResponseEntity.ok(lecturas.stream().map(lecturaMapper::toDto).toList());
    }

    /**
     * Guarda una lectura en la BBDD
     * @param lecturaCreateDto  (Json) con los datos de la lectura a guardar,
     *                          incluido el id del sensor al que pertenece
     */
    @PostMapping("/lecturas")
    @Transactional
    public ResponseEntity<LecturaDto> saveLectura(@RequestBody LecturaCreateDto lecturaCreateDto){
        //logger.info("Guardando lectura: " + lecturaCreateDto);

        Lectura lectura = lecturaMapper.toEntity(lecturaCreateDto);

        //Sacamos el sensor de la BBDD, esta consulta es rápida pues hay pocos sensores en BBDD
        //Se podría optimizar un poco con existsById y getReferencedById
        Optional<Sensor> sensor = sensorRepository.findById(lecturaCreateDto.sensorId());
        if (sensor.isEmpty()) {
            return ResponseEntity.notFound().build();
        } else {
            //Le ponemos el id de sensor a la lectura y la insertamos
            lectura.setSensor(sensor.get());
            lecturaRepository.save(lectura);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(lecturaMapper.toDto(lectura));
        }
    }


}
