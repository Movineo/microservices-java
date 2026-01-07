package com.p2p.booking.service;

import com.p2p.booking.dto.BookingRequestDto;
import com.p2p.booking.events.BookingRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class BookingEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(BookingEventConsumer.class);
    
    @Autowired
    private BookingService bookingService;
    
    @KafkaListener(topics = "booking-events", groupId = "booking-service-group")
    public void handleBookingRequested(BookingRequestedEvent event) {
        logger.info("Received booking requested event for booking ID: {}", event.getBookingId());
        
        if (!"booking-requested".equals(event.getEventType())) {
            logger.debug("Ignoring non-booking-requested event: {}", event.getEventType());
            return;
        }
        
        try {
            // Convert event to DTO
            BookingRequestDto requestDto = convertEventToDto(event);
            
            // Process booking
            bookingService.processBooking(event.getUserId(), event.getPhoneNumber(), requestDto);
            
            logger.info("Successfully processed booking request: {}", event.getBookingId());
            
        } catch (Exception e) {
            logger.error("Failed to process booking request {}: {}", 
                        event.getBookingId(), e.getMessage(), e);
        }
    }
    
    private BookingRequestDto convertEventToDto(BookingRequestedEvent event) {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setStartLocation(event.getStartPoint());
        dto.setEndLocation(event.getEndPoint());
        
        if (event.getPickupLatitude() != null) {
            dto.setStartLatitude(BigDecimal.valueOf(event.getPickupLatitude()));
        }
        if (event.getPickupLongitude() != null) {
            dto.setStartLongitude(BigDecimal.valueOf(event.getPickupLongitude()));
        }
        if (event.getDropoffLatitude() != null) {
            dto.setEndLatitude(BigDecimal.valueOf(event.getDropoffLatitude()));
        }
        if (event.getDropoffLongitude() != null) {
            dto.setEndLongitude(BigDecimal.valueOf(event.getDropoffLongitude()));
        }
        
        // Parse travel date and preferred time
        LocalDateTime preferredDateTime = parsePreferredDateTime(event.getTravelDate(), event.getPreferredTime());
        dto.setPreferredDateTime(preferredDateTime);
        
        dto.setPassengerCount(event.getPassengerCount() != null ? event.getPassengerCount() : 1);
        dto.setProviderType(event.getProviderType());
        dto.setSpecialRequests(event.getSpecialRequests());
        
        return dto;
    }
    
    private LocalDateTime parsePreferredDateTime(String travelDate, String preferredTime) {
        try {
            // If no date provided, use current date
            if (travelDate == null || travelDate.trim().isEmpty()) {
                return LocalDateTime.now().plusHours(1); // Default to 1 hour from now
            }
            
            // Parse date
            LocalDateTime dateTime = LocalDateTime.parse(travelDate + "T00:00:00");
            
            // Add preferred time if provided
            if (preferredTime != null && !preferredTime.trim().isEmpty()) {
                String timeStr = preferredTime.toLowerCase().trim();
                
                switch (timeStr) {
                    case "morning":
                        dateTime = dateTime.withHour(8).withMinute(0);
                        break;
                    case "afternoon":
                        dateTime = dateTime.withHour(14).withMinute(0);
                        break;
                    case "evening":
                        dateTime = dateTime.withHour(18).withMinute(0);
                        break;
                    case "night":
                        dateTime = dateTime.withHour(20).withMinute(0);
                        break;
                    default:
                        // Try to parse as HH:mm format
                        try {
                            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                            String[] timeParts = timeStr.split(":");
                            if (timeParts.length == 2) {
                                int hour = Integer.parseInt(timeParts[0]);
                                int minute = Integer.parseInt(timeParts[1]);
                                dateTime = dateTime.withHour(hour).withMinute(minute);
                            }
                        } catch (Exception e) {
                            logger.warn("Could not parse preferred time: {}, using default", preferredTime);
                            dateTime = dateTime.withHour(8).withMinute(0);
                        }
                        break;
                }
            } else {
                // Default to 8 AM
                dateTime = dateTime.withHour(8).withMinute(0);
            }
            
            return dateTime;
            
        } catch (Exception e) {
            logger.warn("Could not parse travel date: {}, using current time + 1 hour", travelDate);
            return LocalDateTime.now().plusHours(1);
        }
    }
}