package com.rikkei.incidentworkflow.service;

/**
 * Ngoai le chuyen biet cho tinh huong thiet bi phat tin hieu ban / loi phat canh bao.
 * Duoc ConsoleAlertService nem ra de IncidentETLService bat va co lap.
 */
public class AlertDispatchException extends RuntimeException {

    public AlertDispatchException(String message) {
        super(message);
    }

    public AlertDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
