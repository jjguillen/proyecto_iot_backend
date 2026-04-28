package com.jaroso.proyectiot.controllers;

import com.jaroso.proyectiot.dtos.AutomationActuatorRequestDto;
import com.jaroso.proyectiot.dtos.AutomationDecisionResponseDto;
import com.jaroso.proyectiot.services.ActuatorAutomationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AutomationController {

    @Autowired
    private ActuatorAutomationService actuatorAutomationService;

    @PostMapping("/automatizaciones/actuadores/decidir")
    public ResponseEntity<AutomationDecisionResponseDto> decideAndApply(@RequestBody AutomationActuatorRequestDto request) {
        AutomationDecisionResponseDto response =
                actuatorAutomationService.decideAndApply(request.actuadorId(), request.targetState());

        if (!response.allowed()) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}

