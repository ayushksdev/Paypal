package com.payfinity.reward_service.kafka;

import com.payfinity.reward_service.entity.Reward;
import com.payfinity.reward_service.entity.Transaction;
import com.payfinity.reward_service.repository.RewardRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@Component
public class RewardConsumer {

    private final RewardRepository rewardRepository;
    private static final Logger log = LoggerFactory.getLogger(RewardConsumer.class);

    public RewardConsumer(RewardRepository rewardRepository) {
        this.rewardRepository = rewardRepository;
    }

    @KafkaListener(topics = "txn-initiated", groupId = "reward-group")
    public void consumeTransaction(Transaction transaction) {

        if (rewardRepository.existsByTransactionId(transaction.getId())) {
            log.warn("⚠️ Reward already exists for transaction: {}", transaction.getId());
            return;
        }

        Reward reward = new Reward();
        reward.setUserId(transaction.getSenderId());
        reward.setPoints(transaction.getAmount() * 100);
        reward.setSentAt(LocalDateTime.now());
        reward.setTransactionId(transaction.getId());

        rewardRepository.save(reward);
        log.info("✅ Reward saved: {}", reward);
    }
}
