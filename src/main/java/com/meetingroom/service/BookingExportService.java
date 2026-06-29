package com.meetingroom.service;

import com.meetingroom.model.Booking;

import java.util.List;

public interface BookingExportService {
    byte[] exportToExcel(List<Booking> bookings) throws Exception;
    byte[] exportToPdf(List<Booking> bookings) throws Exception;
    byte[] exportToXml(List<Booking> bookings) throws Exception;
}
