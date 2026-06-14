package com.paypal.reward_service.service;

import com.paypal.reward_service.entity.Reward;
import com.paypal.reward_service.repository.RewardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RewardServiceImpl implements RewardService{
    @Autowired
    private RewardRepository rewardRepository;
    private static final Logger log = LoggerFactory.getLogger(RewardServiceImpl.class);

    @Override
    public Reward sendReward(Reward reward) {
        log.info("Saving reward to database for user: {}", reward.getUserId());
        reward.setSentAt(LocalDateTime.now());
        return rewardRepository.save(reward);
    }

    @Override
    public List<Reward> getRewardsByUserId(Long userId) {
        log.info("Fetching rewards from database for user: {}", userId);
        return rewardRepository.findByUserId(userId);
    }

    @Override
    public List<Reward> findAll() {
        log.info("Fetching all rewards from database");
        return rewardRepository.findAll();
    }
}