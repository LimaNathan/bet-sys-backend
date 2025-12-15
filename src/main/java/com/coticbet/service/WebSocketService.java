package com.coticbet.service;

import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.coticbet.dto.response.EventResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastEventUpdate(EventResponse event) {
        messagingTemplate.convertAndSend("/topic/events", event);
    }

    public void broadcastAdminRequest(Object request) {
        messagingTemplate.convertAndSend("/topic/admin/requests", request);
    }

    /**
     * Notify a user by their userId. Looks up the email to use as principal.
     */
    public void notifyUser(String userId, String type, String message) {
        Map<String, String> notification = Map.of(
                "type", type,
                "message", message);

        // Also broadcast to user-specific topic using userId (simpler approach)
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", notification);
    }

    public void notifyUser(String userId, Object payload) {
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", payload);
    }

    /**
     * Broadcast a notification to all connected users (global topic)
     */
    public void broadcastGlobalNotification(String type, String message) {
        Map<String, String> notification = Map.of(
                "type", type,
                "message", message);
        messagingTemplate.convertAndSend("/topic/global/notifications", notification);
    }
}
