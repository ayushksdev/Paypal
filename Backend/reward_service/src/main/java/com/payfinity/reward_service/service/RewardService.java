package com.payfinity.reward_service.service;

import com.payfinity.reward_service.entity.Reward;

import java.util.List;

public interface RewardService {
    List<Reward> findAll();
    Reward sendReward(Reward reward);
    List<Reward> getRewardsByUserId(Long UserId);
}