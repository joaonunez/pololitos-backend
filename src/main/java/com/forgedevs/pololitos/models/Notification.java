package com.forgedevs.pololitos.models;

import java.time.Instant;

public class Notification {

    private String id; // Auto-generated by Firebase
    private Long receiverId; // User who receives the notification
    private String type; // Type: "message", "request", "status"
    private String message; // Message shown to the user
    private String targetUrl; // Redirect URL on click
    private boolean read = false; // Whether the user has seen it
    private String timestamp; // ISO-8601 timestamp
    private Long senderId;
    private String senderName;

    private String serviceName;
    private String serviceImage;

    public Notification() {}

    public Notification(Long receiverId, String type, String message, String targetUrl, String timestamp,
                        Long senderId, String senderName, String serviceName, String serviceImage) {
        this.receiverId = receiverId;
        this.type = type;
        this.message = message;
        this.targetUrl = targetUrl;
        this.timestamp = timestamp;
        this.read = false;
        this.senderId = senderId;
        this.senderName = senderName;
        this.serviceName = serviceName;
        this.serviceImage = serviceImage;
    }

    public static Notification createStatusChange(Request request, String status, String message) {
        User requester = request.getRequester();
        User provider = request.getService().getUser();
        OfferedService service = request.getService();

        Long receiverId;
        Long senderId;
        String senderName;
        String targetUrl;

        if (status.equals("Cancelada")) {
            receiverId = provider.getId();
            senderId = requester.getId();
            senderName = requester.getFirstName() + " " + requester.getLastName();
            targetUrl = "/my-received-requests";
        } else {
            receiverId = requester.getId();
            senderId = provider.getId();
            senderName = provider.getFirstName() + " " + provider.getLastName();
            targetUrl = "/my-sent-requests";
        }

        return new Notification(
            receiverId,
            "Cambio de Estado",
            message + " para el servicio: " + service.getName(),
            targetUrl,
            Instant.now().toString(),
            senderId,
            senderName,
            service.getName(),
            service.getImageUrl()
        );
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceImage() {
        return serviceImage;
    }

    public void setServiceImage(String serviceImage) {
        this.serviceImage = serviceImage;
    }
}
