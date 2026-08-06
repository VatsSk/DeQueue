package com.dequeue.common.util;

import com.dequeue.vendor.entity.BusinessHour;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class DateUtils {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    
    private DateUtils() {}
    
    public static Instant startOfToday() {
        return LocalDate.now(IST).atStartOfDay(IST).toInstant();
    }
    
    public static Instant endOfToday() {
        return LocalDate.now(IST).plusDays(1).atStartOfDay(IST).toInstant();
    }
    
    public static boolean isWithinBusinessHours(List<BusinessHour> businessHours) {
        if (businessHours == null || businessHours.isEmpty()) return true;
        
        LocalDateTime now = LocalDateTime.now(IST);
        DayOfWeek today = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();
        
        return businessHours.stream()
                .filter(bh -> bh.getDayOfWeek() == today && !bh.isClosed())
                .anyMatch(bh -> !currentTime.isBefore(bh.getOpenTime()) && !currentTime.isAfter(bh.getCloseTime()));
    }
    
    public static String formatTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, IST).format(TIME_FORMATTER);
    }
    
    public static String formatDate(Instant instant) {
        return LocalDateTime.ofInstant(instant, IST).format(DATE_FORMATTER);
    }
}
