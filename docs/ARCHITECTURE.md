# Architecture

## Mục tiêu kiến trúc

Thiết kế tách domain điều khiển máy lạnh khỏi Android framework, định dạng upstream và implementation C/C++. Nhờ đó cùng một `AcState` có thể đi qua protocol encoder, profile hoặc raw fallback mà UI không cần biết chi tiết timing.

## Sơ đồ thành phần

```text
Compose UI
   |
Application / use cases
   |
Domain: AcState, capability, device/profile identity
   |
IR resolution: protocol engine -> profile engine -> raw fallback
   |
IrTransmission + validator + safety policy
   |
Android IR adapter (ConsumerIrManager)

Importers --------> normalized IR repository
Upstream tooling --> validated, provenance-bearing bundles
JNI bridge -------> optional native protocol adapter
Update clients ---> app release metadata / future DB bundles
```

## Module boundary dự kiến

Ở M1, repository có duy nhất module `app`; các module còn lại là ranh giới thiết kế cho milestone sau và chưa được tạo:

| Khu vực | Trách nhiệm | Không được chứa |
|---|---|---|
| `app` | Compose, navigation, Android lifecycle, dependency wiring | Protocol constant/timing |
| `domain` | `AcState`, capability, use case, lỗi domain | Android framework, JNI |
| `ir-core` | `IrTransmission`, validation, encoder contracts | UI, network |
| `ir-android` | Adapter `ConsumerIrManager`, hardware capability | Upstream parsers |
| `ir-protocols` | Protocol encoders Kotlin và registry | Android UI |
| `ir-native` | JNI facade và native adapter được pin | Domain/UI coupling |
| `ir-import` | Parser Flipper và định dạng bổ sung | Phát IR trực tiếp |
| `ir-data` | Profile/raw repository, schema, provenance | Network update logic |
| `update` | GitHub Release check và future DB updater | Signing secrets |
| `tools` | Pin/fetch/normalize/validate upstream ngoài runtime | Dynamic fetch trong build release |

Các module thực tế chỉ được tạo khi milestone yêu cầu; tránh tạo cấu trúc rỗng trước thời điểm đó.

## Mô hình domain

### `AcState`

`AcState` là trạng thái mong muốn độc lập protocol. Các trường dự kiến:

- power; mode (`AUTO`, `COOL`, `DRY`, `FAN`, `HEAT` khi profile hỗ trợ);
- target temperature với đơn vị rõ ràng;
- fan speed; vertical/horizontal swing;
- turbo/quiet/eco/light/filter/clean/sleep;
- timer hoặc clock chỉ khi protocol/profile mô hình hóa đáng tin cậy;
- tập capability và giá trị `unknown/unsupported` tách biệt.

Không ép protocol phải hỗ trợ mọi trường. Encoder trả lỗi có kiểu hoặc kết quả giảm cấp rõ ràng; không âm thầm tạo giá trị protocol.

### `IrTransmission`

Giá trị bất biến, tối thiểu gồm carrier frequency Hz, chuỗi mark/space duration theo microsecond, repeat policy, nguồn/provenance và metadata phục vụ kiểm thử. Validator từ chối duration âm/rỗng, độ dài hoặc tổng thời gian vượt giới hạn, carrier ngoài capability được báo cáo/chính sách, repeat không hợp lệ và số liệu tràn kiểu.

## Luồng encode và fallback

1. Use case nhận `AcState` và target profile.
2. Resolver ưu tiên encoder protocol có cấu trúc và đã xác minh.
3. Nếu không có encoder, resolver tìm profile ánh xạ trạng thái sang raw command hợp lệ.
4. Raw command chỉ được dùng nếu đã normalize, validate và có provenance.
5. `IrTransmissionValidator` kiểm tra lần cuối.
6. Android adapter chuyển durations sang kiểu mà `ConsumerIrManager.transmit()` yêu cầu và phát một cách có kiểm soát.

Fallback không được che lỗi encoder. Lý do chọn nhánh và mức capability phải quan sát được trong log cục bộ đã loại bỏ dữ liệu nhạy cảm.

## Android IR boundary

`ConsumerIrManager` được bọc sau interface để domain/unit test không phụ thuộc Android. Adapter chịu trách nhiệm:

- phát hiện feature/service có tồn tại;
- đọc carrier frequency range khi API/thiết bị hỗ trợ;
- kiểm tra conversion microsecond sang `Int` không overflow;
- serialize lệnh phát để tránh chồng tín hiệu;
- trả lỗi phân loại: không có emitter, frequency không hỗ trợ, pattern không hợp lệ, lỗi hệ thống.

Khả năng OnePlus 15 phải được đo và ghi nhận ở M1; tài liệu này không khẳng định hành vi vendor API trước khi kiểm tra thực tế.

## JNI/NDK boundary

IRremoteESP8266 được xem là nguồn implementation/protocol, không phải runtime dependency tự động. Proof of concept sẽ chọn đúng một protocol AC, pin full commit SHA và chỉ biên dịch tập file cần thiết.

JNI facade nhận DTO ổn định, primitive-only từ Kotlin và trả encoded durations/metadata hoặc lỗi có kiểu. C++ exception, pointer và class upstream không vượt qua ABI. Build pin NDK/CMake, khai báo ABI hỗ trợ, có unit/golden test ở cả biên native và Kotlin. License/notice phải được review trước khi đưa code vào.

## Dữ liệu và provenance

Mỗi record normalized mang `sourceId`, upstream URL, full commit SHA, source path, source record identity, license status, importer/schema version và content hash. Dữ liệu thiếu provenance hoặc chưa rõ giấy phép không được đóng gói/phát hành.

Ứng dụng chỉ đọc bundle đã được tạo và validate trước; release build không clone upstream hoặc gọi mạng để tạo database.

## Update boundary

App updater và database updater là hai luồng độc lập:

- App updater chỉ đọc metadata GitHub Releases, so version tương thích và đưa người dùng tới APK release đã ký.
- Future database updater tải manifest/bundle có chữ ký, xác minh hash, schema compatibility, cài atomically và rollback được.

Chi tiết tại [UPDATE_SYSTEM.md](UPDATE_SYSTEM.md).

## Quyết định kiến trúc M0

- Kotlin/Compose cho app; Android framework bị cô lập ở adapter.
- `AcState` và `IrTransmission` là hai contract chuẩn hóa trung tâm.
- Protocol-first, profile/raw fallback.
- JNI là adapter tùy chọn, không chi phối domain.
- Upstream được pin và nhập offline qua PR; không dynamic latest trong build.
- Thiết kế không chứa ESP32 hoặc phần cứng IR ngoài.
