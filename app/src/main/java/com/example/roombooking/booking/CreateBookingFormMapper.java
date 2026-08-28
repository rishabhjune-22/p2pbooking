package com.example.roombooking.booking;

import com.example.roombooking.utils.DateTimeUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;

final class CreateBookingFormMapper {

    private static final long ONE_HOUR_MILLIS = 60L * 60L * 1000L;

    private CreateBookingFormMapper() {
    }

    static CreateBookingFormState defaultState(
            CreateBookingInitialData initialData,
            SimpleDateFormat apiDateTimeFormat
    ) {
        CreateBookingFormState state = new CreateBookingFormState();
        Calendar arrival = DateTimeUtils.newBookingCalendar();
        Calendar departure = DateTimeUtils.newBookingCalendar();
        departure.setTimeInMillis(arrival.getTimeInMillis() + ONE_HOUR_MILLIS);

        if (initialData != null) {
            state.setRoomId(initialData.getRoomId());
            state.setPreselectedRoomName(initialData.getRoomName());
            state.setHasPreselectedRoom(initialData.getRoomId() != null);
            state.setPartialRoom(initialData.isPartialRoom());
            state.setAvailableFromDate(initialData.getAvailableFromDate());
            state.setAvailableFromTime(initialData.getAvailableFromTime());

            boolean hasArrivalDate = !isEmpty(initialData.getArrivalDate());
            boolean hasDepartureDate = !isEmpty(initialData.getDepartureDate());

            if (hasArrivalDate) {
                setCalendarValue(arrival, initialData.getArrivalDate(), 10, 0, apiDateTimeFormat);
            }

            if (hasDepartureDate) {
                setCalendarValue(departure, initialData.getDepartureDate(), 10, 0, apiDateTimeFormat);
            }

            state.setHasPreselectedDateRange(hasArrivalDate && hasDepartureDate);
        }

        ensureDepartureAfterArrival(arrival, departure);
        applyDateTimes(state, arrival, departure, apiDateTimeFormat);
        return state;
    }

    static void applyDateTimes(
            CreateBookingFormState state,
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
        Calendar calendar = DateTimeUtils.newBookingCalendar();
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    static void ensureDepartureAfterArrival(Calendar arrival, Calendar departure) {
        if (!departure.getTime().after(arrival.getTime())) {
            departure.setTimeInMillis(arrival.getTimeInMillis() + ONE_HOUR_MILLIS);
        }
    }

    static BookingCreateRequest toCreateRequest(CreateBookingFormState data) {
        return new BookingCreateRequest(
                data.getRoomId(),
                data.getArrivalAt(),
                data.getDepartureAt(),

                data.getVisitorName(),
                data.getVisitorDesignation(),
                data.getVisitorOrganisation(),
                data.getVisitorGender(),
                data.getVisitorMobile(),
                data.getVisitorEmail(),
                data.getPurpose(),
                data.getVisitorCategory(),

                data.isAttenderRequired(),
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

    static CreateBookingValidationResult validate(CreateBookingFormState data) {
        if (data.getRoomId() == null) {
            return CreateBookingValidationResult.invalid("Please select a room.");
        }

        if (isEmpty(data.getVisitorName())
                || isEmpty(data.getArrivalAt())
                || isEmpty(data.getDepartureAt())) {
            return CreateBookingValidationResult.invalid("Please fill all required fields.");
        }

        if (!isEmpty(data.getVisitorMobile())
                && !data.getVisitorMobile().matches("\\d{10}")) {
            return CreateBookingValidationResult.invalid("Visitor mobile must be 10 digits.");
        }

        if (!isEmpty(data.getRequestorMobile())
                && !data.getRequestorMobile().matches("\\d{10}")) {
            return CreateBookingValidationResult.invalid("Requestor mobile must be 10 digits.");
        }

        if (!isEmpty(data.getLogisticsMobile())
                && !data.getLogisticsMobile().matches("\\d{10}")) {
            return CreateBookingValidationResult.invalid("Logistics mobile must be 10 digits.");
        }

        if (data.getDepartureAtMillis() <= data.getArrivalAtMillis()) {
            return CreateBookingValidationResult.invalid(
                    "Departure date/time must be after arrival date/time."
            );
        }

        if (data.isAttenderRequired()
                && !data.isAttenderGeneralShift()
                && !data.isAttenderMorningShift()
                && !data.isAttenderDayShift()) {
            return CreateBookingValidationResult.invalid(
                    "Please select at least one attender shift."
            );
        }

        if ("yes".equals(data.getRoomChargesStatus())
                && !isPositiveAmount(data.getRoomChargesAmount())) {
            return CreateBookingValidationResult.invalid(
                    "Enter room charges amount.",
                    CreateBookingFormState.FIELD_ROOM_CHARGES_AMOUNT
            );
        }

        if ("yes".equals(data.getAttenderChargesStatus())
                && !isPositiveAmount(data.getAttenderChargesAmount())) {
            return CreateBookingValidationResult.invalid(
                    "Enter attender charges amount.",
                    CreateBookingFormState.FIELD_ATTENDER_CHARGES_AMOUNT
            );
        }

        return CreateBookingValidationResult.valid();
    }

    static String makeFriendlyMessage(String message) {
        return EditBookingFormMapper.makeFriendlyMessage(message);
    }

    private static void setCalendarDateOnly(
            Calendar calendar,
            String date,
            int hour,
            int minute
    ) {
        try {
            String[] parts = date.split("-");

            if (parts.length != 3) {
                return;
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day = Integer.parseInt(parts[2]);

            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        } catch (Exception ignored) {
            // Keep default calendar value.
        }
    }

    private static void setCalendarValue(
            Calendar calendar,
            String value,
            int defaultHour,
            int defaultMinute,
            SimpleDateFormat apiDateTimeFormat
    ) {
        if (isEmpty(value)) {
            return;
        }

        if (value.contains("T")) {
            try {
                calendar.setTime(apiDateTimeFormat.parse(value));
                return;
            } catch (Exception ignored) {
                // Fall back to date-only parsing below.
            }
        }

        setCalendarDateOnly(calendar, value, defaultHour, defaultMinute);
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
}
