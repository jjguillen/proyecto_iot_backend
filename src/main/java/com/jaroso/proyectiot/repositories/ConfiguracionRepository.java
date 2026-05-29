package com.jaroso.proyectiot.repositories;

import com.jaroso.proyectiot.entities.Configuracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionRepository extends JpaRepository<Configuracion, String> {
}

