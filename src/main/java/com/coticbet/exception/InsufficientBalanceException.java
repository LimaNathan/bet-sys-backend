package com.coticbet.exception;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException() {
        super("Insufficient balance for this operation");
    }

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
