package com.paypal.transaction_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paypal.transaction_service.entity.Transaction;
import com.paypal.transaction_service.kafka.KafkaEventProducer;
import com.paypal.transaction_service.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository repository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaEventProducer kafkaEventProducer;

    @Value("${wallet.service.url}")
    private String walletServiceUrl;

    public TransactionServiceImpl(
            TransactionRepository repository,
            KafkaEventProducer kafkaEventProducer,
            ObjectMapper objectMapper,
            RestTemplate restTemplate) {

        this.repository = repository;
        this.kafkaEventProducer = kafkaEventProducer;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public Transaction createTransaction(Transaction request) {
        log.info("Starting transaction from sender {} to receiver {} for amount {}",
                request.getSenderId(), request.getReceiverId(), request.getAmount());

        Transaction transaction = initializeTransaction(request);

        // FIX 3: Track hold reference so we can release it on failure
        String holdReference = null;

        try {
            // FIX 1: Convert Double amount to long (wallet service expects Long)
            long amountLong = request.getAmount().longValue();

            holdReference = placeHold(transaction.getSenderId(), amountLong);
            log.info("Hold placed successfully with reference: {}", holdReference);

            validateReceiver(transaction.getReceiverId());
            log.info("Receiver {} validated successfully", transaction.getReceiverId());

            captureHold(holdReference);
            log.info("Hold captured successfully for reference: {}", holdReference);

            creditReceiver(transaction.getReceiverId(), amountLong);
            log.info("Receiver {} credited successfully", transaction.getReceiverId());

            transaction.setStatus("SUCCESS");
            repository.save(transaction);

            publishKafkaEvent(transaction);

            return transaction;

        } catch (Exception ex) {
            log.error("Transaction failed: {}", ex.getMessage(), ex);

            // FIX 3: Release the hold if it was placed but capture/credit failed
            if (holdReference != null) {
                try {
                    releaseHold(holdReference);
                    log.info("Hold {} released after transaction failure", holdReference);
                } catch (Exception releaseEx) {
                    log.error("Failed to release hold {} after transaction failure: {}",
                            holdReference, releaseEx.getMessage(), releaseEx);
                }
            }

            transaction.setStatus("FAILED");
            repository.save(transaction);

            throw new RuntimeException("Transaction failed: " + ex.getMessage());
        }
    }

    @Override
    public Transaction getTransactionById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return repository.findAll();
    }

    @Override
    public List<Transaction> getTransactionsByUser(Long userId) {
        return repository.findBySenderIdOrReceiverId(userId, userId);
    }

    private Transaction initializeTransaction(Transaction request) {
        request.setStatus("PENDING");
        request.setTimestamp(LocalDateTime.now());
        return repository.save(request);
    }

    // FIX 1: Parameter is now long, serialized as %d (integer) not %.2f (float)
    private String placeHold(Long userId, long amount) throws Exception {
        String payload = String.format(
                "{\"userId\": %d, \"currency\": \"INR\", \"amount\": %d}",
                userId,
                amount   // ← was %.2f (Double), Wallet Service expects Long
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                walletServiceUrl + "/hold",
                createEntity(payload),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to place hold");
        }

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        return jsonNode.get("holdReference").asText();
    }

    private void validateReceiver(Long receiverId) {
        ResponseEntity<String> response = restTemplate.getForEntity(
                walletServiceUrl + "/" + receiverId,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Receiver wallet not found");
        }
    }

    private void captureHold(String holdReference) {
        String payload = String.format("{\"holdReference\":\"%s\"}", holdReference);

        ResponseEntity<String> response = restTemplate.postForEntity(
                walletServiceUrl + "/capture",
                createEntity(payload),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Capture failed");
        }
    }

    // FIX 3: New method to release hold on partial failure
    private void releaseHold(String holdReference) {
        String url = walletServiceUrl + "/release/" + holdReference;
        restTemplate.postForEntity(url, null, String.class);
    }

    // FIX 1: Parameter is now long, serialized as %d
    private void creditReceiver(Long receiverId, long amount) {
        String payload = String.format(
                "{\"userId\": %d, \"currency\": \"INR\", \"amount\": %d}",
                receiverId,
                amount   // ← was %.2f (Double)
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                walletServiceUrl + "/credit",
                createEntity(payload),
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Credit failed");
        }
    }

    private HttpEntity<String> createEntity(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(json, headers);
    }

    private void publishKafkaEvent(Transaction transaction) {
        try {
            kafkaEventProducer.sendTransactionEvent(
                    String.valueOf(transaction.getId()),
                    transaction
            );
        } catch (Exception ex) {
            log.error("Kafka publish failed: {}", ex.getMessage(), ex);
        }
    }
}