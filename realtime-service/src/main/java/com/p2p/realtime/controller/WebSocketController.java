package com.p2p.realtime.controller;

import com.p2p.realtime.dto.*;
import com.p2p.realtime.service.EmergencyService;
import com.p2p.realtime.service.SafetyAlertService;
import com.p2p.realtime.service.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final TrackingService trackingService;
    private final SafetyAlertService safetyAlertService;
    private final EmergencyService emergencyService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Handle location updates via WebSocket
     */
    @MessageMapping("/ws/location/update")
    public void handleLocationUpdate(@Payload LocationUpdateRequest request) {
        LocationResponse response = trackingService.updateLocation(request);
        messagingTemplate.convertAndSend("/topic/vehicle/" + request.getVehicleId(),
                WebSocketMessage.create(WebSocketMessage.MessageType.LOCATION_UPDATE.name(), response));
    }
    
    /**
     * Handle emergency reports via WebSocket
     */
    @MessageMapping("/ws/emergency/report")
    @SendTo("/topic/emergency")
    public WebSocketMessage<EmergencyResponse> handleEmergencyReport(@Payload EmergencyRequest request) {
        EmergencyResponse response = emergencyService.reportEmergency(request);
        return WebSocketMessage.create(WebSocketMessage.MessageType.EMERGENCY.name(), response);
    }
    
    /**
     * Handle safety alerts via WebSocket
     */
    @MessageMapping("/ws/alert/report")
    public void handleSafetyAlert(@Payload SafetyAlertRequest request) {
        SafetyAlertResponse response = safetyAlertService.createAlert(request);
        
        // Send to area-specific topic
        String geohashPrefix = response.getGeohash().substring(0, 4);
        messagingTemplate.convertAndSend("/topic/alerts/area/" + geohashPrefix,
                WebSocketMessage.create(WebSocketMessage.MessageType.SAFETY_ALERT.name(), response));
    }
}
