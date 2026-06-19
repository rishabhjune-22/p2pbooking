package com.example.roombooking.booking;

import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.utils.DateTimeUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class EditBookingFormMapper {

    private static final long ONE_HOUR_MILLIS = 60L * 60L * 1000L;
    private static final Pattern BOOKED_CONFLICT_PATTERN = Pattern.compile(
            "(.+?) is already booked from (.+?) to (.+?)\\.",
            Pattern.CASE_INSENSITIVE
    );

    private EditBookingFormMapper() {
    }

    static EditBookingFormState fromBookingItem(
            BookingItem bookingItem,
            SimpleDateFormat apiDateTimeFormat
    ) {
        EditBookingFormState state = new EditBookingFormState();
        state.setRoomId(bookingItem.getRoom());
        state.setVisitorName(bookingItem.getVisitorName());
        state.setVisitorDesignation(bookingItem.getVisitorDesignation());
        state.setVisitorOrganisation(bookingItem.getVisitorOrganisation());
        state.setVisitorGender(bookingItem.getVisitorGender());
        state.setVisitorAddress(bookingItem.getVisitorAddress());
        state.setVisitorMobile(bookingItem.getVisitorMobile());
        state.setVisitorEmail(bookingItem.getVisitorEmail());
        state.setPurpose(bookingItem.getPurposeOfVisit());
        state.setVisitorCategory(bookingItem.getVisitorCategory());
        state.setAttenderRequired(bookingItem.isAttenderRequired());
        state.setAttenderCountPerDay(bookingItem.getAttenderCountPerDay());
        state.setAttenderGeneralShift(bookingItem.isAttenderGeneralShift());
        state.setAttenderMorningShift(bookingItem.isAttenderMorningShift());
        state.setAttenderDayShift(bookingItem.isAttenderDayShift());
        state.setRoomChargesStatus(bookingItem.getRoomChargesStatus());
        state.setAttenderChargesStatus(bookingItem.getAttenderChargesStatus());
        state.setRoomChargesAmount(
                "yes".equalsIgnoreCase(bookingItem.getRoomChargesStatus())
                        ? bookingItem.getRoomChargesAmount()
                        : ""
        );
        state.setAttenderChargesAmount(
                "yes".equalsIgnoreCase(bookingItem.getAttenderChargesStatus())
                        ? bookingItem.getAttenderChargesAmount()
                        : ""
        );
        state.setBudgetHeadType(bookingItem.getBudgetHeadType());
        state.setBudgetHeadValue(bookingItem.getBudgetHeadValue());
        state.setBudgetHeadName(resolveBudgetHeadName(bookingItem));
        state.setBudgetHeadDepartmentName(resolveBudgetHeadDepartmentName(bookingItem));
        state.setBudgetHeadProjectCode(resolveBudgetHeadProjectCode(bookingItem));
        state.setRequestorName(bookingItem.getRequestorName());
        state.setRequestorDesignation(bookingItem.getRequestorDesignation());
        state.setRequestorDepartment(bookingItem.getRequestorDepartment());
        state.setRequestorMobile(bookingItem.getRequestorMobile());
        state.setLogisticsName(bookingItem.getLogisticsName());
        state.setLogisticsDesignation(bookingItem.getLogisticsDesignation());
        state.setLogisticsMobile(bookingItem.getLogisticsMobile());

        Calendar arrival = Calendar.getInstance();
        Calendar departure = Calendar.getInstance();
        parseCalendarFromApiString(bookingItem.getArrivalAt(), arrival, apiDateTimeFormat);
        parseCalendarFromApiString(bookingItem.getDepartureAt(), departure, apiDateTimeFormat);
        ensureDepartureAfterArrival(arrival, departure);

        applyDateTimes(state, arrival, departure, apiDateTimeFormat);
        return state;
    }

    private static String resolveBudgetHeadName(BookingItem bookingItem) {
        String value = bookingItem.getBudgetHeadName();
        if (!isEmpty(value)) {
            return value;
        }

        if (EditBookingFormState.BUDGET_HEAD_INDIVIDUAL.equals(bookingItem.getBudgetHeadType())) {
            return bookingItem.getBudgetHeadValue();
        }

        return "";
    }

    private static String resolveBudgetHeadDepartmentName(BookingItem bookingItem) {
        String value = bookingItem.getBudgetHeadDepartmentName();
        if (!isEmpty(value)) {
            return value;
        }

        if (EditBookingFormState.BUDGET_HEAD_INSTITUTE.equals(bookingItem.getBudgetHeadType())) {
            return bookingItem.getBudgetHeadValue();
        }

        return "";
    }

    private static String resolveBudgetHeadProjectCode(BookingItem bookingItem) {
        String value = bookingItem.getBudgetHeadProjectCode();
        if (!isEmpty(value)) {
            return value;
        }

        if (EditBookingFormState.BUDGET_HEAD_PROJECT.equals(bookingItem.getBudgetHeadType())) {
            return bookingItem.getBudgetHeadValue();
        }

        return "";
    }

    static void applyDateTimes(
            EditBookingFormState state,
            Calendar arrival,
            Calendar departure,
            SimpleDateFormat apiDateTimeFormat
    ) {
        state.setArrivalAtMillis(arrival.getTimeInMillis());
        state.setDepartureAtMillis(departure.getTimeInMillis());
        state.setArrivalAt(apiDateTimeFormat.format(arrival.getTime()));
        state.setDepartureAt(apiDateTimeFormat.format(departure.getTime()));
    }

    static Calendar calendarFromMillis(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    static void ensureDepartureAfterArrival(Calendar arrival, Calendar departure) {
        if (!departure.getTime().after(arrival.getTime())) {
            departure.setTimeInMillis(arrival.getTimeInMillis() + ONE_HOUR_MILLIS);
        }
    }

    static BookingUpdateRequest toUpdateRequest(EditBookingFormState data) {
        return new BookingUpdateRequest(
                data.getRoomId(),
                data.getArrivalAt(),
                data.getDepartureAt(),

                data.getVisitorName(),
                data.getVisitorDesignation(),
                data.getVisitorOrganisation(),
                data.getVisitorGender(),
                data.getVisitorAddress(),
                data.getVisitorMobile(),
                data.getVisitorEmail(),
                data.getPurpose(),

                data.getVisitorCategory(),
                data.isAttenderRequired(),
                data.getAttenderCountPerDay(),
                data.isAttenderGeneralShift(),
                data.isAttenderMorningShift(),
                data.isAttenderDayShift(),
                data.getRoomChargesStatus(),
                data.getAttenderChargesStatus(),
                data.getRoomChargesAmount(),
                data.getAttenderChargesAmount(),

                data.getBudgetHeadType(),
                data.getBudgetHeadValue(),
                data.getBudgetHeadName(),
                data.getBudgetHeadDepartmentName(),
                data.getBudgetHeadProjectCode(),

                data.getRequestorName(),
                data.getRequestorDesignation(),
                data.getRequestorDepartment(),
                data.getRequestorMobile(),

                data.getLogisticsName(),
                data.getLogisticsDesignation(),
                data.getLogisticsMobile()
        );
    }

    static EditBookingValidationResult validate(EditBookingFormState data) {
        if (data.getRoomId() == null) {
            return EditBookingValidationResult.invalid("Please select a room.");
        }

        if (isEmpty(data.getVisitorName())
                || isEmpty(data.getArrivalAt())
                || isEmpty(data.getDepartureAt())) {
            return EditBookingValidationResult.invalid("Please fill all required fields.");
        }

        if (!isEmpty(data.getVisitorMobile())
                && !data.getVisitorMobile().matches("\\d{10}")) {
            return EditBookingValidationResult.invalid("Visitor mobile must be 10 digits.");
        }

        if (!isEmpty(data.getRequestorMobile())
                && !data.getRequestorMobile().matches("\\d{10}")) {
            return EditBookingValidationResult.invalid("Requestor mobile must be 10 digits.");
        }

        if (!isEmpty(data.getLogisticsMobile())
                && !data.getLogisticsMobile().matches("\\d{10}")) {
            return EditBookingValidationResult.invalid("Logistics mobile must be 10 digits.");
        }

        if (data.getDepartureAtMillis() <= data.getArrivalAtMillis()) {
            return EditBookingValidationResult.invalid("Departure must be after arrival.");
        }

        if (data.isAttenderRequired() && data.getAttenderCountPerDay() <= 0) {
            return EditBookingValidationResult.invalid(
                    "Enter number of attenders required per day."
            );
        }

        if (data.isAttenderRequired()
                && !data.isAttenderGeneralShift()
                && !data.isAttenderMorningShift()
                && !data.isAttenderDayShift()) {
            return EditBookingValidationResult.invalid(
                    "Please select at least one attender shift."
            );
        }

        if ("yes".equals(data.getRoomChargesStatus())
                && !isPositiveAmount(data.getRoomChargesAmount())) {
            return EditBookingValidationResult.invalid(
                    "Enter room charges amount.",
                    EditBookingFormState.FIELD_ROOM_CHARGES_AMOUNT
            );
        }

        if ("yes".equals(data.getAttenderChargesStatus())
                && !isPositiveAmount(data.getAttenderChargesAmount())) {
            return EditBookingValidationResult.invalid(
                    "Enter attender charges amount.",
                    EditBookingFormState.FIELD_ATTENDER_CHARGES_AMOUNT
            );
        }

        return EditBookingValidationResult.valid();
    }

    static String makeFriendlyMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Something went wrong. Please try again.";
        }

        String conflictMessage = buildFriendlyConflictMessage(message);

        if (!conflictMessage.isEmpty()) {
            return conflictMessage;
        }

        String lowerMessage = message.toLowerCase(Locale.ROOT).trim();

        if (lowerMessage.contains("before the next booking")
                || lowerMessage.contains("next booking starts")) {
            return buildFriendlyNextBookingCoolingMessage(message);
        }

        if (lowerMessage.contains("cooling period")) {
            return buildFriendlyCoolingMessage(message);
        }

        if (lowerMessage.contains("unavailable")) {
            return buildFriendlyUnavailableMessage(message);
        }

        if (lowerMessage.contains("arrival")
                && lowerMessage.contains("departure")) {
            return "Please check the arrival and departure date/time. Departure must be after arrival.";
        }

        if (lowerMessage.contains("mobile")
                || lowerMessage.contains("phone")) {
            return "Please enter a valid 10-digit mobile number.";
        }

        if (lowerMessage.contains("required")
                || lowerMessage.contains("blank")
                || lowerMessage.contains("empty")
                || lowerMessage.contains("null")) {
            return "Please fill all required details before saving the booking.";
        }

        if (lowerMessage.contains("network")
                || lowerMessage.contains("internet")
                || lowerMessage.contains("timeout")
                || lowerMessage.contains("failed to connect")) {
            return "Internet connection seems slow or unavailable. Please check your connection and try again.";
        }

        if (lowerMessage.contains("failed")) {
            return "Booking could not be updated. Please check the details and try again.";
        }

        return message.trim();
    }

    private static void parseCalendarFromApiString(
            String value,
            Calendar calendar,
            SimpleDateFormat apiDateTimeFormat
    ) {
        try {
            if (value == null || value.trim().isEmpty()) {
                return;
            }

            Date date = apiDateTimeFormat.parse(value);

            if (date != null) {
                calendar.setTime(date);
            }

        } catch (Exception ignored) {
            // Keep current default date/time.
        }
    }

    private static boolean isPositiveAmount(String amount) {
        try {
            return !isEmpty(amount) && new BigDecimal(amount).signum() > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String buildFriendlyConflictMessage(String message) {
        Matcher matcher = BOOKED_CONFLICT_PATTERN.matcher(message.trim());

        if (!matcher.find()) {
            return "";
        }

        String roomName = matcher.group(1).trim();
        String startTime = formatBackendDateTime(matcher.group(2).trim());
        String endTime = formatBackendDateTime(matcher.group(3).trim());

        return roomName
                + " is already booked from "
                + startTime
                + " to "
                + endTime
                + ". Please choose another room or change the timing.";
    }

    private static String buildFriendlyCoolingMessage(String message) {
        String lowerMessage = message.toLowerCase(Locale.ROOT);

        if (!lowerMessage.contains("booked after")) {
            return "This room is in cooling period after a previous booking. Please choose a later time or another room.";
        }

        int index = lowerMessage.indexOf("booked after");
        String dateTimeText = message.substring(index + "booked after".length())
                .replace(".", "")
                .trim();
        String availableAfter = formatBackendDateTime(dateTimeText);

        return "This room is in cooling period after a previous booking. It can be booked after "
                + availableAfter
                + ".";
    }

    private static String buildFriendlyNextBookingCoolingMessage(String message) {
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        String marker = "departure must be at or before";
        int index = lowerMessage.indexOf(marker);

        if (index < 0) {
            return "This room needs a 1-hour gap before the next booking. Please choose an earlier departure time.";
        }

        String dateTimeText = message.substring(index + marker.length())
                .replace(".", "")
                .trim();
        String latestDeparture = formatBackendDateTime(dateTimeText);

        return "This room needs a 1-hour gap before the next booking. Set departure at or before "
                + latestDeparture
                + ".";
    }

    private static String buildFriendlyUnavailableMessage(String message) {
        return message
                .replace("+00:00", "")
                .replace(" 00:00:00", "")
                .trim();
    }

    private static String formatBackendDateTime(String value) {
        return DateTimeUtils.formatUtcToLocal(value);
    }
}
