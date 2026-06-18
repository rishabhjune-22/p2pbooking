package com.example.roombooking.utils;

public final class UiEvent<T> {

    private final T content;
    private boolean handled = false;

    public UiEvent(T content) {
        this.content = content;
    }

    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }

        handled = true;
        return content;
    }

    public T peekContent() {
        return content;
    }
}
