package com.weekly.service;

import com.weekly.entity.Report;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd");
    private static final String[] HEADERS = {"번호", "작성자", "팀", "업무기간", "작성일", "주간보고", "AI", "Teams"};

    public byte[] exportReports(List<Report> reports) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("주간보고");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Wrap text style for content columns
            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            // Data rows
            int rowIdx = 1;
            for (Report r : reports) {
                Row row = sheet.createRow(rowIdx);
                row.createCell(0).setCellValue(rowIdx);
                row.createCell(1).setCellValue(r.getUser().getName());
                row.createCell(2).setCellValue(r.getUser().getTeam() != null ? r.getUser().getTeam().getName() : "");
                row.createCell(3).setCellValue(r.getPeriodStart() != null && r.getPeriodEnd() != null
                        ? r.getPeriodStart().format(DATE_FMT) + "~" + r.getPeriodEnd().format(DATE_FMT) : "");
                row.createCell(4).setCellValue(r.getCreatedAt().format(FMT));

                Cell generalCell = row.createCell(5);
                generalCell.setCellValue(truncate(r.getGeneralReportMd(), 32000));
                generalCell.setCellStyle(wrapStyle);

                row.createCell(6).setCellValue(r.getLlmProvider() != null ? r.getLlmProvider() : "");
                row.createCell(7).setCellValue(r.isTeamsSent() ? "전송됨" : "미전송");
                rowIdx++;
            }

            // Auto-size non-content columns
            for (int i = 0; i < HEADERS.length; i++) {
                if (i != 5) {
                    sheet.autoSizeColumn(i);
                }
            }
            sheet.setColumnWidth(5, 20000);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}
