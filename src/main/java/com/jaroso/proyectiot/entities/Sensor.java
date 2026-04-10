package com.jaroso.proyectiot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


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

    private String topicMQTT; //Topic para la lectura del sensor

    private String topicMQTTAct; //Topic para cambiar el estado del actuador

    private Integer valorMin; //Umbral mínimo

    private Integer valorMax; //Umbral máximo

    private Boolean isActuador; //Indica si es un actuador o no

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSensor tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSensor estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;



}
