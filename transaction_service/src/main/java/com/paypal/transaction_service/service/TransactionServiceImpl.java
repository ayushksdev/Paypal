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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

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

        Transaction transaction = initializeTransaction(request);

        try {

            String holdReference = placeHold(
                    transaction.getSenderId(),
                    transaction.getAmount()
            );

            validateReceiver(transaction.getReceiverId());

            captureHold(holdReference);

            creditReceiver(
                    transaction.getReceiverId(),
                    transaction.getAmount()
            );

            transaction.setStatus("SUCCESS");

            repository.save(transaction);

            publishKafkaEvent(transaction);

            return transaction;

        } catch (Exception ex) {

            transaction.setStatus("FAILED");

            repository.save(transaction);

            throw new RuntimeException(
                    "Transaction failed: " + ex.getMessage()
            );
        }
    }

    @Override
    public Transaction getTransactionById(Long id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Transaction not found"));
    }

    @Override
    public List<Transaction> getAllTransactions() {

        return repository.findAll();
    }

    @Override
    public List<Transaction> getTransactionsByUser(Long userId) {

        return repository.findBySenderIdOrReceiverId(
                userId,
                userId
        );
    }

    private Transaction initializeTransaction(Transaction request) {

        request.setStatus("PENDING");
        request.setTimestamp(LocalDateTime.now());

        return repository.save(request);
    }

    private String placeHold(
            Long userId,
            Double amount
    ) throws Exception {

        String payload = String.format(
                "{\"userId\": %d, \"currency\": \"INR\", \"amount\": %.2f}",
                userId,
                amount
        );

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        walletServiceUrl + "/hold",
                        createEntity(payload),
                        String.class
                );

        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null) {

            throw new RuntimeException("Failed to place hold");
        }

        JsonNode jsonNode =
                objectMapper.readTree(response.getBody());

        return jsonNode.get("holdReference").asText();
    }

    private void validateReceiver(Long receiverId) {

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        walletServiceUrl + "/" + receiverId,
                        String.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {

            throw new RuntimeException(
                    "Receiver wallet not found"
            );
        }
    }

    private void captureHold(String holdReference) {

        String payload = String.format(
                "{\"holdReference\":\"%s\"}",
                holdReference
        );

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        walletServiceUrl + "/capture",
                        createEntity(payload),
                        String.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {

            throw new RuntimeException("Capture failed");
        }
    }

    private void creditReceiver(
            Long receiverId,
            Double amount
    ) {

        String payload = String.format(
                "{\"userId\": %d, \"currency\": \"INR\", \"amount\": %.2f}",
                receiverId,
                amount
        );

        ResponseEntity<String> response =
                restTemplate.postForEntity(
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

            System.err.println(
                    "Kafka publish failed: "
                            + ex.getMessage()
            );
        }
    }
}