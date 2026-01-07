package com.p2p.realtime.controller;

import com.p2p.realtime.dto.EmergencyRequest;
import com.p2p.realtime.dto.EmergencyResponse;
import com.p2p.realtime.service.EmergencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/emergency")
@RequiredArgsConstructor
@Slf4j
public class EmergencyController {

    private final EmergencyService emergencyService;
    
    /**
     * Report a new emergency
     */
    @PostMapping
    public ResponseEntity<EmergencyResponse> reportEmergency(@Valid @RequestBody EmergencyRequest request) {
        return ResponseEntity.ok(emergencyService.reportEmergency(request));
    }
    
    /**
     * Get all active emergencies
     */
    @GetMapping
    public ResponseEntity<List<EmergencyResponse>> getAllActiveEmergencies() {
        return ResponseEntity.ok(emergencyService.getAllActiveEmergencies());
    }
    
    /**
     * Get active emergencies for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EmergencyResponse>> getUserEmergencies(@PathVariable UUID userId) {
        return ResponseEntity.ok(emergencyService.getUserEmergencies(userId));
    }
    
    /**
     * Get active emergencies for a trip
     */
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<EmergencyResponse>> getTripEmergencies(@PathVariable UUID tripId) {
        return ResponseEntity.ok(emergencyService.getTripEmergencies(tripId));
    }
    
    /**
     * Resolve an emergency
     */
    @PutMapping("/{emergencyId}/resolve")
    public ResponseEntity<EmergencyResponse> resolveEmergency(@PathVariable UUID emergencyId) {
        return ResponseEntity.ok(emergencyService.resolveEmergency(emergencyId));
    }
}
