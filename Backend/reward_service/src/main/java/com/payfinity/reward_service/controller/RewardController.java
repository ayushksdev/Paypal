package com.payfinity.reward_service.controller;

import com.payfinity.reward_service.entity.Reward;
import com.payfinity.reward_service.service.RewardService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@RequestMapping("/api/rewards")
@CrossOrigin(origins = { "http://localhost:5173", "https://payfinity.vercel.app" })
public class RewardController {

    private final RewardService rewardService;
    private static final Logger log = LoggerFactory.getLogger(RewardController.class);

    public RewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping
    public List<Reward> getAllRewards() {
        log.info("Get all rewards request received");
        return rewardService.findAll();
    }

    @GetMapping("/user/{userId}")
    public List<Reward> getRewardsByUserId(@PathVariable Long userId) {
        log.info("Get rewards request received for user: {}", userId);
        return rewardService.getRewardsByUserId(userId);
    }
}
