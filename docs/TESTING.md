# Testing Strategy

## Nguyên tắc

Kiểm thử ưu tiên tính xác định và chỉ chạy phạm vi liên quan đến thay đổi. Không coi “API không ném exception” là bằng chứng máy lạnh nhận đúng lệnh. Mọi tuyên bố tương thích model cần nguồn fixture hoặc xác nhận phần cứng được ghi nhận.

## Kim tự tháp kiểm thử dự kiến

### Unit tests

- `AcState` validation và capability negotiation.
- Encoder cho cùng input tạo cùng frame/transmission.
- Checksum/bit packing/timing so với golden vector có provenance.
- `IrTransmission` validation, overflow và boundary cases.
- Version comparison cho app/database updater.
- Resolver protocol/profile/raw và lỗi giảm cấp.

### Parser/importer tests

- Fixture Flipper `.ir` hợp lệ, malformed, truncated, oversized và token bất thường.
- Canonicalization/deduplication ổn định.
- Path/metadata không tin cậy, duplicate alias và thiếu provenance/license.
- Fuzz/property tests cho parser khi implementation đủ ổn định.
- Golden normalized output có schema/importer version.

### JNI/native tests

- C++ unit test từ upstream vector đã pin.
- Contract test Kotlin/JNI cho conversion, error mapping và ownership bộ nhớ.
- ABI matrix theo ABI app thực sự hỗ trợ.
- Sanitizer/static analysis trong lane phù hợp; không đưa binary chưa test vào APK.

### Android integration tests

- Thiết bị/emulator không có `ConsumerIrManager` trả trạng thái không hỗ trợ sạch.
- Fake adapter xác minh serialize và cancellation của transmission queue.
- Updater UI với fake GitHub response, offline, rate limit, malformed release và prerelease.
- Package/install flow chỉ kiểm tra nơi khả thi; không tự động cấp quyền nhạy cảm.

### Hardware tests

Kiểm tra phần cứng M1 trên OnePlus 15 cần ghi Android/OxygenOS build, trạng thái emitter/frequency ranges, carrier thử, giới hạn pattern, lỗi API và kết quả quan sát bằng thiết bị nhận phù hợp hoặc máy lạnh đã biết. Kết quả phải được ghi nhận sau khi chạy trên thiết bị thật; kết quả build hoặc fake test không thay thế phép đo này.

Kiểm thử tương thích máy lạnh về sau ghi tối thiểu: hãng/model, profile/protocol version, nguồn/commit, lệnh thử, kết quả quan sát, thiết bị/OS phát và ngày kiểm tra. Dữ liệu cá nhân không được ghi.

## Scanner safety tests

- Stop/cancel xóa hàng đợi và ngăn transmission tiếp theo.
- Lifecycle background/rotation không tự tiếp tục phát.
- Rate limit/checkpoint không bị bypass bởi thao tác nhanh.
- Resume cursor không lặp ngoài ý muốn hoặc tự xác nhận candidate.
- Candidate list chỉ gồm record `validated`.

## Update security tests

- Từ chối manifest/bundle sai chữ ký, hash, schema hoặc compatibility range.
- Từ chối downgrade trái policy, path traversal, symlink và archive bomb.
- Atomic activation và rollback khi crash/validation fail.
- App updater bỏ qua draft và xử lý prerelease đúng channel.
- Asset sai tên/host/hash không được đề xuất cài đặt.

## UI/UX acceptance

Với mọi task redesign hoặc sửa flow production:

- Không được kết luận hoàn tất chỉ vì Compose compile.
- Rà toàn bộ clickable action trong các screen bị ảnh hưởng; không có `onClick = {}`, route chết hoặc CTA giả.
- Add A/C, Scanner, Verification Wizard, Saved Remote và Remote phải dùng dữ liệu/runtime thật.
- Remote control phải phát ngay trên thao tác điều khiển theo contract trong `UI_UX.md`.
- Scanner phải kiểm tra safe probe, candidate lock, verification từng bước và Power cuối.
- Technical exception không được hiển thị thô cho người dùng.
- Cần tách rõ `CODE/CI VERIFIED` và `REQUIRES REAL DEVICE TEST`; build xanh không thay thế test OnePlus 15/máy lạnh thật.
- Khi có ảnh tham chiếu từ người dùng, visual comparison với ảnh đó là acceptance criterion, không chỉ là gợi ý.

## CI gates dự kiến

| Thay đổi | Gate tối thiểu |
|---|---|
| Domain/encoder | Unit + golden fixtures liên quan |
| Importer/data | Parser/schema + normalized diff + provenance/license review |
| JNI/native | Native unit + JNI contract + ABI build |
| Android adapter/UI | Unit + Android integration/lint liên quan |
| Updater | Unit + mocked integration + security negative cases |
| Release/toolchain | Clean build + lock verification + APK signature/metadata checks |

## Test data policy

- Fixture nhỏ nhất đủ chứng minh hành vi; không commit nguyên database lớn.
- Mỗi fixture có nguồn, full commit SHA/path hoặc ghi rõ fixture tự tạo để kiểm tra parser không đại diện protocol thật.
- Không sửa golden output chỉ để test xanh nếu chưa giải thích semantic diff.
- Không tạo timing/checksum/model mapping giả rồi dùng như dữ liệu production.
