package com.example.roombooking;

import static org.junit.Assert.assertEquals;

import com.example.roombooking.utils.ApiErrorUtils;
import com.example.roombooking.utils.SyncStatusFormatter;

import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ApiErrorUtilsTest {

    @Test
    public void throwableMessagesAreUserFriendly() {
        assertEquals(
                ApiErrorUtils.NO_INTERNET_ERROR_MESSAGE,
                ApiErrorUtils.messageFromThrowable(new UnknownHostException())
        );
        assertEquals(
                ApiErrorUtils.TIMEOUT_ERROR_MESSAGE,
                ApiErrorUtils.messageFromThrowable(new SocketTimeoutException())
        );
        assertEquals(
                ApiErrorUtils.SERVER_UNAVAILABLE_ERROR_MESSAGE,
                ApiErrorUtils.messageFromThrowable(new ConnectException())
        );
        assertEquals(
                ApiErrorUtils.NETWORK_ERROR_MESSAGE,
                ApiErrorUtils.messageFromThrowable(new IOException())
        );
    }

    @Test
    public void cachedDataMessagesAvoidRawFailures() {
        assertEquals(
                "Server unavailable. Showing saved data.",
                ApiErrorUtils.cachedDataMessageForHttpCode(503)
        );
        assertEquals(
                "Request timed out. Showing saved data.",
                ApiErrorUtils.cachedDataMessageForThrowable(new SocketTimeoutException())
        );
        assertEquals(
                SyncStatusFormatter.OFFLINE_SAVED_DATA,
                ApiErrorUtils.cachedDataMessageForThrowable(new IOException())
        );
    }
}
