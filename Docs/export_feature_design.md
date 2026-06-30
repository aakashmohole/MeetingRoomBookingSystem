# Admin & Employee Booking Export Feature - Technical Design Document

This document outlines the architecture, database additions, service logic, and ZK presentation layers required to develop an export utility for both Administrators (who can export all system bookings with filters/ranges) and Employees (who can export their personal reservations with filters/ranges), producing outputs in **Excel (.xlsx)**, **PDF (.pdf)**, or **XML (.xml)** formats.

---

## 1. User Interaction Flow (Admin Panel)
1. **Trigger**: Admin navigates to "System Bookings" (`admin_bookings.zul`) and clicks the new **Export Bookings** button.
2. **Options Dialog**: A modal window opens containing:
   - **Scope Selector** (Radio Buttons):
     - *Download All Bookings* (exports all bookings in the system)
     - *Export Current Filtered List* (exports only the bookings currently matching search keywords, status, or date filters applied on the screen)
     - *Choose Date Range* (renders two Datebox pickers: "From Date" and "To Date")
   - **Format Selector** (Dropdown):
     - *Excel Spreadsheet (.xlsx)*
     - *Adobe PDF (.pdf)*
     - *XML Document (.xml)*
   - **Action Buttons**: *Cancel* and *Download*.
3. **Execution**: Clicking *Download* triggers validation, resolves records (using database queries for date-range or system-wide bounds, or using view-level filters), formats data through the export service, and triggers a file download in the user's browser using ZK's `Filedownload` utility.

---

## 2. Dependencies
To support Excel (using Apache POI), PDF generation (using OpenPDF), and XML validation, verify these dependencies are configured in [pom.xml](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/pom.xml):

```xml
<!-- Maven Dependencies in pom.xml -->

<!-- Apache POI for Excel (.xlsx) generation -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- Force modern commons-io version for POI compatibility -->
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.15.1</version>
</dependency>

<!-- OpenPDF for PDF generation (LGPL/MPL licensed, replacement for iText) -->
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>1.3.39</version>
</dependency>
```

---

## 3. Data Access Layer (DAO)

Extend the DAO layer to allow retrieving bookings within a specified date range.

### 3.1. [BookingDao.java](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/java/com/meetingroom/dao/BookingDao.java) Modifications
Add the following method declaration to filter bookings by date range:

```java
// Add to BookingDao interface:
java.util.List<Booking> findByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate);
```

### 3.2. [BookingDaoImpl.java](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/java/com/meetingroom/dao/BookingDaoImpl.java) Implementation
Implement the method using Hibernate HQL:

```java
@Override
public List<Booking> findByDateRange(LocalDate startDate, LocalDate endDate) {
    return sessionFactory.getCurrentSession()
            .createQuery("from Booking where bookingDate between :startDate and :endDate order by bookingDate desc, startTime desc", Booking.class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .list();
}
```

---

## 4. Service Layer

Create a dedicated `BookingExportService` to handle format-specific conversions and expose methods to fetch date-range bookings in `BookingService`.

### 4.1. [BookingService.java](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/java/com/meetingroom/service/BookingService.java) Extensions
Add the interface method:

```java
// Add to BookingService interface:
List<Booking> getBookingsByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate);
```

### 4.2. [BookingServiceImpl.java](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/java/com/meetingroom/service/BookingServiceImpl.java) Updates
Implement the delegate method to the DAO:

```java
@Override
public List<Booking> getBookingsByDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
    log.info("Fetching all bookings between {} and {}", startDate, endDate);
    return bookingDao.findByDateRange(startDate, endDate);
}
```

### 4.3. [BookingExportService.java](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/java/com/meetingroom/service/BookingExportService.java)
Define methods to transform a list of `Booking` entities into file byte buffers.

```java
package com.meetingroom.service;

import com.meetingroom.model.Booking;
import java.util.List;

public interface BookingExportService {
    byte[] exportToExcel(List<Booking> bookings) throws Exception;
    byte[] exportToPdf(List<Booking> bookings) throws Exception;
    byte[] exportToXml(List<Booking> bookings) throws Exception;
}
```

### 4.4. [BookingExportServiceImp.java](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/java/com/meetingroom/service/BookingExportServiceImp.java)
This implementation manages the logic for generating Excel tables, PDF documents, and XML trees.

