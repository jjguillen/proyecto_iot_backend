package com.jaroso.proyectiot.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuracion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Configuracion {

    @Id
    @Column(name = "clave", nullable = false, length = 100)
    private String clave;

    @Column(name = "valor", nullable = false, length = 255)
    private String valor;
}

