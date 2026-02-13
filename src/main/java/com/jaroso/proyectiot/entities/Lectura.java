package com.jaroso.proyectiot.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "lecturas")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Lectura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(nullable = false)
    private Double valor;

    @Column(length = 20)
    private String unidad;

    @Column(nullable = false, name = "fecha_hora", updatable = false)
    private LocalDateTime fechaHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @PrePersist
    protected void onCreate() {
        fechaHora = LocalDateTime.now();
    }



}
