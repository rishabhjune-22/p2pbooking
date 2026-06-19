package com.example.roombooking.booking;

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
        Calendar arrival = Calendar.getInstance();
        Calendar departure = Calendar.getInstance();
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
                setCalendarDateOnly(arrival, initialData.getArrivalDate(), 10, 0);
            }

            if (hasDepartureDate) {
                setCalendarDateOnly(departure, initialData.getDepartureDate(), 10, 0);
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
        Calendar calendar = Calendar.getInstance();
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
                data.getCreatedByName(),

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

        if (isEmpty(data.getCreatedByName())) {
            return CreateBookingValidationResult.invalid("Name is required");
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

        if (data.isAttenderRequired() && data.getAttenderCountPerDay() <= 0) {
            return CreateBookingValidationResult.invalid(
                    "Enter number of attenders required per day."
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

        if (!isEmpty(data.getBudgetHeadType()) && isEmpty(data.getBudgetHeadValue())) {
            return CreateBookingValidationResult.invalid(
                    "Enter budget head value.",
                    CreateBookingFormState.FIELD_BUDGET_HEAD_VALUE
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