```java
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
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("System Bookings");

            // Header Style
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Booking ID", "Employee Username", "Employee Email", "Room Name", "Booking Date", "Start Time", "End Time", "Purpose", "Status", "Invitees"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Populate Data
            int rowIdx = 1;
            for (Booking booking : bookings) {
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
                
                // Format invitees: e.g. "john(ACCEPTED), alice(PENDING)"
                String inviteesStr = "";
                if (booking.getInvitees() != null) {
                    inviteesStr = booking.getInvitees().stream()
                            .map(inv -> inv.getUser().getUsername() + "(" + inv.getStatus() + ")")
                            .collect(Collectors.joining(", "));
                }
                row.createCell(9).setCellValue(inviteesStr);
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] exportToPdf(List<Booking> bookings) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Document Title
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Meeting Room Booking System - Bookings Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Table Setup (8 Columns)
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
```

---

## 5. ViewModel Presentation Layer (ZK Admin)

Update `AdminBookingsViewModel.java` to support exporting all system bookings, the filtered list, or date-range lists.

### 5.1. Add Export-Related Fields and DI
Wire the `BookingExportService` dependency and declare fields in [AdminBookingsViewModel.java](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/java/com/meetingroom/viewmodel/AdminBookingsViewModel.java):

```java
// Inject Export Service
@WireVariable
private BookingExportService bookingExportService;

// Dialog controls
private boolean exportDialogOpen = false;
private String exportScope = "ALL"; // Options: "ALL", "FILTERED", "RANGE"
private Date exportStartDate;
private Date exportEndDate;
private String exportFormat = "EXCEL"; // Options: "EXCEL", "PDF", "XML"

// Getters and Setters
public boolean isExportDialogOpen() { return exportDialogOpen; }
public void setExportDialogOpen(boolean exportDialogOpen) { this.exportDialogOpen = exportDialogOpen; }

public String getExportScope() { return exportScope; }
public void setExportScope(String exportScope) { this.exportScope = exportScope; }

public Date getExportStartDate() { return exportStartDate; }
public void setExportStartDate(Date exportStartDate) { this.exportStartDate = exportStartDate; }

public Date getExportEndDate() { return exportEndDate; }
public void setExportEndDate(Date exportEndDate) { this.exportEndDate = exportEndDate; }

public String getExportFormat() { return exportFormat; }
public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
```

### 5.2. Add Commands
Add commands to open the dialog, close it, and perform export logic:

```java
@Command
@NotifyChange("exportDialogOpen")
public void openExportDialog() {
    this.exportScope = "ALL";
    this.exportStartDate = null;
    this.exportEndDate = null;
    this.exportFormat = "EXCEL";
    this.exportDialogOpen = true;
}

@Command
@NotifyChange("exportDialogOpen")
public void closeExportDialog() {
    this.exportDialogOpen = false;
}

@Command
@NotifyChange({"exportDialogOpen", "exportScope"})
public void changeExportScope() {
    // Triggers UI visibility changes
}

@Command
@NotifyChange("exportDialogOpen")
public void performExport() {
    List<Booking> bookingsToExport;

    switch (exportScope) {
        case "FILTERED":
            // Export the current filtered list shown on the screen
            bookingsToExport = new ArrayList<>(filteredBookings);
            break;

        case "RANGE":
            if (exportStartDate == null || exportEndDate == null) {
                ToastUtil.error("Please select both Start and End Dates for exporting.");
                return;
            }
            if (exportStartDate.after(exportEndDate)) {
                ToastUtil.error("Start Date cannot be after End Date.");
                return;
            }

            // Convert Date to LocalDate
            LocalDate start = exportStartDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate end = exportEndDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            bookingsToExport = bookingService.getBookingsByDateRange(start, end);
            break;

        case "ALL":
        default:
            // Export all bookings in the system
            bookingsToExport = bookingService.getAllBookings();
            break;
    }

    if (bookingsToExport.isEmpty()) {
        ToastUtil.error("No bookings found in the selected range to export.");
        return;
    }

    try {
        byte[] fileData = null;
        String fileName = "bookings_export_" + System.currentTimeMillis();
        String contentType = "";

        switch (exportFormat) {
            case "EXCEL":
                fileData = bookingExportService.exportToExcel(bookingsToExport);
                fileName += ".xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                break;
            case "PDF":
                fileData = bookingExportService.exportToPdf(bookingsToExport);
                fileName += ".pdf";
                contentType = "application/pdf";
                break;
            case "XML":
                fileData = bookingExportService.exportToXml(bookingsToExport);
                fileName += ".xml";
                contentType = "text/xml";
                break;
            default:
                ToastUtil.error("Invalid export format.");
                return;
        }

        if (fileData != null) {
            org.zkoss.zul.Filedownload.save(fileData, contentType, fileName);
            this.exportDialogOpen = false;
            ToastUtil.success("Export successful! Download started.");
        }
    } catch (Exception e) {
        log.error("Error occurred while exporting bookings", e);
        ToastUtil.error("An error occurred during export: " + e.getMessage());
    }
}
```

