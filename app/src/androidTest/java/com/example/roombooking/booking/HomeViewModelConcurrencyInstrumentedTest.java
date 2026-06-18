package com.example.roombooking.booking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.roombooking.api.ApiService;
import com.example.roombooking.home.HomeViewModel;
import com.example.roombooking.model.booking.BookingActionData;
import com.example.roombooking.model.booking.BookingItem;
import com.example.roombooking.model.common.ApiResponse;
import com.example.roombooking.model.common.PaginatedData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Request;
import okhttp3.ResponseBody;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RunWith(AndroidJUnit4.class)
public class HomeViewModelConcurrencyInstrumentedTest {

    private final Gson gson = new Gson();

    @Test
    public void replacingListRequestIgnoresStaleResponse() {
        FakeApi fakeApi = new FakeApi();
        HomeViewModel viewModel = createViewModel(fakeApi);

        runOnMain(viewModel::loadInitialBookings);
        FakeCall<?> firstCall = fakeApi.bookingCalls.get(0);

        runOnMain(() -> viewModel.applyFilter(null, null, null, "expired"));
        FakeCall<?> secondCall = fakeApi.bookingCalls.get(1);

        assertTrue(firstCall.isCanceled());

        deliverBookings(firstCall, "Old active result");
        deliverBookings(secondCall, "Current expired result");

        AtomicReference<List<BookingItem>> items = new AtomicReference<>();
        runOnMain(() -> items.set(viewModel.getBookingsLiveData().getValue()));

        assertEquals(1, items.get().size());
        assertEquals("Current expired result", items.get().get(0).getVisitorName());
    }

    @Test
    public void repeatedDeleteCreatesOneMutationRequest() {
        FakeApi fakeApi = new FakeApi();
        HomeViewModel viewModel = createViewModel(fakeApi);
        BookingItem booking = gson.fromJson(
                "{\"id\":42,\"status\":\"active\"}",
                BookingItem.class
        );

        runOnMain(() -> {
            viewModel.deleteBooking(booking);
            viewModel.deleteBooking(booking);
        });

        assertEquals(1, fakeApi.deleteCalls.size());
    }

    @Test
    public void rapidNextPageTriggersCreateOneInFlightRequest() {
        FakeApi fakeApi = new FakeApi();
        HomeViewModel viewModel = createViewModel(fakeApi);

        runOnMain(viewModel::loadInitialBookings);
        deliverBookings(fakeApi.bookingCalls.get(0), "First page", true);

        runOnMain(() -> {
            viewModel.loadNextPage();
            viewModel.loadNextPage();
            viewModel.loadNextPage();
        });

        assertEquals(2, fakeApi.bookingCalls.size());
        assertEquals(2, fakeApi.bookingPages.get(1).intValue());
    }

    @Test
    public void pageTwoNotFoundMarksLastPageAndStopsFurtherPagination() {
        FakeApi fakeApi = new FakeApi();
        HomeViewModel viewModel = createViewModel(fakeApi);

        runOnMain(viewModel::loadInitialBookings);
        deliverBookings(fakeApi.bookingCalls.get(0), "First page", true);

        runOnMain(viewModel::loadNextPage);
        deliverHttpError(fakeApi.bookingCalls.get(1), 404);

        runOnMain(viewModel::loadNextPage);

        assertTrue(viewModel.isLastPage());
        assertFalse(viewModel.isLoading());
        assertEquals(2, fakeApi.bookingCalls.size());
    }

    @Test
    public void rateLimitedPageDoesNotRetryByItself() {
        FakeApi fakeApi = new FakeApi();
        HomeViewModel viewModel = createViewModel(fakeApi);

        runOnMain(viewModel::loadInitialBookings);
        deliverBookings(fakeApi.bookingCalls.get(0), "First page", true);

        runOnMain(viewModel::loadNextPage);
        deliverHttpError(fakeApi.bookingCalls.get(1), 429);

        assertFalse(viewModel.isLoading());
        assertEquals(2, fakeApi.bookingCalls.size());
    }

    @Test
    public void networkFailureStopsWithoutAutoLoadingNextPage() {
        FakeApi fakeApi = new FakeApi();
        HomeViewModel viewModel = createViewModel(fakeApi);

        runOnMain(viewModel::loadInitialBookings);
        deliverBookings(fakeApi.bookingCalls.get(0), "First page", true);

        runOnMain(viewModel::loadNextPage);
        failCall(fakeApi.bookingCalls.get(1), new IOException("server down"));

        assertFalse(viewModel.isLoading());
        assertEquals(2, fakeApi.bookingCalls.size());
    }

    @Test
    public void refreshCancelsOldRequestAndIgnoresCanceledCallback() {
        FakeApi fakeApi = new FakeApi();
        HomeViewModel viewModel = createViewModel(fakeApi);

        runOnMain(viewModel::loadInitialBookings);
        FakeCall<?> firstCall = fakeApi.bookingCalls.get(0);

        runOnMain(viewModel::refreshBookings);
        FakeCall<?> secondCall = fakeApi.bookingCalls.get(1);

        assertTrue(firstCall.isCanceled());

        deliverBookings(firstCall, "Stale canceled result", false);
        deliverBookings(secondCall, "Fresh result", false);

        AtomicReference<List<BookingItem>> items = new AtomicReference<>();
        runOnMain(() -> items.set(viewModel.getBookingsLiveData().getValue()));

        assertEquals(1, items.get().size());
        assertEquals("Fresh result", items.get().get(0).getVisitorName());
    }

