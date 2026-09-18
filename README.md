# Universal A/C Remote for Android

Ứng dụng Android cá nhân nhằm điều khiển càng nhiều máy lạnh hồng ngoại càng tốt bằng **IR blaster tích hợp** trên điện thoại, với OnePlus 15 là thiết bị mục tiêu đầu tiên.

## Trạng thái

Repository đang ở **M0 — tài liệu dự án và kiến trúc CI/CD**. Chưa có production Android source code, chưa kiểm tra phần cứng, chưa tải database IR lớn và chưa có APK.

## Ràng buộc cốt lõi

- Android only, Kotlin + Jetpack Compose.
- Phát IR qua Android `ConsumerIrManager`; không dùng phần cứng IR ngoài.
- Không có ESP32 trong thiết kế hoặc implementation.
- GitHub là repository chính.
- GitHub Actions sẽ build APK; GitHub Releases sẽ phân phối APK release đã ký.
- Ứng dụng tương lai sẽ kiểm tra GitHub Releases và thông báo khi có phiên bản mới.
- Toolchain và nguồn IR phải được pin, kiểm chứng và có thể tái tạo.

## Hướng kiến trúc

Luồng điều khiển dự kiến:

`UI/Compose -> use case -> AcState -> protocol/profile encoder -> IrTransmission -> ConsumerIrManager`

Hệ thống ưu tiên protocol encoder có cấu trúc; nếu không có, dùng profile hoặc raw IR đã validate. Một adapter JNI/NDK cô lập sẽ cho phép tái sử dụng phần phù hợp từ IRremoteESP8266 mà không làm rò rỉ kiểu dữ liệu C++ vào domain Kotlin.

Nguồn dữ liệu dự kiến gồm IRremoteESP8266, SmartIR, Flipper IRDB và irplus; LIRC, Broadlink và Pronto có thể được bổ sung về sau. Không nguồn nào được kéo “latest” trực tiếp vào build.

## Tài liệu

- [Mục tiêu sản phẩm](docs/PRODUCT.md)
- [Kiến trúc hệ thống](docs/ARCHITECTURE.md)
- [Mô hình protocol và dữ liệu IR](docs/IR_PROTOCOL.md)
- [Quản trị nguồn upstream](docs/UPSTREAM_SOURCES.md)
- [Thiết kế CI/CD](docs/CI_CD.md)
- [Cập nhật ứng dụng và database](docs/UPDATE_SYSTEM.md)
- [Chiến lược kiểm thử](docs/TESTING.md)
- [Roadmap](docs/ROADMAP.md)

## Nguyên tắc đóng góp

Đọc [AGENTS.md](AGENTS.md) trước khi thay đổi repository. Mỗi pull request chỉ nên giải quyết một nhóm chức năng rõ ràng, tránh thay đổi ngoài phạm vi và không chứa secret hoặc dữ liệu IR không rõ nguồn gốc/giấy phép.

## Phát hành

Khi pipeline được triển khai ở milestone phù hợp, APK release đã ký sẽ chỉ được công bố qua GitHub Releases. Người dùng phải chủ động xác nhận cài đặt; ứng dụng không thiết kế cơ chế cài đặt im lặng.

