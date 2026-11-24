package com.thapar.gpf.backend.service;

import com.thapar.gpf.backend.dto.SubjectStatDTO;
import com.thapar.gpf.backend.exception.BadRequestException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class StatisticsExportService {
    public record Download(byte[] bytes, String contentType, String extension) { }
    private final AnalyticsService analytics;

    public StatisticsExportService(AnalyticsService analytics) { this.analytics = analytics; }

    public Download export(String format) {
        try {
            return switch (format.toLowerCase(Locale.ROOT)) {
                case "csv" -> new Download(analytics.buildStatisticsCsv().getBytes(StandardCharsets.UTF_8), "text/csv", "csv");
                case "excel", "xlsx" -> new Download(excel(analytics.buildReport().subjectStats()),
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");
                case "pdf" -> new Download(pdf(analytics.buildReport().subjectStats()), "application/pdf", "pdf");
                default -> throw new BadRequestException("Choose CSV, Excel or PDF.");
            };
        } catch (IOException e) {
            throw new IllegalStateException("Could not generate the statistics report", e);
        }
    }

    private byte[] excel(List<SubjectStatDTO> statistics) throws IOException {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Subject statistics");
            String[] headers = {"Code", "Subject", "Responses", "Average", "Median", "Mode", "Highest", "Lowest",
                    "Standard deviation", "Pass %", "Fail %", "A", "A-", "B", "B-", "C", "D", "E"};
            Font font = workbook.createFont();
            font.setBold(true);
            CellStyle heading = workbook.createCellStyle();
            heading.setFont(font);
            Row first = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                first.createCell(i).setCellValue(headers[i]);
                first.getCell(i).setCellStyle(heading);
                sheet.setColumnWidth(i, i == 1 ? 14000 : 4500);
            }
            int index = 1;
            for (var s : statistics) {
                Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(s.code());
                row.createCell(1).setCellValue(s.name());
                row.createCell(2).setCellValue(s.totalResponses());
                if (s.hidden()) continue;
                double[] numbers = {s.average(), s.median(), s.mode(), s.highest(), s.lowest(), s.standardDeviation(),
                        s.passPercentage(), s.failurePercentage(), s.gradeA(), s.gradeAMinus(), s.gradeB(),
                        s.gradeBMinus(), s.gradeC(), s.gradeD(), s.gradeE()};
                for (int i = 0; i < numbers.length; i++) row.createCell(i + 3).setCellValue(numbers[i]);
            }
            sheet.createFreezePane(2, 1);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private byte[] pdf(List<SubjectStatDTO> statistics) throws IOException {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            int pageSize = 34;
            for (int start = 0; start < Math.max(1, statistics.size()); start += pageSize) {
                var page = new PDPage();
                document.addPage(page);
                try (var content = new PDPageContentStream(document, page)) {
                    text(content, font, 17, 40, 750, "Thapar Grade Predictor");
                    text(content, font, 11, 40, 728, "Subject statistics report");
                    text(content, font, 8, 40, 708, "Aggregates only. Statistics are hidden below five responses. Predictions are estimates.");
                    text(content, font, 8, 40, 680, "Code");
                    text(content, font, 8, 138, 680, "Subject");
                    text(content, font, 8, 340, 680, "Responses");
                    text(content, font, 8, 405, 680, "Average");
                    text(content, font, 8, 465, 680, "Median");
                    text(content, font, 8, 525, 680, "Pass %");
                    for (int i = start; i < Math.min(start + pageSize, statistics.size()); i++) {
                        var s = statistics.get(i);
                        float y = 660 - (i - start) * 17;
                        text(content, font, 7, 40, y, shorten(s.code(), 20));
                        text(content, font, 7, 138, y, shorten(s.name(), 38));
                        text(content, font, 8, 340, y, Integer.toString(s.totalResponses()));
                        text(content, font, 8, 405, y, s.hidden() ? "-" : number(s.average()));
                        text(content, font, 8, 465, y, s.hidden() ? "-" : number(s.median()));
                        text(content, font, 8, 525, y, s.hidden() ? "-" : number(s.passPercentage()));
                    }
                    text(content, font, 8, 40, 42, "Page " + (start / pageSize + 1) + " | Individual student marks are excluded.");
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private static String number(double value) { return String.format(Locale.ROOT, "%.2f", value); }
    private static String shorten(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
    private static void text(PDPageContentStream content, PDType1Font font, int size,
                             float x, float y, String value) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(value.replaceAll("[^\\x20-\\x7E]", "?"));
        content.endText();
    }
}
