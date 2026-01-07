package com.p2p.realtime.exception;

public class RealtimeException extends RuntimeException {
    
    public RealtimeException(String message) {
        super(message);
    }
    
    public RealtimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
