package com.meetingroom.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.meetingroom.model.Booking;
import com.meetingroom.model.BookingInvitee;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service("bookingExportService")
public class BookingExportServiceImp implements BookingExportService {

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public byte[] exportToExcel(List<Booking> bookings) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()){
            Sheet sheet = workbook.createSheet("System Bookings");

            // Header style
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Booking ID", "Employee Username", "Employee Email", "Room Name", "Booking Date", "Start Time", "End Time", "Purpose", "Status", "Invitees"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Populate data
            int rowIdx = 1;

            for (Booking booking: bookings){
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(booking.getId());
                row.createCell(1).setCellValue(booking.getUser() != null ? booking.getUser().getUsername() : "N/A");
                row.createCell(2).setCellValue(booking.getUser() != null ? booking.getUser().getEmail() : "N/A");
                row.createCell(3).setCellValue(booking.getRoom() != null ? booking.getRoom().getName() : "N/A");
                row.createCell(4).setCellValue(booking.getBookingDate() != null ? booking.getBookingDate().format(dateFormatter) : "");
                row.createCell(5).setCellValue(booking.getStartTime() != null ? booking.getStartTime().format(timeFormatter) : "");
                row.createCell(6).setCellValue(booking.getEndTime() != null ? booking.getEndTime().format(timeFormatter) : "");
                row.createCell(7).setCellValue(booking.getPurpose());
                row.createCell(8).setCellValue(booking.getStatus());

                // Format invitees : john(Accepted)
                String inviteesStr = "";
                if(booking.getInvitees() != null){
                    inviteesStr = booking.getInvitees().stream()
                            .map(inv -> inv.getUser().getUsername() + "(" + inv.getStatus() + ")")
                            .collect(Collectors.joining(", "));
                }
                row.createCell(9).setCellValue(inviteesStr);
            }

            // Auto size col
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(List<Booking> bookings) throws Exception {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream()){
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Doc title
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Meeting Room Booking System - Bookings Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Table setup
            PdfPTable table = new PdfPTable(new float[]{1f, 2f, 2f, 1.5f, 1.5f, 2.5f, 1.5f, 2.5f});
            table.setWidthPercentage(100f);

            // Table Headers
            Font headFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            String[] headers = {"ID", "User", "Room", "Date", "Time", "Purpose", "Status", "Invitees"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(6);
                table.addCell(cell);
            }

            // Add Table Rows
            Font rowFont = new Font(Font.HELVETICA, 9, Font.NORMAL);
            for (Booking booking : bookings) {
                // Booking ID
                PdfPCell cellId = new PdfPCell(new Phrase(String.valueOf(booking.getId()), rowFont));
                cellId.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cellId);

                // User Username
                table.addCell(new PdfPCell(new Phrase(booking.getUser() != null ? booking.getUser().getUsername() : "N/A", rowFont)));

                // Room
                table.addCell(new PdfPCell(new Phrase(booking.getRoom() != null ? booking.getRoom().getName() : "N/A", rowFont)));

                // Date
                String dateStr = booking.getBookingDate() != null ? booking.getBookingDate().format(dateFormatter) : "";
                table.addCell(new PdfPCell(new Phrase(dateStr, rowFont)));

                // Time Slot
                String timeStr = (booking.getStartTime() != null ? booking.getStartTime().format(timeFormatter) : "")
                        + " - " +
                        (booking.getEndTime() != null ? booking.getEndTime().format(timeFormatter) : "");
                table.addCell(new PdfPCell(new Phrase(timeStr, rowFont)));

                // Purpose
                table.addCell(new PdfPCell(new Phrase(booking.getPurpose(), rowFont)));

                // Status
                table.addCell(new PdfPCell(new Phrase(booking.getStatus(), rowFont)));

                // Invitees
                String inviteesStr = "";
                if (booking.getInvitees() != null) {
                    inviteesStr = booking.getInvitees().stream()
                            .map(inv -> inv.getUser().getUsername() + "(" + inv.getStatus() + ")")
                            .collect(Collectors.joining(", "));
                }
                table.addCell(new PdfPCell(new Phrase(inviteesStr, rowFont)));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToXml(List<Booking> bookings) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        org.w3c.dom.Document doc = dBuilder.newDocument();

        // Root element
        org.w3c.dom.Element rootElement = doc.createElement("bookings");
        doc.appendChild(rootElement);

        for (Booking booking : bookings) {
            org.w3c.dom.Element bookingElement = doc.createElement("booking");
            bookingElement.setAttribute("id", String.valueOf(booking.getId()));

            // User Info
            org.w3c.dom.Element userNode = doc.createElement("user");
            org.w3c.dom.Element usernameNode = doc.createElement("username");
            usernameNode.setTextContent(booking.getUser() != null ? booking.getUser().getUsername() : "N/A");
            org.w3c.dom.Element emailNode = doc.createElement("email");
            emailNode.setTextContent(booking.getUser() != null ? booking.getUser().getEmail() : "N/A");
            userNode.appendChild(usernameNode);
            userNode.appendChild(emailNode);
            bookingElement.appendChild(userNode);

            // Room Info
            org.w3c.dom.Element roomNode = doc.createElement("room");
            roomNode.setTextContent(booking.getRoom() != null ? booking.getRoom().getName() : "N/A");
            bookingElement.appendChild(roomNode);

            // Schedule Info
            org.w3c.dom.Element dateNode = doc.createElement("date");
            dateNode.setTextContent(booking.getBookingDate() != null ? booking.getBookingDate().format(dateFormatter) : "");
            bookingElement.appendChild(dateNode);

            org.w3c.dom.Element startNode = doc.createElement("startTime");
            startNode.setTextContent(booking.getStartTime() != null ? booking.getStartTime().format(timeFormatter) : "");
            bookingElement.appendChild(startNode);

            org.w3c.dom.Element endNode = doc.createElement("endTime");
            endNode.setTextContent(booking.getEndTime() != null ? booking.getEndTime().format(timeFormatter) : "");
            bookingElement.appendChild(endNode);

            // Purpose & Status
            org.w3c.dom.Element purposeNode = doc.createElement("purpose");
            purposeNode.setTextContent(booking.getPurpose());
            bookingElement.appendChild(purposeNode);

            org.w3c.dom.Element statusNode = doc.createElement("status");
            statusNode.setTextContent(booking.getStatus());
            bookingElement.appendChild(statusNode);

            // Remarks
            org.w3c.dom.Element remarksNode = doc.createElement("remarks");
            remarksNode.setTextContent(booking.getAdminRemarks() != null ? booking.getAdminRemarks() : "");
            bookingElement.appendChild(remarksNode);

            // Invitees Node
            org.w3c.dom.Element inviteesNode = doc.createElement("invitees");
            if (booking.getInvitees() != null) {
                for (BookingInvitee invitee : booking.getInvitees()) {
                    org.w3c.dom.Element inviteeNode = doc.createElement("invitee");
                    inviteeNode.setAttribute("username", invitee.getUser().getUsername());
                    inviteeNode.setAttribute("status", invitee.getStatus());
                    inviteesNode.appendChild(inviteeNode);
                }
            }
            bookingElement.appendChild(inviteesNode);

            rootElement.appendChild(bookingElement);
        }

        // Transform DOM to byte array
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(out);
        transformer.transform(source, result);

        return out.toByteArray();
    }
}
