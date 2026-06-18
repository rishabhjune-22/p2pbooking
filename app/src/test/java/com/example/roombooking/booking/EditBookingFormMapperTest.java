package com.example.roombooking.booking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class EditBookingFormMapperTest {

    @Test
    public void validateRejectsInvalidEditMutationInputs() {
        EditBookingFormState invalidDeparture = validState();
        invalidDeparture.setDepartureAtMillis(invalidDeparture.getArrivalAtMillis());
        assertInvalid(invalidDeparture, "Departure must be after arrival.");

        EditBookingFormState invalidRequestorMobile = validState();
        invalidRequestorMobile.setRequestorMobile("123");
        assertInvalid(invalidRequestorMobile, "Requestor mobile must be 10 digits.");

        EditBookingFormState missingAttenderCount = validState();
        missingAttenderCount.setAttenderRequired(true);
        missingAttenderCount.setAttenderCountPerDay(0);
        assertInvalid(missingAttenderCount, "Enter number of attenders required per day.");

        EditBookingFormState missingCharges = validState();
        missingCharges.setAttenderChargesStatus("yes");
        missingCharges.setAttenderChargesAmount("");
        EditBookingValidationResult result = EditBookingFormMapper.validate(missingCharges);
        assertFalse(result.isValid());
        assertEquals(EditBookingFormState.FIELD_ATTENDER_CHARGES_AMOUNT, result.getField());

        EditBookingFormState missingBudgetHeadValue = validState();
        missingBudgetHeadValue.setBudgetHeadType(EditBookingFormState.BUDGET_HEAD_INSTITUTE);
        missingBudgetHeadValue.setBudgetHeadValue("");
        result = EditBookingFormMapper.validate(missingBudgetHeadValue);
        assertFalse(result.isValid());
        assertEquals(EditBookingFormState.FIELD_BUDGET_HEAD_VALUE, result.getField());
    }

    @Test
    public void toUpdateRequestSerializesMutationPayload() {
        EditBookingFormState state = validState();
        state.setVisitorCategory("other_guest");
        state.setAttenderRequired(true);
        state.setAttenderCountPerDay(1);
        state.setAttenderDayShift(true);
        state.setAttenderChargesStatus("yes");
        state.setAttenderChargesAmount("500");
        state.setBudgetHeadType(EditBookingFormState.BUDGET_HEAD_INSTITUTE);
        state.setBudgetHeadValue("Computer Science");

        BookingUpdateRequest request = EditBookingFormMapper.toUpdateRequest(state);
        JsonObject json = JsonParser.parseString(new Gson().toJson(request)).getAsJsonObject();

        assertEquals(11, json.get("room").getAsInt());
        assertEquals("Edited Visitor", json.get("visitor_name").getAsString());
        assertEquals("other_guest", json.get("visitor_category").getAsString());
        assertTrue(json.get("attender_required").getAsBoolean());
        assertEquals(1, json.get("attender_count_per_day").getAsInt());
        assertTrue(json.get("attender_day_shift").getAsBoolean());
        assertFalse(json.has("attender_night_shift"));
        assertEquals("yes", json.get("attender_charges_status").getAsString());
        assertEquals("500", json.get("attender_charges_amount").getAsString());
        assertEquals("institute_head", json.get("budget_head_type").getAsString());
        assertEquals("Computer Science", json.get("budget_head_value").getAsString());
    }

    private static void assertInvalid(EditBookingFormState state, String message) {
        EditBookingValidationResult result = EditBookingFormMapper.validate(state);
        assertFalse(result.isValid());
        assertEquals(message, result.getMessage());
    }

    private static EditBookingFormState validState() {
        EditBookingFormState state = new EditBookingFormState();
        state.setRoomId(11);
        state.setVisitorName("Edited Visitor");
        state.setVisitorMobile("9876543210");
        state.setVisitorGender("Female");
        state.setPurpose("Review");
        state.setRoomChargesStatus("no");
        state.setRoomChargesAmount("0");
        state.setAttenderChargesStatus("no");
        state.setAttenderChargesAmount("0");
        state.setRequestorName("Requester");
        state.setRequestorMobile("9876543211");
        state.setLogisticsName("Logistics");
        state.setLogisticsMobile("9876543212");

        Calendar arrival = fixedCalendar(2026, Calendar.AUGUST, 2, 9, 0);
        Calendar departure = fixedCalendar(2026, Calendar.AUGUST, 2, 11, 0);
        EditBookingFormMapper.applyDateTimes(state, arrival, departure, apiFormatter());
        return state;
    }

    private static Calendar fixedCalendar(
            int year,
            int month,
            int day,
            int hour,
            int minute
    ) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private static SimpleDateFormat apiFormatter() {
        SimpleDateFormat formatter = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                Locale.US
        );
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter;
    }
}
