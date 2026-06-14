package com.paypal.notification_service.controller;

import com.paypal.notification_service.service.NotificationService;
import com.paypal.notification_service.entity.Notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/notify")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @PostMapping
    public Notification sendNotification(@RequestBody Notification notification) {
        log.info("Send notification request received for user: {}", notification.getUserId());
        return notificationService.sendNotification(notification);
    }

    @GetMapping("/{userId}")
    public List<Notification> getNotificationsByUser(@PathVariable Long userId) {
        log.info("Get notifications request received for user: {}", userId);
        return notificationService.getNotificationsByUserId(userId);
    }
}