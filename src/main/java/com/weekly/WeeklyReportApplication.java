package com.weekly;

import com.weekly.config.AppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = OAuth2ClientAutoConfiguration.class)
@EnableConfigurationProperties(AppConfig.class)
public class WeeklyReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeeklyReportApplication.class, args);
    }
}