---

## 6. View Layer (ZK Admin ZUL UI)

Integrate the button and the dialog in [admin_bookings.zul](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/webapp/admin_bookings.zul).

### 6.1. Add Export Button
Modify line 285 in `admin_bookings.zul` to trigger the dialog:

```xml
<!-- Replace this placeholder -->
<button label="Export" sclass="btn-reset" onClick="" />

<!-- With the active export command -->
<button label="Export Bookings" 
        style="background: #2563eb; color: white; border: none; border-radius: 8px; padding: 10px 20px; font-weight: 600; cursor: pointer; height: 38px; transition: all 0.2s ease;" 
        onClick="@command('openExportDialog')" />
```

### 6.2. Add Modal Dialog for Options
Add this `<window>` at the bottom of the main `<div class="main-content">` layer:

```xml
<!-- Modal Dialog for Booking Export Option -->
<window title="Export Bookings" mode="modal" border="normal" width="460px"
        visible="@load(vm.exportDialogOpen)" onClose="@command('closeExportDialog')"
        style="border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.1);">
    
    <vlayout spacing="18px" style="padding: 20px;">
        <h:p style="color: #64748b; font-size: 0.9rem; margin: 0 0 10px 0;">
            Select export range settings and format options to generate your report.
        </h:p>

        <!-- Scope Choice -->
        <vlayout spacing="6px">
            <label value="Export Scope:" style="font-weight: 600; color: #334155; font-size: 0.9rem;" />
            <radiogroup id="exportRange" selectedItem="@bind(vm.exportScope)">
                <vlayout spacing="10px">
                    <radio label="Download All Bookings" value="ALL" onCheck="@command('changeExportScope')" />
                    <radio label="Export Current Filtered List" value="FILTERED" onCheck="@command('changeExportScope')" />
                    <radio label="Filter by Custom Date Range" value="RANGE" onCheck="@command('changeExportScope')" />
                </vlayout>
            </radiogroup>
        </vlayout>

        <!-- Conditional Date Range Input -->
        <vlayout spacing="8px" visible="@load(vm.exportScope eq 'RANGE')" 
                 style="background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 8px; padding: 12px; margin-top: 5px;">
            <hlayout spacing="12px" style="align-items: center; justify-content: space-between;">
                <label value="From Date:" style="font-size: 0.85rem; color: #475569; font-weight: 500;" />
                <datebox value="@bind(vm.exportStartDate)" format="yyyy-MM-dd" width="180px" />
            </hlayout>
            <hlayout spacing="12px" style="align-items: center; justify-content: space-between; margin-top: 6px;">
                <label value="To Date:" style="font-size: 0.85rem; color: #475569; font-weight: 500;" />
                <datebox value="@bind(vm.exportEndDate)" format="yyyy-MM-dd" width="180px" />
            </hlayout>
        </vlayout>

        <!-- Format Selector -->
        <vlayout spacing="6px">
            <label value="Export Format:" style="font-weight: 600; color: #334155; font-size: 0.9rem;" />
            <listbox mold="select" selectedItem="@bind(vm.exportFormat)" 
                     style="width: 100%; border: 1px solid #cbd5e1; border-radius: 8px; padding: 8px; font-size: 0.9rem;">
                <listitem label="Excel Spreadsheet (.xlsx)" value="EXCEL" />
                <listitem label="Adobe PDF (.pdf)" value="PDF" />
                <listitem label="XML Document (.xml)" value="XML" />
            </listbox>
        </vlayout>

        <!-- Actions -->
        <hlayout style="width: 100%; justify-content: flex-end; margin-top: 10px;" spacing="12px">
            <button label="Cancel" onClick="@command('closeExportDialog')"
                    style="background: #f1f5f9; border: 1px solid #cbd5e1; color: #475569; border-radius: 8px; padding: 10px 20px; cursor: pointer; font-weight: 600; font-size: 0.85rem;" />
            <button label="Download" onClick="@command('performExport')"
                    style="background: #2563eb; border: none; color: white; border-radius: 8px; padding: 10px 20px; cursor: pointer; font-weight: 600; font-size: 0.85rem;" />
        </hlayout>
    </vlayout>
</window>
```

---

## 7. Employee/User Booking Export Option

This section details how to enable the same Excel, PDF, and XML export features for regular employees under their personal booking screen (`my_bookings.zul`).

