package com.jaroso.proyectiot.controllers;

import com.jaroso.proyectiot.dtos.AutomationActuatorRequestDto;
import com.jaroso.proyectiot.dtos.AutomationDecisionResponseDto;
import com.jaroso.proyectiot.services.ActuatorAutomationService;
import com.jaroso.proyectiot.services.ConfiguracionService;
import com.jaroso.proyectiot.services.ConfiguracionService.AutomationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/automatizaciones")
public class AutomationController {

    @Autowired
    private ActuatorAutomationService actuatorAutomationService;

    @Autowired
    private ConfiguracionService configuracionService;

    @PostMapping("/actuadores/decidir")
    public ResponseEntity<AutomationDecisionResponseDto> decideAndApply(@RequestBody AutomationActuatorRequestDto request) {
        AutomationDecisionResponseDto response =
                actuatorAutomationService.decideAndApply(request.actuadorId(), request.targetState());

        if (!response.allowed()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    // ── Configuración de automatizaciones ──────────────────────────────────────

    /** Devuelve el estado actual de todas las flags de automatización. */
    @GetMapping("/config")
    public ResponseEntity<AutomationConfig> getConfig() {
        return ResponseEntity.ok(configuracionService.getAll());
    }

    /** Activa o desactiva la automatización de niveles de balsa. */
    @PutMapping("/config/nivel")
    public ResponseEntity<AutomationConfig> setNivel(@RequestParam boolean enabled) {
        configuracionService.setNivelEnabled(enabled);
        return ResponseEntity.ok(configuracionService.getAll());
    }

    /** Activa o desactiva la automatización de riego por humedad. */
    @PutMapping("/config/humedad")
    public ResponseEntity<AutomationConfig> setHumedad(@RequestParam boolean enabled) {
        configuracionService.setHumedadEnabled(enabled);
        return ResponseEntity.ok(configuracionService.getAll());
    }
}

