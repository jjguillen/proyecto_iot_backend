package com.jaroso.proyectiot.repositories;

import com.jaroso.proyectiot.entities.Lectura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LecturaRepository extends JpaRepository<Lectura, Long> {

    List<Lectura> findAllBySensorIdAndFechaHoraBetween(Long sensorId,
                         LocalDateTime fechaHoraAfter, LocalDateTime fechaHoraBefore);

    // Método para obtener la última lectura de un sensor (ordenada por fechaHora descendente)
    Optional<Lectura> findTopBySensorIdOrderByFechaHoraDesc(Long sensorId);




}
