package com.jaroso.proyectiot.entities;

/**
 * Indica el canal por el que llegó la lectura de un sensor.
 * MQTT  → broker local directo (alta frecuencia, baja latencia)
 * LORA  → red TTN LoRaWAN (envíos periódicos ~15 min, largo alcance)
 */
public enum OrigenLectura {
    MQTT,
    LORA
}

