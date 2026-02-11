package com.weekly.service;

import com.weekly.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TeamsWebhookService {

    private static final Logger log = LoggerFactory.getLogger(TeamsWebhookService.class);

    private final RestClient teamsRestClient;
    private final AppConfig.TeamsConfig teamsConfig;

    public TeamsWebhookService(RestClient teamsRestClient, AppConfig appConfig) {
        this.teamsRestClient = teamsRestClient;
        this.teamsConfig = appConfig.teams();
    }

    public boolean isEnabled() {
        return teamsConfig.isEnabled();
    }

    public void sendReport(String generalReport) {
        if (!isEnabled()) {
            throw new IllegalStateException("Teams webhook URL이 설정되지 않았습니다");
        }

        Map<String, Object> payload = AdaptiveCardBuilder.build(generalReport);

        log.info("Sending report to Teams webhook");

        teamsRestClient.post()
                .uri(teamsConfig.webhookUrl())
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        log.info("Teams webhook send successful");
    }
}
