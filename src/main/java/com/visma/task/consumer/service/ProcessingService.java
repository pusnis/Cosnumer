package com.visma.task.consumer.service;

import com.visma.task.consumer.model.Status;
import com.visma.task.consumer.model.StatusType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(ProcessingService.class);

    private static final String URL_INIT = "http://localhost:8037/thirdpartyservice/init";
    private static final String URL_GET = "http://localhost:8037/thirdpartyservice/checkStatus/{uuid}";

    private final RestfulService restfulService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(100);

    public ProcessingService(RestfulService restfulService) {
        this.restfulService = restfulService;
    }

    public CompletableFuture<Status> processAsync() {
        return CompletableFuture.supplyAsync(this::callInit)
                .thenCompose(this::pollUntilOk);
    }

    public String callInit() {
        String content = "content";
        ResponseEntity<String> response = restfulService.postJson(URL_INIT, content, String.class);
        return response.getBody();
    }

    public Status getStatus(String uuid) {
        ResponseEntity<Status> response = restfulService.get(URL_GET, Status.class, uuid);
        return response.getBody();
    }

    private CompletableFuture<Status> pollUntilOk(String uuid) {
        CompletableFuture<Status> future = new CompletableFuture<>();
        poll(uuid, future, 0);
        return future;
    }

    private void poll(String uuid, CompletableFuture<Status> future, int attempt) {
        if (attempt > 30) { // stop polling after 30 seconds
            future.complete(null);
            return;
        }

        scheduler.schedule(() -> {
            Status status = getStatus(uuid);
            logger.info(String.format("Polling attempt %s: %s", attempt, status));
            if (status != null && status.getStatusType() == StatusType.OK) {
                future.complete(status);
            } else {
                poll(uuid, future, attempt + 1);
            }
        }, 1, TimeUnit.SECONDS);
    }
}
