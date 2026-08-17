package com.rikkei.incidentworkflow.service;

import com.rikkei.incidentworkflow.entity.IncidentReport;
import com.rikkei.incidentworkflow.entity.NotificationStatus;
import com.rikkei.incidentworkflow.entity.UrgencyLevel;
import com.rikkei.incidentworkflow.repository.IncidentReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * IncidentETLService - dieu phoi workflow su co khan cap khep kin (End-to-End).
 *
 * Kien truc chiu loi tach 2 pha:
 *   Phase 1: Boc tach (ETL) + luu su co vao DB  -> PHAI thanh cong truoc.
 *   Phase 2: Kiem duyet muc do + phat canh bao  -> duoc co lap bang try-catch,
 *            neu that bai KHONG lam mat du lieu Phase 1, chi cap nhat trang thai FAILED.
 *
 * Nho tach 2 pha, loi o khau phat canh bao khong bao gio keo theo rollback ban ghi su co.
 */
@Service
public class IncidentETLService {

    private static final Logger log = LoggerFactory.getLogger(IncidentETLService.class);

    private final IncidentReportRepository repository;
    private final ConsoleAlertService consoleAlertService;

    public IncidentETLService(IncidentReportRepository repository,
                              ConsoleAlertService consoleAlertService) {
        this.repository = repository;
        this.consoleAlertService = consoleAlertService;
    }

    /**
     * Xu ly tin nhan bao cao tu tai xe theo workflow khep kin.
     *
     * @param rawMessage      tin nhan tho tu tai xe
     * @param simulateFailure mo phong loi phat canh bao de kiem chung chiu loi
     * @return ban ghi su co da luu (voi notificationStatus cuoi cung)
     */
    public IncidentReport processIncident(String rawMessage, boolean simulateFailure) {
        // ===== PHASE 1: ETL + Save DB (transaction rieng, phai thanh cong) =====
        IncidentReport saved = extractAndSave(rawMessage);
        log.info("[PHASE 1] Da luu su co vao DB: {}", saved);

        // ===== Check Urgency =====
        boolean needAlert = saved.getUrgencyLevel() == UrgencyLevel.HIGH
                || saved.getUrgencyLevel() == UrgencyLevel.CRITICAL;

        if (!needAlert) {
            // Khong can phat canh bao -> danh dau NOT_REQUIRED
            saved.setNotificationStatus(NotificationStatus.NOT_REQUIRED);
            updateStatus(saved);
            log.info("[CHECK] Muc do {} khong can canh bao -> NOT_REQUIRED",
                    saved.getUrgencyLevel());
            return saved;
        }

        // ===== PHASE 2: Phat canh bao (CO LAP LOI bang try-catch) =====
        try {
            consoleAlertService.dispatchRedAlert(saved, simulateFailure);
            saved.setNotificationStatus(NotificationStatus.SUCCESS);
            log.info("[PHASE 2] Phat canh bao THANH CONG -> SUCCESS (id={})", saved.getId());
        } catch (AlertDispatchException ex) {
            // Co lap ngoai le: KHONG nem lai, KHONG anh huong du lieu Phase 1.
            // Ghi log loi chi tiet + danh dau FAILED de ky thuat vien tra cuu, xu ly thu cong.
            saved.setNotificationStatus(NotificationStatus.FAILED);
            log.error("[PHASE 2] Phat canh bao THAT BAI cho su co id={} (order={}). "
                            + "Du lieu su co van duoc giu trong DB, danh dau FAILED de xu ly thu cong.",
                    saved.getId(), saved.getOrderCode(), ex);
        }

        // Cap nhat trang thai thong bao cuoi cung (Update Status Phase 2)
        updateStatus(saved);
        return saved;
    }

    /**
     * PHASE 1 - transaction doc lap: boc tach thong tin va luu su co.
     * Neu buoc nay loi thi rollback binh thuong (chua co gi de mat).
     */
    @Transactional
    protected IncidentReport extractAndSave(String rawMessage) {
        // Boc tach don gian tu tin nhan tho (mo phong ETL).
        // Trong du an that, day la noi goi LLM Structured Output de trich xuat.
        IncidentReport report = parse(rawMessage);
        return repository.save(report);
    }

    /**
     * Cap nhat trang thai thong bao trong transaction rieng (Phase 2 persistence).
     */
    @Transactional
    protected void updateStatus(IncidentReport report) {
        repository.save(report);
    }

    /**
     * Boc tach thong tin su co tu tin nhan tho.
     * Dinh dang mo phong: "order=ORD123;plate=29A-12345;urgency=CRITICAL;desc=Xe lat"
     * Cac truong thieu se dung gia tri mac dinh phong thu.
     */
    private IncidentReport parse(String rawMessage) {
        String orderCode = extract(rawMessage, "order", "UNKNOWN");
        String plate = extract(rawMessage, "plate", "UNKNOWN");
        String desc = extract(rawMessage, "desc", rawMessage);
        UrgencyLevel urgency = parseUrgency(extract(rawMessage, "urgency", "LOW"));

        return new IncidentReport(orderCode, plate, urgency, desc, LocalDateTime.now());
    }

    private String extract(String raw, String key, String defaultValue) {
        if (raw == null) {
            return defaultValue;
        }
        for (String part : raw.split(";")) {
            String trimmed = part.trim();
            if (trimmed.toLowerCase().startsWith(key.toLowerCase() + "=")) {
                String value = trimmed.substring(key.length() + 1).trim();
                return value.isEmpty() ? defaultValue : value;
            }
        }
        return defaultValue;
    }

    private UrgencyLevel parseUrgency(String value) {
        try {
            return UrgencyLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Khong nhan dien duoc muc do khan cap '{}', mac dinh LOW", value);
            return UrgencyLevel.LOW;
        }
    }
}
