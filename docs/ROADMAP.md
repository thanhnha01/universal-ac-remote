# Roadmap

Các milestone tuần tự; không tự động bắt đầu milestone kế tiếp. Mỗi milestone cần issue/task riêng, acceptance criteria rõ và thay đổi nhỏ nhất cần thiết.

## M0 — Tài liệu dự án và kiến trúc CI/CD

- Tạo product, architecture, IR/source/update/testing/CI documentation.
- Định nghĩa chính sách upstream SHA, reproducible toolchain và Dependabot.
- Thiết kế bốn workflow nhưng chưa cần implementation.
- Không có production Android source, database lớn hoặc kiểm tra phần cứng.

**Exit:** tài liệu nhất quán, repository structure rõ, không có thiết kế ESP32.

## M1 — Kiểm tra IR phần cứng Android/OnePlus 15

- Xác minh `ConsumerIrManager`, feature/service, frequency range và pattern behavior trên thiết bị thật.
- Ghi test matrix theo phiên bản OS và giới hạn quan sát được.
- Không mở rộng protocol/database ngoài fixture tối thiểu phục vụ phép thử đã duyệt.

**Exit:** báo cáo khả năng/giới hạn và quyết định go/no-go cho Android IR adapter.

## M2 — Pipeline build APK release đã ký

- Khởi tạo Android project tối thiểu với toolchain pin.
- Implement `build.yml`, `build-latest.yml` theo contract và `release.yml` với environment approval.
- Build, ký, verify và phát APK thử nghiệm qua GitHub Releases mà không lộ secret.

**Exit:** release APK đã ký truy vết được, checksum/provenance đi kèm.

## M3 — Kiểm tra cập nhật ứng dụng từ GitHub Release

- Client kiểm tra stable/prerelease channel, version và asset an toàn.
- UX thông báo; cài đặt do người dùng xác nhận qua Android.
- Offline/rate-limit/malformed response tests.

**Exit:** phát hiện đúng release mới và không ảnh hưởng remote khi update check lỗi.

## M4 — Raw IR chuẩn hóa + import Flipper `.ir`

- Chốt schema `IrTransmission`, parser giới hạn và provenance model.
- Import tập fixture nhỏ, validate/canonicalize/deduplicate.
- Không tải toàn Flipper IRDB.

**Exit:** fixture `.ir` tạo transmission xác định và malformed input bị từ chối.

## M5 — Proof-of-concept IRremoteESP8266 qua JNI với 1 protocol AC

- Pin một upstream commit và chọn đúng một protocol AC.
- JNI facade tối thiểu, NDK/toolchain pin và golden tests.
- Review license/notice và chỉ lấy file cần thiết.

**Exit:** một `AcState` được encode qua JNI thành transmission khớp vector đã xác minh.

## M6 — Mở rộng protocol engine

- Registry/capability/error model ổn định.
- Thêm protocol từng PR với fixtures/provenance.
- Hỗ trợ Kotlin/native adapter mà domain không phụ thuộc implementation.

**Exit:** nhiều protocol cùng contract, resolver và regression suite.

## M7 — Database profile/raw IR

- Bundle format, provenance, license states và indexed lookup.
- Import chọn lọc SmartIR/Flipper IRDB/irplus qua validation pipeline.
- Implement `upstream-check.yml` ở quyền tối thiểu, không auto-merge.

**Exit:** database có version, audit được và chỉ chứa record validated.

## M8 — Universal AC profile scanner + production UX

- Candidate ranking, rate limit, checkpoint, stop/cancel và resume.
- Hoàn thiện UX production cho Add A/C, Scanner, Verification Wizard, Saved Remote và Remote screen theo [UI_UX.md](UI_UX.md).
- Scanner phải dùng wizard từng bước, safe probe, khóa candidate sau phản ứng và test Power cuối cùng.
- Remote production phải bấm control là phát lệnh ngay; không có nút "Gửi lệnh" chung nếu không có use case kỹ thuật bắt buộc.
- Không để CTA giả, onClick rỗng, placeholder kỹ thuật hoặc màn debug lẫn vào flow người dùng.
- Hardware safety/lifecycle tests.

**Exit:** flow thêm máy → dò/chọn profile → xác minh → lưu → mở remote → điều khiển usable trên thiết bị thật, và visual/interaction đạt tiêu chí UI_UX.md.

## M9 — Import thêm các định dạng IR khác

- Đánh giá/triển khai adapter LIRC, Broadlink hoặc Pronto theo nhu cầu.
- Mỗi format có parser limits, fixtures, provenance và license review.
- Broadlink chỉ là định dạng dữ liệu; không thêm hub/phần cứng ngoài.

**Exit:** format được chọn normalize về cùng `IrTransmission` mà không giảm validation.

## M10 — Database-only updater

- Manifest/bundle có chữ ký, compatibility gate, atomic install và rollback.
- Khóa ký riêng với APK signing key; chống traversal/bomb/tamper.
- Last-known-good và database bundled fallback.

**Exit:** cập nhật database độc lập an toàn, failure tự rollback.

## M11 — Release hardening cuối

- Không phải milestone đầu tiên mới được làm UI. UI production đã phải usable từ M8 và được cải tiến ở mọi task có liên quan.
- Accessibility, localization, performance, battery/network policy, threat review và release hardening.
- Rà visual consistency cuối, nhưng không trì hoãn redesign/UX fix đã được yêu cầu trực tiếp.
- Mở rộng hardware/model compatibility matrix dựa trên bằng chứng.

**Exit:** release candidate ổn định cho sử dụng cá nhân, tài liệu giới hạn rõ ràng.