    @Test
    public void deleteSuccessClearsListAndRefreshesPageOneOnce() {
        FakeApi fakeApi = new FakeApi();
        HomeViewModel viewModel = createViewModel(fakeApi);
        BookingItem booking = gson.fromJson(
                "{\"id\":42,\"visitor_name\":\"Deletable\",\"status\":\"active\"}",
                BookingItem.class
        );

        runOnMain(viewModel::loadInitialBookings);
        deliverBookings(fakeApi.bookingCalls.get(0), "Existing result", true);

        runOnMain(() -> viewModel.deleteBooking(booking));
        deliverDeleteSuccess(fakeApi.deleteCalls.get(0));

        AtomicReference<List<BookingItem>> items = new AtomicReference<>();
        runOnMain(() -> items.set(viewModel.getBookingsLiveData().getValue()));

        assertEquals(2, fakeApi.bookingCalls.size());
        assertEquals(1, fakeApi.bookingPages.get(1).intValue());
        assertTrue(items.get().isEmpty());
    }

    private HomeViewModel createViewModel(FakeApi fakeApi) {
        return new HomeViewModel(new BookingRepository(fakeApi.service));
    }

    @SuppressWarnings("unchecked")
    private void deliverBookings(FakeCall<?> rawCall, String visitorName) {
        deliverBookings(rawCall, visitorName, false);
    }

    @SuppressWarnings("unchecked")
    private void deliverBookings(FakeCall<?> rawCall, String visitorName, boolean hasNextPage) {
        ApiResponse<PaginatedData<BookingItem>> body = gson.fromJson(
                "{\"success\":true,\"message\":\"ok\",\"data\":{" +
                        "\"count\":1,\"next\":" + nextPageJson(hasNextPage) +
                        ",\"previous\":null," +
                        "\"results\":[{\"id\":1,\"visitor_name\":\"" + visitorName +
                        "\",\"status\":\"active\"}]}}",
                new TypeToken<ApiResponse<PaginatedData<BookingItem>>>() {}.getType()
        );
        FakeCall<ApiResponse<PaginatedData<BookingItem>>> call =
                (FakeCall<ApiResponse<PaginatedData<BookingItem>>>) rawCall;
        runOnMain(() -> call.deliver(Response.success(body)));
    }

    @SuppressWarnings("unchecked")
    private void deliverHttpError(FakeCall<?> rawCall, int code) {
        FakeCall<ApiResponse<PaginatedData<BookingItem>>> call =
                (FakeCall<ApiResponse<PaginatedData<BookingItem>>>) rawCall;
        runOnMain(() -> call.deliver(Response.error(
                code,
                ResponseBody.create(null, "")
        )));
    }

    @SuppressWarnings("unchecked")
    private void failCall(FakeCall<?> rawCall, Throwable throwable) {
        FakeCall<ApiResponse<PaginatedData<BookingItem>>> call =
                (FakeCall<ApiResponse<PaginatedData<BookingItem>>>) rawCall;
        runOnMain(() -> call.fail(throwable));
    }

    @SuppressWarnings("unchecked")
    private void deliverDeleteSuccess(FakeCall<?> rawCall) {
        ApiResponse<BookingActionData> body = gson.fromJson(
                "{\"success\":true,\"message\":\"Deleted\",\"data\":{}}",
                new TypeToken<ApiResponse<BookingActionData>>() {}.getType()
        );
        FakeCall<ApiResponse<BookingActionData>> call =
                (FakeCall<ApiResponse<BookingActionData>>) rawCall;
        runOnMain(() -> call.deliver(Response.success(body)));
    }

    private String nextPageJson(boolean hasNextPage) {
        return hasNextPage ? "\"http://localhost/api/bookings/?page=2\"" : "null";
    }

    private void runOnMain(Runnable action) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(action);
    }

    private static final class FakeApi {
        private final List<FakeCall<?>> bookingCalls = new ArrayList<>();
        private final List<Integer> bookingPages = new ArrayList<>();
        private final List<FakeCall<?>> deleteCalls = new ArrayList<>();
        private final ApiService service = (ApiService) Proxy.newProxyInstance(
                ApiService.class.getClassLoader(),
                new Class<?>[]{ApiService.class},
                (proxy, method, args) -> {
                    FakeCall<?> call = new FakeCall<>();
                    if ("getBookings".equals(method.getName())) {
                        bookingCalls.add(call);
                        bookingPages.add((Integer) args[0]);
                    } else if ("deleteBooking".equals(method.getName())) {
                        deleteCalls.add(call);
                    }
                    return call;
                }
        );
    }

    private static final class FakeCall<T> implements Call<T> {
        private Callback<T> callback;
        private boolean executed;
        private boolean canceled;

        @Override
        public Response<T> execute() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void enqueue(Callback<T> callback) {
            this.callback = callback;
            executed = true;
        }

        void deliver(Response<T> response) {
            callback.onResponse(this, response);
        }

        void fail(Throwable throwable) {
            callback.onFailure(this, throwable);
        }

        @Override
        public boolean isExecuted() {
            return executed;
        }

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public Call<T> clone() {
            return new FakeCall<>();
        }

        @Override
        public Request request() {
            return new Request.Builder().url("http://localhost/").build();
        }

        @Override
        public Timeout timeout() {
            return Timeout.NONE;
        }
    }
}
