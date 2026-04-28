package com.jaroso.proyectiot.dtos;

import java.util.List;

public record AutomationDecisionResponseDto(boolean allowed,
                                            String reason,
                                            List<AutomationActuatorActionDto> actions) {
}

