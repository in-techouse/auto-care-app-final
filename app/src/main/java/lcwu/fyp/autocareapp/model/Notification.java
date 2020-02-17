package lcwu.fyp.autocareapp.model;

import java.io.Serializable;

public class Notification implements Serializable {
    private String id, userId, providerId, bookingId, userMessage, providerMessage, date;
    private boolean read;

    public Notification() {
    }

    public Notification(String id, String userId, String providerId, String bookingId, String userMessage, String providerMessage, String date, boolean read) {
        this.id = id;
        this.userId = userId;
        this.providerId = providerId;
        this.bookingId = bookingId;
        this.userMessage = userMessage;
        this.providerMessage = providerMessage;
        this.date = date;
        this.read = read;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getProviderMessage() {
        return providerMessage;
    }

    public void setProviderMessage(String providerMessage) {
        this.providerMessage = providerMessage;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
