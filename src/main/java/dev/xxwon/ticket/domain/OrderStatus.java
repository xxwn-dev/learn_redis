package dev.xxwon.ticket.domain;

public enum OrderStatus {
    INIT,
    SUCCESS,
    FAILED,
    FAILED_AT_PRODUCER,
    FAILED_NO_STOCK
}