### 7.1. User Interaction Flow
1. **Trigger**: Employee navigates to "My Reservations Hub" (`my_bookings.zul`). They can optionally apply search/filter criteria.
2. **Action**: Employee clicks the **Export My Bookings** button in the search panel.
3. **Options Dialog**: A modal window opens containing:
   - **Export Scope Selector** (Radio Group):
     - *Export All My Bookings* (exports the complete list of bookings they organized)
     - *Export Current Filtered List* (exports only the subset matching active search text, date, or status)
     - *Filter by Custom Date Range* (renders "From Date" and "To Date" datebox pickers)
   - **Format Selector** (Dropdown):
     - *Excel Spreadsheet (.xlsx)*
     - *Adobe PDF (.pdf)*
     - *XML Document (.xml)*
   - **Action Buttons**: *Cancel* and *Download*.
4. **Execution**: Clicking *Download* resolves the list of bookings (matching scope & filtered constraints), invokes `BookingExportService`, and triggers the browser download.

### 7.2. ViewModel Layer Changes ([MyBookingsViewModel.java](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/java/com/meetingroom/viewmodel/MyBookingsViewModel.java))

Declare fields and commands inside `MyBookingsViewModel.java` to support this logic:

```java
// Inject Export Service
@WireVariable
private BookingExportService bookingExportService;

// Dialog controls
private boolean exportDialogOpen = false;
private String exportScope = "ALL"; // Options: "ALL", "FILTERED", "RANGE"
private Date exportStartDate;
private Date exportEndDate;
private String exportFormat = "EXCEL"; // Options: "EXCEL", "PDF", "XML"

// Getters and Setters
public boolean isExportDialogOpen() { return exportDialogOpen; }
public void setExportDialogOpen(boolean exportDialogOpen) { this.exportDialogOpen = exportDialogOpen; }

public String getExportScope() { return exportScope; }
public void setExportScope(String exportScope) { this.exportScope = exportScope; }

public Date getExportStartDate() { return exportStartDate; }
public void setExportStartDate(Date exportStartDate) { this.exportStartDate = exportStartDate; }

public Date getExportEndDate() { return exportEndDate; }
public void setExportEndDate(Date exportEndDate) { this.exportEndDate = exportEndDate; }

public String getExportFormat() { return exportFormat; }
public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
```

Add commands:

```java
@Command
@NotifyChange("exportDialogOpen")
public void openExportDialog() {
    this.exportScope = "ALL";
    this.exportStartDate = null;
    this.exportEndDate = null;
    this.exportFormat = "EXCEL";
    this.exportDialogOpen = true;
}

@Command
@NotifyChange("exportDialogOpen")
public void closeExportDialog() {
    this.exportDialogOpen = false;
}

@Command
@NotifyChange({"exportDialogOpen", "exportScope"})
public void changeExportScope() {
    // Triggers UI updates
}

@Command
@NotifyChange("exportDialogOpen")
public void performExport() {
    List<Booking> bookingsToExport;

    switch (exportScope) {
        case "FILTERED":
            // Export the current filtered list shown on screen
            bookingsToExport = new ArrayList<>(filteredBookings);
            break;
            
        case "RANGE":
            if (exportStartDate == null || exportEndDate == null) {
                ToastUtil.error("Please select both Start and End Dates.");
                return;
            }
            if (exportStartDate.after(exportEndDate)) {
                ToastUtil.error("Start Date cannot be after End Date.");
                return;
            }
            LocalDate start = exportStartDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate end = exportEndDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            
            // Filter allBookings in-memory for security and efficiency
            bookingsToExport = allBookings.stream()
                    .filter(b -> !b.getBookingDate().isBefore(start) && !b.getBookingDate().isAfter(end))
                    .collect(Collectors.toList());
            break;
            
        case "ALL":
        default:
            // Export all user's bookings
            bookingsToExport = new ArrayList<>(allBookings);
            break;
    }

    if (bookingsToExport.isEmpty()) {
        ToastUtil.error("No bookings found in the selected range to export.");
        return;
    }

    try {
        byte[] fileData = null;
        String fileName = "my_bookings_" + System.currentTimeMillis();
        String contentType = "";

        switch (exportFormat) {
            case "EXCEL":
                fileData = bookingExportService.exportToExcel(bookingsToExport);
                fileName += ".xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                break;
            case "PDF":
                fileData = bookingExportService.exportToPdf(bookingsToExport);
                fileName += ".pdf";
                contentType = "application/pdf";
                break;
            case "XML":
                fileData = bookingExportService.exportToXml(bookingsToExport);
                fileName += ".xml";
                contentType = "text/xml";
                break;
            default:
                ToastUtil.error("Invalid export format.");
                return;
        }

        if (fileData != null) {
            org.zkoss.zul.Filedownload.save(fileData, contentType, fileName);
            this.exportDialogOpen = false;
            ToastUtil.success("Export successful! Download started.");
        }
    } catch (Exception e) {
        log.error("Error occurred while exporting user bookings", e);
        ToastUtil.error("An error occurred during export: " + e.getMessage());
    }
}
```

