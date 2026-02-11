package com.weekly.controller;

import com.weekly.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ReportService.InvalidReportException.class)
    public String handleInvalidReport(ReportService.InvalidReportException ex, Model model) {
        log.warn("Invalid report generation: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(RestClientException.class)
    public String handleApiError(RestClientException ex, Model model) {
        log.error("API call failed", ex);
        model.addAttribute("errorMessage", "AI API 호출 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        log.warn("Bad request: {}", ex.getMessage());
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericError(Exception ex, Model model) {
        log.error("Unexpected error", ex);
        model.addAttribute("errorMessage", "예기치 않은 오류가 발생했습니다. 관리자에게 문의해주세요.");
        return "error";
    }
}
