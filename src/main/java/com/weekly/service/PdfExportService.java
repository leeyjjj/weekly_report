package com.weekly.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.weekly.entity.Report;
import com.weekly.entity.TeamReport;
import com.weekly.util.MarkdownRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfExportService {

    private static final Logger log = LoggerFactory.getLogger(PdfExportService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MarkdownRenderer markdownRenderer;

    public PdfExportService(MarkdownRenderer markdownRenderer) {
        this.markdownRenderer = markdownRenderer;
    }

    public byte[] exportReport(Report report) throws IOException {
        String title = report.getUser().getName() + "님의 주간보고";
        StringBuilder meta = new StringBuilder();
        if (report.getUser().getTeam() != null) {
            meta.append("<p class=\"meta\">팀: ").append(escape(report.getUser().getTeam().getName())).append("</p>");
        }
        if (report.getPeriodStart() != null) {
            meta.append("<p class=\"meta\">업무기간: ")
                    .append(report.getPeriodStart().format(DATE_FMT))
                    .append(" ~ ")
                    .append(report.getPeriodEnd().format(DATE_FMT))
                    .append("</p>");
        }
        meta.append("<p class=\"meta\">작성일: ").append(report.getCreatedAt().format(DATETIME_FMT)).append("</p>");

        String bodyHtml = toXhtml(markdownRenderer.toHtml(report.getGeneralReportMd()));
        return renderPdf(title, meta.toString(), bodyHtml);
    }

    public byte[] exportTeamReport(TeamReport teamReport) throws IOException {
        String title = teamReport.getTeam().getName() + " 팀 통합 주간보고";
        String meta = "<p class=\"meta\">기간: " + teamReport.getWeekStart().format(DATE_FMT)
                + " ~ " + teamReport.getWeekStart().plusDays(6).format(DATE_FMT) + "</p>"
                + "<p class=\"meta\">" + teamReport.getReportCount() + "명의 보고서 종합</p>";

        String bodyHtml = toXhtml(markdownRenderer.toHtml(teamReport.getAggregatedMd()));
        return renderPdf(title, meta, bodyHtml);
    }

    private byte[] renderPdf(String title, String metaHtml, String bodyHtml) throws IOException {
        String xhtml = buildXhtml(title, metaHtml, bodyHtml);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            try {
                ClassPathResource fontResource = new ClassPathResource("fonts/NanumGothic-Regular.ttf");
                try (InputStream fontStream = fontResource.getInputStream()) {
                    byte[] fontBytes = fontStream.readAllBytes();
                    builder.useFont(() -> new java.io.ByteArrayInputStream(fontBytes), "NanumGothic");
                }
            } catch (Exception e) {
                log.warn("NanumGothic font not found, PDF may not render Korean correctly", e);
            }

            builder.withHtmlContent(xhtml, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }

    private String buildXhtml(String title, String metaHtml, String bodyHtml) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                    <style>
                        body {
                            font-family: 'NanumGothic', sans-serif;
                            font-size: 10pt;
                            line-height: 1.6;
                            color: #1a1a2e;
                            margin: 40px;
                        }
                        h1 {
                            font-size: 18pt;
                            color: #1a1a2e;
                            border-bottom: 2px solid #4361ee;
                            padding-bottom: 8px;
                            margin-bottom: 4px;
                        }
                        .meta {
                            font-size: 9pt;
                            color: #64748b;
                            margin: 2px 0;
                        }
                        .content {
                            margin-top: 20px;
                        }
                        h2 { font-size: 14pt; color: #2d3748; margin-top: 20px; }
                        h3 { font-size: 12pt; color: #4a5568; margin-top: 16px; }
                        h4 { font-size: 11pt; color: #4a5568; }
                        table {
                            border-collapse: collapse;
                            width: 100%%;
                            margin: 12px 0;
                            font-size: 9pt;
                        }
                        th, td {
                            border: 1px solid #cbd5e0;
                            padding: 6px 10px;
                            text-align: left;
                        }
                        th {
                            background-color: #f1f5f9;
                            font-weight: bold;
                        }
                        ul, ol { padding-left: 20px; }
                        li { margin-bottom: 4px; }
                        strong { color: #1a1a2e; }
                        code {
                            background: #f1f5f9;
                            padding: 1px 4px;
                            border-radius: 3px;
                            font-size: 9pt;
                        }
                        hr {
                            border: none;
                            border-top: 1px solid #e2e8f0;
                            margin: 16px 0;
                        }
                    </style>
                </head>
                <body>
                    <h1>%s</h1>
                    %s
                    <div class="content">
                        %s
                    </div>
                </body>
                </html>
                """.formatted(escape(title), metaHtml, bodyHtml);
    }

    private String toXhtml(String html) {
        if (html == null) return "";
        return html
                .replaceAll("<br>", "<br/>")
                .replaceAll("<hr>", "<hr/>")
                .replaceAll("<img([^>]*)(?<!/)>", "<img$1/>");
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