### 7.3. UI Layer Changes ([my_bookings.zul](file:///Users/apple/Aakash/Spring/MeetingRoomBookingSystem/src/main/webapp/my_bookings.zul))

In the search panel filter row, append the export button:

```xml
<!-- In the <div class="filter-row"> panel -->
<button label="Export My Bookings" 
        style="background: #059669; color: white; border: none; border-radius: 8px; padding: 10px 20px; font-weight: 600; cursor: pointer; height: 42px; transition: all 0.2s ease;" 
        onClick="@command('openExportDialog')" />
```

Append the export modal window at the bottom of the main `<div class="main-content">` layout block:

```xml
<!-- Modal Dialog for Employee Booking Export Option -->
<window title="Export My Reservations" mode="modal" border="normal" width="460px"
        visible="@load(vm.exportDialogOpen)" onClose="@command('closeExportDialog')"
        style="border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.1);">
    
    <vlayout spacing="18px" style="padding: 20px;">
        <h:p style="color: #64748b; font-size: 0.9rem; margin: 0 0 10px 0;">
            Select which reservations you want to download and choose your preferred file format.
        </h:p>

        <!-- Scope Choice -->
        <vlayout spacing="6px">
            <label value="Export Scope:" style="font-weight: 600; color: #334155; font-size: 0.9rem;" />
            <radiogroup id="exportRange" selectedItem="@bind(vm.exportScope)">
                <vlayout spacing="10px">
                    <radio label="Export All My Bookings" value="ALL" onCheck="@command('changeExportScope')" />
                    <radio label="Export Current Filtered List" value="FILTERED" onCheck="@command('changeExportScope')" />
                    <radio label="Filter by Custom Date Range" value="RANGE" onCheck="@command('changeExportScope')" />
                </vlayout>
            </radiogroup>
        </vlayout>

        <!-- Conditional Date Range Input -->
        <vlayout spacing="8px" visible="@load(vm.exportScope eq 'RANGE')" 
                 style="background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 8px; padding: 12px; margin-top: 5px;">
            <hlayout spacing="12px" style="align-items: center; justify-content: space-between;">
                <label value="From Date:" style="font-size: 0.85rem; color: #475569; font-weight: 500;" />
                <datebox value="@bind(vm.exportStartDate)" format="yyyy-MM-dd" width="180px" />
            </hlayout>
            <hlayout spacing="12px" style="align-items: center; justify-content: space-between; margin-top: 6px;">
                <label value="To Date:" style="font-size: 0.85rem; color: #475569; font-weight: 500;" />
                <datebox value="@bind(vm.exportEndDate)" format="yyyy-MM-dd" width="180px" />
            </hlayout>
        </vlayout>

        <!-- Format Selector -->
        <vlayout spacing="6px">
            <label value="Export Format:" style="font-weight: 600; color: #334155; font-size: 0.9rem;" />
            <listbox mold="select" selectedItem="@bind(vm.exportFormat)" 
                     style="width: 100%; border: 1px solid #cbd5e1; border-radius: 8px; padding: 8px; font-size: 0.9rem;">
                <listitem label="Excel Spreadsheet (.xlsx)" value="EXCEL" />
                <listitem label="Adobe PDF (.pdf)" value="PDF" />
                <listitem label="XML Document (.xml)" value="XML" />
            </listbox>
        </vlayout>

        <!-- Actions -->
        <hlayout style="width: 100%; justify-content: flex-end; margin-top: 10px;" spacing="12px">
            <button label="Cancel" onClick="@command('closeExportDialog')"
                    style="background: #f1f5f9; border: 1px solid #cbd5e1; color: #475569; border-radius: 8px; padding: 10px 20px; cursor: pointer; font-weight: 600; font-size: 0.85rem;" />
            <button label="Download" onClick="@command('performExport')"
                    style="background: #2563eb; border: none; color: white; border-radius: 8px; padding: 10px 20px; cursor: pointer; font-weight: 600; font-size: 0.85rem;" />
        </hlayout>
    </vlayout>
</window>
```
