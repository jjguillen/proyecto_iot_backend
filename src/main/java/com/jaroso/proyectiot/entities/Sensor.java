package com.jaroso.proyectiot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sensores")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Sensor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    private String nombre;

    private String descripcion;

    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSensor tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSensor estado;


}
