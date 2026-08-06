package com.sirithree.shopops.admin.tool.executor;

import com.sirithree.shopops.admin.tool.domain.ToolInvokeContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

@Component
public class ReportExportExcelExecutor extends PortfolioOperationToolExecutor {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final List<String> SHEETS = List.of("Daily Review", "Risk Comments", "Product Candidates", "Ad Campaigns");

    @Override
    public String toolCode() {
        return "report.export_excel";
    }

    @Override
    protected Map<String, Object> output(ToolInvokeContext context, Map<String, Object> input) {
        long startedAt = System.nanoTime();
        Map<String, Object> data = base(context, input);
        long reportId = number(input.get("reportId"), 0L);
        String exportId = "XLSX-" + LocalDateTime.now().format(FILE_TIME);
        String fileName = "shopops-operation-report-%s.xlsx".formatted(exportId);
        Path outputPath = Path.of("target", "shopops-exports", fileName).toAbsolutePath().normalize();

        writeWorkbook(outputPath, context, input, reportId, exportId);

        data.put("exportId", exportId);
        data.put("fileName", fileName);
        data.put("filePath", outputPath.toString());
        data.put("fileSizeBytes", size(outputPath));
        data.put("sheets", SHEETS);
        data.put("sheetCount", SHEETS.size());
        data.put("status", "EXPORTED");
        data.put("mode", "local-xlsx");
        data.put("durationMs", Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
        return data;
    }

    private void writeWorkbook(Path outputPath, ToolInvokeContext context, Map<String, Object> input,
                               long reportId, String exportId) {
        try {
            Files.createDirectories(outputPath.getParent());
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(outputPath))) {
                entry(zip, "[Content_Types].xml", contentTypes());
                entry(zip, "_rels/.rels", rels());
                entry(zip, "xl/workbook.xml", workbook());
                entry(zip, "xl/_rels/workbook.xml.rels", workbookRels());
                entry(zip, "xl/styles.xml", styles());
                entry(zip, "xl/worksheets/sheet1.xml", sheet(List.of(
                        List.of("Metric", "Value"),
                        List.of("Report ID", String.valueOf(reportId)),
                        List.of("Tenant ID", String.valueOf(context.getTenantId())),
                        List.of("Shop ID", String.valueOf(context.getShopId())),
                        List.of("Export ID", exportId),
                        List.of("Generated At", LocalDateTime.now().toString())
                )));
                entry(zip, "xl/worksheets/sheet2.xml", sheet(List.of(
                        List.of("Risk Type", "Count", "Action"),
                        List.of("Low-score comments", value(input, "negativeCommentCount", "51"), "Review and reply"),
                        List.of("Refund proxy", value(input, "refundProxyRate", "7.63%"), "Check after-sales risk")
                )));
                entry(zip, "xl/worksheets/sheet3.xml", sheet(List.of(
                        List.of("Product", "Risk Score", "Suggestion"),
                        List.of(value(input, "topProduct", "Furniture Bedroom / 4f18ca98"), "80.0", "Optimize title and description"),
                        List.of("Candidate count", value(input, "productCandidateCount", "10"), "Prioritize P0 items")
                )));
                entry(zip, "xl/worksheets/sheet4.xml", sheet(List.of(
                        List.of("Campaign", "Spend", "ROI", "Suggestion"),
                        List.of(value(input, "campaignId", "CRITEO-RISK-SAMPLE"), value(input, "spend", "8.50"), value(input, "roi", "low-conversion"), "Review budget"),
                        List.of("Source", "Criteo Attribution", "public baseline", "Anonymized campaign IDs")
                )));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to export xlsx report: " + outputPath, ex);
        }
    }

    private void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String contentTypes() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
                """;
    }

    private String rels() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """;
    }

    private String workbook() {
        StringBuilder builder = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                """);
        for (int i = 0; i < SHEETS.size(); i++) {
            builder.append("    <sheet name=\"").append(xml(SHEETS.get(i))).append("\" sheetId=\"")
                    .append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>\n");
        }
        builder.append("  </sheets>\n</workbook>\n");
        return builder.toString();
    }

    private String workbookRels() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
                  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
                  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
                  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                </Relationships>
                """;
    }

    private String styles() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
                  <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
                  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
                </styleSheet>
                """;
    }

    private String sheet(List<List<String>> rows) {
        StringBuilder builder = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                """);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            builder.append("    <row r=\"").append(rowIndex + 1).append("\">\n");
            List<String> row = rows.get(rowIndex);
            for (int colIndex = 0; colIndex < row.size(); colIndex++) {
                builder.append("      <c r=\"").append(cellRef(rowIndex, colIndex))
                        .append("\" t=\"inlineStr\"><is><t>")
                        .append(xml(row.get(colIndex)))
                        .append("</t></is></c>\n");
            }
            builder.append("    </row>\n");
        }
        builder.append("  </sheetData>\n</worksheet>\n");
        return builder.toString();
    }

    private String cellRef(int rowIndex, int colIndex) {
        return String.valueOf((char) ('A' + colIndex)) + (rowIndex + 1);
    }

    private String xml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String value(Map<String, Object> input, String key, String defaultValue) {
        Object value = input.get(key);
        return value == null || String.valueOf(value).isBlank() ? defaultValue : String.valueOf(value);
    }

    private long number(Object value, long defaultValue) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private long size(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read xlsx file size: " + path, ex);
        }
    }
}
