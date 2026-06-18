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

public class CreateBookingFormMapperTest {

    @Test
    public void defaultStatePreservesPreselectedRoomAndDateRange() {
        CreateBookingInitialData initialData = new CreateBookingInitialData(
                42,
                "Alpha 101",
                "2026-07-01",
                "2026-07-01",
                true,
                "2026-07-01",
                "10:00"
        );

        CreateBookingFormState state = CreateBookingFormMapper.defaultState(
                initialData,
                apiFormatter()
        );

        Calendar arrival = CreateBookingFormMapper.calendarFromMillis(
                state.getArrivalAtMillis()
        );
        Calendar departure = CreateBookingFormMapper.calendarFromMillis(
                state.getDepartureAtMillis()
        );

        assertEquals(Integer.valueOf(42), state.getRoomId());
        assertEquals("Alpha 101", state.getPreselectedRoomName());
        assertTrue(state.hasPreselectedRoom());
        assertTrue(state.hasPreselectedDateRange());
        assertTrue(state.isPartialRoom());
        assertEquals(Calendar.JULY, arrival.get(Calendar.MONTH));
        assertEquals(1, arrival.get(Calendar.DAY_OF_MONTH));
        assertTrue(departure.getTime().after(arrival.getTime()));
    }

    @Test
    public void validateRejectsInvalidCreateMutationInputs() {
        CreateBookingFormState missingRoom = validState();
        missingRoom.setRoomId(null);
        assertInvalid(missingRoom, "Please select a room.");

        CreateBookingFormState invalidMobile = validState();
        invalidMobile.setVisitorMobile("12345");
        assertInvalid(invalidMobile, "Visitor mobile must be 10 digits.");

        CreateBookingFormState missingShift = validState();
        missingShift.setAttenderRequired(true);
        missingShift.setAttenderCountPerDay(1);
        missingShift.setAttenderGeneralShift(false);
        missingShift.setAttenderMorningShift(false);
        missingShift.setAttenderDayShift(false);
        assertInvalid(missingShift, "Please select at least one attender shift.");

        CreateBookingFormState missingCharges = validState();
        missingCharges.setRoomChargesStatus("yes");
        missingCharges.setRoomChargesAmount("0");
        CreateBookingValidationResult result = CreateBookingFormMapper.validate(missingCharges);
        assertFalse(result.isValid());
        assertEquals(CreateBookingFormState.FIELD_ROOM_CHARGES_AMOUNT, result.getField());

        CreateBookingFormState missingBudgetHeadValue = validState();
        missingBudgetHeadValue.setBudgetHeadType(CreateBookingFormState.BUDGET_HEAD_PROJECT);
        missingBudgetHeadValue.setBudgetHeadValue("");
        result = CreateBookingFormMapper.validate(missingBudgetHeadValue);
        assertFalse(result.isValid());
        assertEquals(CreateBookingFormState.FIELD_BUDGET_HEAD_VALUE, result.getField());
    }

    @Test
    public void toCreateRequestSerializesMutationPayload() {
        CreateBookingFormState state = validState();
        state.setCreatedByName("Admin User");
        state.setVisitorCategory("conference_workshop_guest");
        state.setAttenderRequired(true);
        state.setAttenderCountPerDay(2);
        state.setAttenderMorningShift(true);
        state.setRoomChargesStatus("yes");
        state.setRoomChargesAmount("1500");
        state.setBudgetHeadType(CreateBookingFormState.BUDGET_HEAD_PROJECT);
        state.setBudgetHeadValue("PRJ-2026-001");

        BookingCreateRequest request = CreateBookingFormMapper.toCreateRequest(state);
        JsonObject json = JsonParser.parseString(new Gson().toJson(request)).getAsJsonObject();

        assertEquals(7, json.get("room").getAsInt());
        assertEquals("Admin User", json.get("created_by_name").getAsString());
        assertEquals("Visitor One", json.get("visitor_name").getAsString());
        assertEquals("conference_workshop_guest", json.get("visitor_category").getAsString());
        assertTrue(json.get("attender_required").getAsBoolean());
        assertEquals(2, json.get("attender_count_per_day").getAsInt());
        assertTrue(json.get("attender_morning_shift").getAsBoolean());
        assertFalse(json.has("attender_night_shift"));
        assertEquals("yes", json.get("room_charges_status").getAsString());
        assertEquals("1500", json.get("room_charges_amount").getAsString());
        assertEquals("project_head", json.get("budget_head_type").getAsString());
        assertEquals("PRJ-2026-001", json.get("budget_head_value").getAsString());
    }

    private static void assertInvalid(CreateBookingFormState state, String message) {
        CreateBookingValidationResult result = CreateBookingFormMapper.validate(state);
        assertFalse(result.isValid());
        assertEquals(message, result.getMessage());
    }

    private static CreateBookingFormState validState() {
        CreateBookingFormState state = new CreateBookingFormState();
        state.setRoomId(7);
        state.setVisitorName("Visitor One");
        state.setVisitorMobile("9876543210");
        state.setVisitorGender("Male");
        state.setPurpose("Workshop");
        state.setRoomChargesStatus("no");
        state.setRoomChargesAmount("0");
        state.setAttenderChargesStatus("no");
        state.setAttenderChargesAmount("0");
        state.setRequestorName("Requester");
        state.setRequestorMobile("9876543211");
        state.setLogisticsName("Logistics");
        state.setLogisticsMobile("9876543212");

        Calendar arrival = fixedCalendar(2026, Calendar.JULY, 1, 10, 0);
        Calendar departure = fixedCalendar(2026, Calendar.JULY, 1, 12, 0);
        CreateBookingFormMapper.applyDateTimes(state, arrival, departure, apiFormatter());
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
