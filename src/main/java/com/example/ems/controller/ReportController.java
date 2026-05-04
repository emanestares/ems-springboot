package com.example.ems.controller;

import com.example.ems.model.Employee;
import com.example.ems.service.EmployeeService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/report")
@CrossOrigin(origins = "*")
public class ReportController {

    private final EmployeeService employeeService;

    public ReportController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // ── GET /api/report/pdf/by-department?active=true|false ───────────────
    @GetMapping("/pdf/by-department")
    public ResponseEntity<byte[]> generateDepartmentReport(
            @RequestParam(required = false) Boolean active) {
        try {
            List<Employee> filteredEmployees;
            if (active == null) {
                filteredEmployees = employeeService.reportByDepartment();
            } else if (active) {
                filteredEmployees = employeeService.reportByDepartmentActive();
            } else {
                filteredEmployees = employeeService.reportByDepartmentInactive();
            }

            // Group by department name for pie chart
            Map<String, List<Employee>> grouped = filteredEmployees.stream()
                    .collect(Collectors.groupingBy(Employee::getDepartmentName));

            double avgSalary = filteredEmployees.stream()
                    .mapToDouble(Employee::getSalary)
                    .average()
                    .orElse(0.0);
            double avgAge = filteredEmployees.stream()
                    .mapToDouble(e -> calculateAge(e.getBirthday()))
                    .average()
                    .orElse(0.0);

            DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
            grouped.forEach((dept, emps) -> dataset.setValue(dept, emps.size()));

            JFreeChart chart = ChartFactory.createPieChart(
                    "Employees by Department" + (active != null ? (active ? " (Active)" : " (Inactive)") : ""),
                    dataset, true, true, false);
            chart.setBackgroundPaint(java.awt.Color.WHITE);
            PiePlot<?> plot = (PiePlot<?>) chart.getPlot();
            plot.setBackgroundPaint(java.awt.Color.WHITE);
            plot.setOutlineVisible(false);
            plot.setShadowPaint(null);
            plot.setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));

            BufferedImage chartImage = chart.createBufferedImage(520, 360);
            ByteArrayOutputStream chartBaos = new ByteArrayOutputStream();
            ImageIO.write(chartImage, "png", chartBaos);

            ByteArrayOutputStream pdfBaos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(document, pdfBaos);
            document.open();

