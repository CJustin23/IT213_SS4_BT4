package com.rikkei.incidentworkflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * IncidentReport - ban ghi su co Logistics duoc ETL boc tach va luu vao DB.
 * Chua truong notificationStatus de theo doi ket qua phat canh bao (Phase 2).
 */
@Entity
@Table(name = "incident_report")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ma don hang lien quan den su co */
    @Column(name = "order_code")
    private String orderCode;

    /** Bien so xe */
    @Column(name = "vehicle_plate")
    private String vehiclePlate;

    /** Muc do khan cap do ETL xac dinh */
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level")
    private UrgencyLevel urgencyLevel;

    /** Mo ta su co */
    @Column(name = "description", length = 2000)
    private String description;

    /** Thoi diem xay ra su co */
    @Column(name = "incident_time")
    private LocalDateTime incidentTime;

    /** Trang thai phat thong bao - trung tam cua yeu cau chiu loi */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status")
    private NotificationStatus notificationStatus;

    public IncidentReport() {
    }

    public IncidentReport(String orderCode, String vehiclePlate, UrgencyLevel urgencyLevel,
                          String description, LocalDateTime incidentTime) {
        this.orderCode = orderCode;
        this.vehiclePlate = vehiclePlate;
        this.urgencyLevel = urgencyLevel;
        this.description = description;
        this.incidentTime = incidentTime;
        // Mac dinh PENDING khi vua boc tach xong, truoc khi quyet dinh phat canh bao
        this.notificationStatus = NotificationStatus.PENDING;
    }

    // Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public UrgencyLevel getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(UrgencyLevel urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getIncidentTime() {
        return incidentTime;
    }

    public void setIncidentTime(LocalDateTime incidentTime) {
        this.incidentTime = incidentTime;
    }

    public NotificationStatus getNotificationStatus() {
        return notificationStatus;
    }

    public void setNotificationStatus(NotificationStatus notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    @Override
    public String toString() {
        return "IncidentReport{id=" + id
                + ", orderCode='" + orderCode + '\''
                + ", vehiclePlate='" + vehiclePlate + '\''
                + ", urgencyLevel=" + urgencyLevel
                + ", notificationStatus=" + notificationStatus
                + '}';
    }
}
