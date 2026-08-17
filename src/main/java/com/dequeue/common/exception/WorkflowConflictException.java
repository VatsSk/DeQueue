package com.dequeue.common.exception;

/**
 * Thrown when an order state transition is attempted but is not valid
 * per the predefined state machine.
 *
 * Maps to HTTP 409 Conflict.
 */
public class WorkflowConflictException extends RuntimeException {

    public WorkflowConflictException(String message) {
        super(message);
    }
}
