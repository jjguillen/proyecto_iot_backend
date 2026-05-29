package com.jaroso.proyectiot.repositories;

import com.jaroso.proyectiot.entities.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, Long> {

    /** Busca un sensor por su nombre — usado para mapear claves del decoded_payload LoRa */
    Optional<Sensor> findByNombre(String nombre);
}
