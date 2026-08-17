# Thiết kế giải pháp: Workflow sự cố khẩn cấp chịu lỗi (Console Alert)

## 1. Sơ đồ luồng xử lý dữ liệu (ASCII Flow Diagram)

```
   Tin nhắn thô từ tài xế (rawMessage)
                │
                ▼
   ┌─────────────────────────────┐
   │      IncidentETLService     │
   │   (điều phối toàn workflow)  │
   └─────────────────────────────┘
                │
                ▼
   ┌─────────────────────────────┐        ╔══════════════════════════╗
   │  ETL: bóc tách thông tin     │        ║   PHASE 1 (Transaction 1) ║
   │  order, plate, urgency,      │◀──────▶║   phải THÀNH CÔNG trước   ║
   │  desc, time                  │        ╚══════════════════════════╝
   └─────────────────────────────┘
                │
                ▼
   ┌─────────────────────────────┐
   │  SAVE DB (Phase 1)           │
   │  notificationStatus=PENDING  │
   └─────────────────────────────┘
                │
                ▼
        ┌───────────────┐
        │ CHECK URGENCY │
        └───────────────┘
          │           │
   HIGH/CRITICAL    LOW/MEDIUM
          │           │
          │           ▼
          │   ┌──────────────────────┐
          │   │ status=NOT_REQUIRED  │──┐
          │   └──────────────────────┘  │
          ▼                             │
   ┌───────────────────────────────┐   │   ╔══════════════════════════╗
   │  try {                        │   │   ║  PHASE 2 (cô lập bằng     ║
   │    ConsoleAlertService        │   │   ║  try-catch + Transaction 2)║
   │      .dispatchRedAlert()      │   │   ╚══════════════════════════╝
   │  }                            │   │
   └───────────────────────────────┘   │
       │                    │           │
   THÀNH CÔNG            THẤT BẠI        │
       │            (AlertDispatchException)
       ▼                    ▼           │
 ┌─────────────┐    ┌──────────────────┐│
 │ status=     │    │ catch:           ││
 │ SUCCESS     │    │  - log.error(...) ││
 │             │    │  - status=FAILED  ││
 └─────────────┘    │  (KHÔNG rollback  ││
       │            │   dữ liệu Phase 1)││
       │            └──────────────────┘│
       │                    │           │
       └────────┬───────────┴───────────┘
                ▼
   ┌─────────────────────────────┐        ╔══════════════════════════╗
   │  UPDATE STATUS (Phase 2)     │        ║  Ghi trạng thái cuối cùng ║
   │  SUCCESS / FAILED /          │◀──────▶║  vào DB (SUCCESS/FAILED/   ║
   │  NOT_REQUIRED                │        ║  NOT_REQUIRED)             ║
   └─────────────────────────────┘        ╚══════════════════════════╝
```

## 2. Thuyết minh kiến trúc chịu lỗi

### 2.1. Tách 2 pha (Transaction Isolation)

Workflow được chia thành hai pha ghi DB độc lập:

- **Phase 1 — Lưu sự cố:** `extractAndSave()` chạy trong `@Transactional` riêng, commit ngay khi bóc tách và lưu thành công. Đây là dữ liệu quan trọng nhất — sự cố phải được ghi nhận bất kể chuyện gì xảy ra ở khâu cảnh báo.
- **Phase 2 — Cập nhật trạng thái thông báo:** `updateStatus()` chạy trong transaction riêng, ghi kết quả `SUCCESS` / `FAILED` / `NOT_REQUIRED`.

Vì hai pha không nằm chung một transaction, một lỗi ở Phase 2 **không thể rollback** bản ghi sự cố đã commit ở Phase 1. Đây là điểm mấu chốt đáp ứng yêu cầu "hệ thống vẫn phải lưu thành công thông tin sự cố".

### 2.2. Cô lập ngoại lệ (try-catch isolation)

Khâu phát cảnh báo được bọc trong `try-catch` bắt `AlertDispatchException`:

- Nếu `ConsoleAlertService.dispatchRedAlert()` thất bại (mô phỏng "thiết bị phát tín hiệu bận"), ngoại lệ **không được ném lại** lên trên.
- Trong khối `catch`: ghi `log.error(...)` chi tiết (kèm stack trace) và đặt `notificationStatus = FAILED`.
- Sau đó vẫn gọi `updateStatus()` để lưu trạng thái `FAILED` xuống DB, giúp nhân viên kỹ thuật tra cứu và xử lý thủ công.

Kết quả: lỗi phát cảnh báo bị **cô lập hoàn toàn** trong Phase 2, không lan sang Phase 1 và không làm sập luồng nghiệp vụ.

### 2.3. Vòng đời `notificationStatus`

```
PENDING ──▶ (urgency LOW/MEDIUM) ──▶ NOT_REQUIRED
   │
   └────▶ (urgency HIGH/CRITICAL) ──┬──▶ SUCCESS   (phát cảnh báo OK)
                                    └──▶ FAILED    (phát cảnh báo lỗi, dữ liệu vẫn còn)
```

## 3. Các lớp chính

| Lớp | Vai trò |
|---|---|
| `IncidentReport` (entity) | Bản ghi sự cố + trường `notificationStatus` (PENDING/SUCCESS/FAILED/NOT_REQUIRED) |
| `ConsoleAlertService` | Phát cảnh báo đỏ ra Console bằng SLF4J; hỗ trợ `simulateFailure` để nêm `AlertDispatchException` |
| `IncidentETLService` | Điều phối workflow, tách 2 pha transaction + try-catch cô lập lỗi |
| `IncidentController` | Endpoint `POST /api/v1/incident/report?simulateFailure=` kích hoạt workflow |
| `DemoRunner` | Tự chạy 3 kịch bản demo khi khởi động để tạo minh chứng log |
