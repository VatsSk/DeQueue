package com.dequeue.common.exception;

public class ShopClosedException extends RuntimeException {
    public ShopClosedException(String message) {
        super(message);
    }
}
