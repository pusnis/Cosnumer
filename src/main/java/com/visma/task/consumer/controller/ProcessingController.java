package com.visma.task.consumer.controller;

import com.visma.task.consumer.model.Status;
import com.visma.task.consumer.model.StatusType;
import com.visma.task.consumer.service.ProcessingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/items")
public class ProcessingController {

    private final ProcessingService processingService;

    public ProcessingController(ProcessingService processingService) {
        this.processingService = processingService;
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<Status>> createItem() {
        return processingService.processAsync()
                .thenApply(status -> {
                    if (status != null && status.getStatusType() == StatusType.OK) {
                        return ResponseEntity.ok(status);
                    } else {
                        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(status);
                    }
                });
    }
}
