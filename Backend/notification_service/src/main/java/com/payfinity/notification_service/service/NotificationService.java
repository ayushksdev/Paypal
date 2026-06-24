package com.payfinity.notification_service.service;

import com.payfinity.notification_service.entity.Notification;

import java.util.List;

public interface NotificationService {
    Notification sendNotification(Notification notification);
    List<Notification> getNotificationsByUserId(Long userId);
    void clearNotifications(Long userId);
}
