package com.fastchar.push.exception;

public class FastPushException  extends RuntimeException{
    private static final long serialVersionUID = 3521086318834696090L;

    public FastPushException() {
        super();
    }

    public FastPushException(String message) {
        super(message);
    }

    public FastPushException(String message, Throwable cause) {
        super(message, cause);
    }

    public FastPushException(Throwable cause) {
        super(cause);
    }

}
