package com.rikkei.incidentworkflow.entity;

/**
 * Trang thai phat thong bao canh bao cho su co.
 *
 * - PENDING:      da luu su co, dang cho / chuan bi phat canh bao
 * - SUCCESS:      phat canh bao thanh cong
 * - FAILED:       phat canh bao that bai (nhung su co van da luu) -> can xu ly thu cong
 * - NOT_REQUIRED: muc do khong phai HIGH/CRITICAL nen khong can phat canh bao
 */
public enum NotificationStatus {
    PENDING,
    SUCCESS,
    FAILED,
    NOT_REQUIRED
}
