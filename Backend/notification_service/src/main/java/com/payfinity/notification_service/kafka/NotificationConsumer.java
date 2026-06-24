package com.payfinity.notification_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.payfinity.notification_service.entity.Notification;
import com.payfinity.notification_service.entity.Transaction;
import com.payfinity.notification_service.repository.NotificationRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Component
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper mapper;
    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    public NotificationConsumer(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @KafkaListener(topics = "txn-initiated", groupId = "notification-group")
    public void consumeTransaction(Transaction transaction) {
        log.info("📥 Received transaction: {}", transaction);

        // FIX 2: Notify the RECEIVER (they received money), not the sender
        // Also create a separate notification for the sender (debit confirmation)

        // Notification for RECEIVER
        Notification receiverNotification = new Notification();
        receiverNotification.setUserId(transaction.getReceiverId());   // ← was senderId
        receiverNotification.setMessage(
                "💰 ₹" + transaction.getAmount() +
                " received from user " + transaction.getSenderId()
        );
        receiverNotification.setSentAt(LocalDateTime.now());
        notificationRepository.save(receiverNotification);

        // Notification for SENDER (debit confirmation)
        Notification senderNotification = new Notification();
        senderNotification.setUserId(transaction.getSenderId());
        senderNotification.setMessage(
                "✅ ₹" + transaction.getAmount() +
                " sent to user " + transaction.getReceiverId() + " successfully"
        );
        senderNotification.setSentAt(LocalDateTime.now());
        notificationRepository.save(senderNotification);

        log.info("✅ Notifications saved for sender {} and receiver {}",
                transaction.getSenderId(), transaction.getReceiverId());
    }
}