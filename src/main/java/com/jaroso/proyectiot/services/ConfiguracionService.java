package com.jaroso.proyectiot.services;

import com.jaroso.proyectiot.entities.Configuracion;
import com.jaroso.proyectiot.repositories.ConfiguracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfiguracionService {

    private static final String KEY_AUTO_NIVEL   = "automatizacion.nivel.enabled";
    private static final String KEY_AUTO_HUMEDAD = "automatizacion.humedad.enabled";

    @Autowired
    private ConfiguracionRepository configuracionRepository;

    // ---- Nivel ----

    public boolean isNivelEnabled() {
        return isEnabled(KEY_AUTO_NIVEL);
    }

    public boolean setNivelEnabled(boolean enabled) {
        return setEnabled(KEY_AUTO_NIVEL, enabled);
    }

    // ---- Humedad ----

    public boolean isHumedadEnabled() {
        return isEnabled(KEY_AUTO_HUMEDAD);
    }

    public boolean setHumedadEnabled(boolean enabled) {
        return setEnabled(KEY_AUTO_HUMEDAD, enabled);
    }

    // ---- Helpers ----

    /**
     * Devuelve el valor de la flag. Si no existe en BD, devuelve true (activo por defecto).
     */
    private boolean isEnabled(String clave) {
        return configuracionRepository.findById(clave)
                .map(c -> "true".equalsIgnoreCase(c.getValor()))
                .orElse(true);
    }

    private boolean setEnabled(String clave, boolean enabled) {
        Configuracion cfg = configuracionRepository.findById(clave)
                .orElse(new Configuracion(clave, "true"));
        cfg.setValor(Boolean.toString(enabled));
        configuracionRepository.save(cfg);
        return enabled;
    }

    /**
     * Devuelve el mapa completo de flags para exponerlo en el endpoint GET.
     */
    public AutomationConfig getAll() {
        return new AutomationConfig(isNivelEnabled(), isHumedadEnabled());
    }

    public record AutomationConfig(boolean nivelEnabled, boolean humedadEnabled) {}
}