            Font titleFont     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new BaseColor(30, 64, 175));
            Font subtitleFont  = FontFactory.getFont(FontFactory.HELVETICA,       11, new BaseColor(107, 114, 128));
            Font sectionFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  13, new BaseColor(30, 64, 175));
            Font headerFont    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, BaseColor.WHITE);
            Font cellFont      = FontFactory.getFont(FontFactory.HELVETICA,        9, new BaseColor(55, 65, 81));
            Font statLabelFont = FontFactory.getFont(FontFactory.HELVETICA,        9, new BaseColor(107, 114, 128));
            Font statValFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  13, new BaseColor(17, 24, 39));

            Paragraph title = new Paragraph("Employee Management System", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            String subtitleText = "Department Report" +
                    (active != null ? (active ? " · Active Employees Only" : " · Inactive Employees Only") : "") +
                    " · Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
            Paragraph subtitle = new Paragraph(subtitleText, subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(18);
            document.add(subtitle);

            String statusLabel = active == null ? "All" : (active ? "Active Only" : "Inactive Only");
            PdfPTable statsTable = new PdfPTable(4);
            statsTable.setWidthPercentage(100);
            statsTable.setSpacingAfter(18);
            statsTable.setWidths(new float[]{1f, 1f, 1f, 1f});
            addStatCell(statsTable, String.valueOf(filteredEmployees.size()), "Total Employees", statValFont, statLabelFont);
            addStatCell(statsTable, String.format("₱%,.2f", avgSalary), "Average Salary", statValFont, statLabelFont);
            addStatCell(statsTable, String.format("%.1f yrs", avgAge), "Average Age", statValFont, statLabelFont);
            addStatCell(statsTable, statusLabel, "Status Filter", statValFont, statLabelFont);
            document.add(statsTable);

            Paragraph chartHeading = new Paragraph("Department Distribution", sectionFont);
            chartHeading.setSpacingAfter(8);
            document.add(chartHeading);
            Image chartImg = Image.getInstance(chartBaos.toByteArray());
            chartImg.setAlignment(Element.ALIGN_CENTER);
            chartImg.scaleToFit(480, 340);
            chartImg.setSpacingAfter(20);
            document.add(chartImg);

            Paragraph tableHeading = new Paragraph("Employee Directory (sorted by Department)", sectionFont);
            tableHeading.setSpacingAfter(8);
            document.add(tableHeading);

            PdfPTable empTable = new PdfPTable(5);
            empTable.setWidthPercentage(100);
            empTable.setWidths(new float[]{0.8f, 2.2f, 1.8f, 1.8f, 1.6f});
            empTable.setHeaderRows(1);

            BaseColor headerBg = new BaseColor(30, 64, 175);
            addTableHeader(empTable, "ID",         headerFont, headerBg);
            addTableHeader(empTable, "Full Name",  headerFont, headerBg);
            addTableHeader(empTable, "Department", headerFont, headerBg);
            addTableHeader(empTable, "Birthday",   headerFont, headerBg);
            addTableHeader(empTable, "Salary",     headerFont, headerBg);

            boolean stripe = false;
            BaseColor rowLight = new BaseColor(239, 246, 255);
            DateTimeFormatter df = DateTimeFormatter.ofPattern("MMM d, yyyy");

            for (Employee e : filteredEmployees) {
                BaseColor bg = stripe ? rowLight : BaseColor.WHITE;
                String name = (e.getFirstname() != null ? e.getFirstname() : "") + " " + e.getLastname();
                String bday = e.getBirthday() != null ? e.getBirthday().format(df) : "—";
                String sal  = e.getSalary()   != null ? String.format("₱%,.2f", e.getSalary()) : "—";

                addTableCell(empTable, String.valueOf(e.getId()), cellFont, bg, Element.ALIGN_CENTER);
                addTableCell(empTable, name.trim(),               cellFont, bg, Element.ALIGN_LEFT);
                addTableCell(empTable, e.getDepartmentName(),     cellFont, bg, Element.ALIGN_LEFT);
                addTableCell(empTable, bday,                      cellFont, bg, Element.ALIGN_CENTER);
                addTableCell(empTable, sal,                       cellFont, bg, Element.ALIGN_RIGHT);
                stripe = !stripe;
            }
            document.add(empTable);

            Paragraph footer = new Paragraph("\nEMS Portal · Confidential · " + LocalDate.now().getYear(),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, new BaseColor(156, 163, 175)));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20);
            document.add(footer);
            document.close();

            String filename = "ems-department-report" +
                    (active != null ? (active ? "-active" : "-inactive") : "") +
                    "-" + LocalDate.now() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBaos.toByteArray());

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    // ── GET /api/report/pdf/by-age?active=true|false ──────────────────────
    @GetMapping("/pdf/by-age")
    public ResponseEntity<byte[]> generateAgeReport(
            @RequestParam(required = false) Boolean active) {
        try {
            List<Employee> filteredEmployees;
            if (active == null) {
                filteredEmployees = employeeService.reportByAge();
            } else if (active) {
                filteredEmployees = employeeService.reportByAgeActive();
            } else {
                filteredEmployees = employeeService.reportByAgeInactive();
            }

            double avgAge = filteredEmployees.stream()
                    .mapToDouble(e -> calculateAge(e.getBirthday()))
                    .average()
                    .orElse(0.0);

            DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
            long under25  = filteredEmployees.stream().filter(e -> calculateAge(e.getBirthday()) < 25).count();
            long age25_34 = filteredEmployees.stream().filter(e -> calculateAge(e.getBirthday()) >= 25 && calculateAge(e.getBirthday()) < 35).count();
            long age35_44 = filteredEmployees.stream().filter(e -> calculateAge(e.getBirthday()) >= 35 && calculateAge(e.getBirthday()) < 45).count();
            long over45   = filteredEmployees.stream().filter(e -> calculateAge(e.getBirthday()) >= 45).count();

            if (under25  > 0) dataset.setValue(String.format("Under 25 (%d)", under25),  under25);
            if (age25_34 > 0) dataset.setValue(String.format("25-34 (%d)",    age25_34), age25_34);
            if (age35_44 > 0) dataset.setValue(String.format("35-44 (%d)",    age35_44), age35_44);
            if (over45   > 0) dataset.setValue(String.format("45+ (%d)",      over45),   over45);

            JFreeChart chart = ChartFactory.createPieChart(
                    "Employees by Age Group" + (active != null ? (active ? " (Active)" : " (Inactive)") : ""),
                    dataset, true, true, false);
            chart.setBackgroundPaint(java.awt.Color.WHITE);
            PiePlot<?> plot = (PiePlot<?>) chart.getPlot();
            plot.setBackgroundPaint(java.awt.Color.WHITE);
            plot.setOutlineVisible(false);
            plot.setShadowPaint(null);

            BufferedImage chartImage = chart.createBufferedImage(520, 360);
            ByteArrayOutputStream chartBaos = new ByteArrayOutputStream();
            ImageIO.write(chartImage, "png", chartBaos);

            ByteArrayOutputStream pdfBaos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 60, 50);
            PdfWriter.getInstance(document, pdfBaos);
            document.open();

            Font titleFont     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new BaseColor(30, 64, 175));
            Font subtitleFont  = FontFactory.getFont(FontFactory.HELVETICA,       11, new BaseColor(107, 114, 128));
            Font sectionFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  13, new BaseColor(30, 64, 175));
            Font headerFont    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, BaseColor.WHITE);
            Font cellFont      = FontFactory.getFont(FontFactory.HELVETICA,        9, new BaseColor(55, 65, 81));
            Font statLabelFont = FontFactory.getFont(FontFactory.HELVETICA,        9, new BaseColor(107, 114, 128));
            Font statValFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  13, new BaseColor(17, 24, 39));

            Paragraph title = new Paragraph("Employee Management System", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            String subtitleText = "Age Report" +
                    (active != null ? (active ? " · Active Employees Only" : " · Inactive Employees Only") : "") +
                    " · Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
            Paragraph subtitle = new Paragraph(subtitleText, subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(18);
            document.add(subtitle);

            String statusLabel = active == null ? "All" : (active ? "Active Only" : "Inactive Only");
            PdfPTable statsTable = new PdfPTable(3);
            statsTable.setWidthPercentage(80);
            statsTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            statsTable.setSpacingAfter(18);
            addStatCell(statsTable, String.valueOf(filteredEmployees.size()), "Total Employees", statValFont, statLabelFont);
            addStatCell(statsTable, String.format("%.1f yrs", avgAge), "Average Age", statValFont, statLabelFont);
            addStatCell(statsTable, statusLabel, "Status Filter", statValFont, statLabelFont);
            document.add(statsTable);

            Paragraph chartHeading = new Paragraph("Age Group Distribution", sectionFont);
            chartHeading.setSpacingAfter(8);
            document.add(chartHeading);
            Image chartImg = Image.getInstance(chartBaos.toByteArray());
            chartImg.setAlignment(Element.ALIGN_CENTER);
            chartImg.scaleToFit(480, 340);
            chartImg.setSpacingAfter(20);
            document.add(chartImg);

            Paragraph tableHeading = new Paragraph("Employee Directory (sorted by Age, oldest first)", sectionFont);
            tableHeading.setSpacingAfter(8);
            document.add(tableHeading);

            PdfPTable empTable = new PdfPTable(5);
            empTable.setWidthPercentage(100);
            empTable.setWidths(new float[]{0.8f, 2.2f, 1.8f, 1.5f, 1.6f});
            empTable.setHeaderRows(1);

            BaseColor headerBg = new BaseColor(30, 64, 175);
            addTableHeader(empTable, "ID",         headerFont, headerBg);
            addTableHeader(empTable, "Full Name",  headerFont, headerBg);
            addTableHeader(empTable, "Department", headerFont, headerBg);
            addTableHeader(empTable, "Age",        headerFont, headerBg);
            addTableHeader(empTable, "Salary",     headerFont, headerBg);

            boolean stripe = false;
            BaseColor rowLight = new BaseColor(239, 246, 255);

            for (Employee e : filteredEmployees) {
                BaseColor bg = stripe ? rowLight : BaseColor.WHITE;
                String name   = (e.getFirstname() != null ? e.getFirstname() : "") + " " + e.getLastname();
                int    age    = calculateAge(e.getBirthday());
                String ageStr = age > 0 ? age + " yrs" : "—";
                String sal    = e.getSalary() != null ? String.format("₱%,.2f", e.getSalary()) : "—";

                addTableCell(empTable, String.valueOf(e.getId()), cellFont, bg, Element.ALIGN_CENTER);
                addTableCell(empTable, name.trim(),               cellFont, bg, Element.ALIGN_LEFT);
                addTableCell(empTable, e.getDepartmentName(),     cellFont, bg, Element.ALIGN_LEFT);
                addTableCell(empTable, ageStr,                    cellFont, bg, Element.ALIGN_CENTER);
                addTableCell(empTable, sal,                       cellFont, bg, Element.ALIGN_RIGHT);
                stripe = !stripe;
            }
            document.add(empTable);

            Paragraph footer = new Paragraph("\nEMS Portal · Confidential · " + LocalDate.now().getYear(),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, new BaseColor(156, 163, 175)));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20);
            document.add(footer);
            document.close();

            String filename = "ems-age-report" +
                    (active != null ? (active ? "-active" : "-inactive") : "") +
                    "-" + LocalDate.now() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBaos.toByteArray());

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void addStatCell(PdfPTable table, String value, String label,
                             Font valFont, Font labelFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new BaseColor(219, 234, 254));
        cell.setBackgroundColor(new BaseColor(239, 246, 255));
        cell.setPadding(12);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph val = new Paragraph(value, valFont);
        val.setAlignment(Element.ALIGN_CENTER);
        Paragraph lbl = new Paragraph(label, labelFont);
        lbl.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(val);
        cell.addElement(lbl);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(new BaseColor(30, 58, 138));
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font,
                              BaseColor bg, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        cell.setBorderColor(new BaseColor(219, 234, 254));
        table.addCell(cell);
    }

    private int calculateAge(LocalDate birthday) {
        if (birthday == null) return 0;
        return Period.between(birthday, LocalDate.now()).getYears();
    }
}