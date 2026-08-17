# Bài 5: Thiết kế và hiện thực hóa Workflow sự cố khẩn cấp (Console Alert)

Project Spring Boot triển khai workflow tự động hóa khép kín (End-to-End) cho **AI Logistics Incident Reporter**: ETL bóc tách sự cố → lưu DB → kiểm duyệt mức độ khẩn cấp → phát cảnh báo đỏ ra Console → cập nhật trạng thái thông báo, với **kiến trúc chịu lỗi tách 2 pha**.

## Yêu cầu đã đáp ứng

- ✅ Sơ đồ luồng ASCII + thuyết minh thiết kế chịu lỗi: [THIET_KE_GIAI_PHAP.md](./THIET_KE_GIAI_PHAP.md)
- ✅ `ConsoleAlertService` phát cảnh báo đỏ ra Console bằng SLF4J, hỗ trợ mô phỏng lỗi
- ✅ `IncidentReport` entity có `notificationStatus` (PENDING / SUCCESS / FAILED / NOT_REQUIRED)
- ✅ `IncidentETLService` tách 2 pha transaction + try-catch cô lập ngoại lệ
- ✅ Dữ liệu sự cố vẫn được lưu (trạng thái FAILED) khi phát cảnh báo lỗi
- ✅ `DemoRunner` tự chạy 3 kịch bản để tạo minh chứng log

## Yêu cầu môi trường

- Java 21
- Maven 3.9+
- Không cần DB ngoài — dùng H2 in-memory

## Cách chạy

```bash
mvn spring-boot:run
```

Khi khởi động, `DemoRunner` tự chạy 3 kịch bản và in log ra console. Ngoài ra có thể gọi thủ công:

```bash
# Trường hợp thành công (CRITICAL)
curl -X POST "http://localhost:8080/api/v1/incident/report" \
  -H "Content-Type: text/plain" \
  -d "order=ORD100;plate=29A-99999;urgency=CRITICAL;desc=Xe lat tren cao toc"

# Trường hợp giả lập lỗi (dữ liệu vẫn lưu, status=FAILED)
curl -X POST "http://localhost:8080/api/v1/incident/report?simulateFailure=true" \
  -H "Content-Type: text/plain" \
  -d "order=ORD101;plate=51B-88888;urgency=HIGH;desc=Chay khoang hang"
```

Xem DB tại `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:incidentdb`).

## Minh chứng chạy thực tế (mẫu log console)

> Thay bằng log thật sau khi bạn chạy `mvn spring-boot:run`.

### Trường hợp 1 — Phát thông báo THÀNH CÔNG (CRITICAL)

```
INFO  --- DemoRunner : ========== DEMO 1: CRITICAL - phat canh bao THANH CONG ==========
INFO  --- IncidentETLService : [PHASE 1] Da luu su co vao DB: IncidentReport{id=1, orderCode='ORD001', vehiclePlate='29A-11111', urgencyLevel=CRITICAL, notificationStatus=PENDING}
ERROR --- ConsoleAlertService :
==================== 🔴 CANH BAO DO KHAN CAP 🔴 ====================
  Muc do khan cap : CRITICAL
  Ma don hang     : ORD001
  Bien so xe      : 29A-11111
  Thoi gian       : 2026-08-18T00:12:03.123
  Mo ta su co     : Xe container mat phanh tren cao toc
  >> YEU CAU NHAN VIEN DIEU HANH XU LY NGAY <<
====================================================================
INFO  --- IncidentETLService : [PHASE 2] Phat canh bao THANH CONG -> SUCCESS (id=1)
```

### Trường hợp 2 — Phát thông báo THẤT BẠI, dữ liệu vẫn lưu với FAILED (HIGH)

```
INFO  --- DemoRunner : ========== DEMO 2: HIGH - gia lap LOI phat canh bao ==========
INFO  --- IncidentETLService : [PHASE 1] Da luu su co vao DB: IncidentReport{id=2, orderCode='ORD002', vehiclePlate='51B-22222', urgencyLevel=HIGH, notificationStatus=PENDING}
ERROR --- IncidentETLService : [PHASE 2] Phat canh bao THAT BAI cho su co id=2 (order=ORD002). Du lieu su co van duoc giu trong DB, danh dau FAILED de xu ly thu cong.
com.rikkei.incidentworkflow.service.AlertDispatchException: Thiet bi phat tin hieu dang ban - khong the phat canh bao cho don ORD002
    at com.rikkei.incidentworkflow.service.ConsoleAlertService.dispatchRedAlert(ConsoleAlertService.java:...)
    ...
# -> Ban ghi id=2 van ton tai trong DB voi notificationStatus=FAILED
```

### Trường hợp 3 — Không cần cảnh báo (LOW)

```
INFO  --- DemoRunner : ========== DEMO 3: LOW - khong can canh bao ==========
INFO  --- IncidentETLService : [PHASE 1] Da luu su co vao DB: IncidentReport{id=3, orderCode='ORD003', ..., notificationStatus=PENDING}
INFO  --- IncidentETLService : [CHECK] Muc do LOW khong can canh bao -> NOT_REQUIRED
```

## Cấu trúc project

```
bt5/
├── pom.xml
├── README.md
├── THIET_KE_GIAI_PHAP.md
└── src/main/
    ├── java/com/rikkei/incidentworkflow/
    │   ├── IncidentWorkflowApplication.java
    │   ├── DemoRunner.java
    │   ├── controller/IncidentController.java
    │   ├── entity/
    │   │   ├── IncidentReport.java
    │   │   ├── NotificationStatus.java
    │   │   └── UrgencyLevel.java
    │   ├── repository/IncidentReportRepository.java
    │   └── service/
    │       ├── ConsoleAlertService.java
    │       ├── AlertDispatchException.java
    │       └── IncidentETLService.java
    └── resources/application.yml
```

## Đẩy lên GitHub và nộp bài

```bash
cd bt5
git init
git add .
git commit -m "Bai 5: Workflow su co khan cap voi kien truc chiu loi"
git branch -M main
git remote add origin https://github.com/<username>/incident-workflow-bt5.git
git push -u origin main
```

Nộp link repository riêng cho bài này trên portal Rikkei.
