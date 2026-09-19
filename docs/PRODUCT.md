# Product Definition

## Tầm nhìn

Universal A/C Remote for Android là ứng dụng cá nhân biến điện thoại Android có IR blaster tích hợp thành remote máy lạnh có độ bao phủ cao, kể cả máy đời cũ, remote hiếm, model OEM/rebrand, thiết bị kén remote và model khó tìm remote thay thế.

## Người dùng và phạm vi

Người dùng chính là chủ thiết bị Android tương thích, trước mắt là OnePlus 15. Ứng dụng chạy cục bộ; GitHub được dùng làm nơi lưu source, build và phát hành. Đây không phải sản phẩm thương mại và không cam kết tương thích với mọi thiết bị Android hoặc mọi máy lạnh.

### Trong phạm vi

- Giao diện Kotlin/Jetpack Compose.
- Kiểm tra khả năng IR và phát qua `ConsumerIrManager`.
- Điều khiển theo trạng thái máy lạnh chuẩn hóa.
- Protocol encoder, profile encoder và raw IR fallback.
- Import raw IR từ Flipper `.ir`, sau đó có thể mở rộng định dạng khác.
- Dò profile an toàn theo kiểu remote máy lạnh “1000 in 1”.
- Kiểm tra phiên bản ứng dụng mới từ GitHub Releases.
- Tương lai cập nhật database IR độc lập với APK.

### Ngoài phạm vi

- iOS, web remote hoặc desktop remote.
- ESP32, dongle IR, Broadlink hub hay phần cứng phát IR ngoài.
- Cloud account, telemetry bắt buộc hoặc backend thương mại.
- Học/thu tín hiệu IR nếu điện thoại không có phần cứng nhận IR phù hợp.
- Cam kết tự động suy ra protocol/timing/checksum chưa được xác minh.

## Năng lực sản phẩm dự kiến

1. Xác định thiết bị có IR emitter và dải carrier frequency được Android báo cáo.
2. Cho người dùng chọn hãng/model/profile hoặc chạy trình dò có kiểm soát.
3. Chuẩn hóa ý định điều khiển thành `AcState`.
4. Encode thành `IrTransmission` qua protocol engine; fallback bằng profile/raw IR.
5. Validate và phát transmission qua adapter Android.
6. Lưu profile hoạt động và nguồn gốc dữ liệu cục bộ.
7. Kiểm tra GitHub Release theo lựa chọn người dùng và thông báo bản mới.

## Chuẩn UI/UX production

Chi tiết bắt buộc nằm tại [UI_UX.md](UI_UX.md). Tóm tắt:

- Giao diện Compose sáng, hiện đại, ưu tiên trắng/xanh nhạt, xanh dương/cyan, card bo tròn, typography rõ và touch target tối thiểu 48dp.
- Flow người dùng phải đơn giản, ưu tiên ngôn ngữ chức năng thay vì thuật ngữ kỹ thuật như `RAW_PROFILE`, `PROTOCOL` hoặc lỗi exception.
- Add A/C tập trung vào ba đường chính: biết model, không biết model → dò 1000-in-1, và nhập file `.ir`.
- Scanner dùng một hành động tại một thời điểm: phát thử → phản ứng/không phản ứng → wizard xác minh từng chức năng. Power kiểm tra cuối.
- Remote hoạt động như remote vật lý: bấm Power/Temp/Mode/Fan/Swing là phát IR ngay nếu profile hỗ trợ; không yêu cầu nút "Gửi lệnh" chung.
- Chức năng không hoạt động hoặc chưa implement phải bị ẩn hoặc ghi rõ chưa khả dụng; không để CTA giả.
- UI chỉ hiển thị capability/status thật từ runtime/catalog, không hardcode dữ liệu mockup.

## Nguyên tắc trải nghiệm và an toàn

- Không phát IR khi người dùng chưa thực hiện hành động rõ ràng.
- Trình dò hiển thị cảnh báo rằng mã thử có thể đổi mode, nhiệt độ, timer hoặc nguồn.
- Có nút dừng tức thời, nhịp phát giới hạn, khoảng nghỉ giữa mã và xác nhận định kỳ.
- Không giả định trạng thái thực của máy lạnh sau khi phát; UI phân biệt “trạng thái mong muốn/đã gửi” với phản hồi thực tế không tồn tại.
- Không quảng bá hỗ trợ model nếu chưa có fixture hoặc xác nhận trên thiết bị thật.

## Tiêu chí thành công dài hạn

- Kiến trúc thêm protocol/profile mà không sửa UI hoặc Android transmitter.
- Mọi dữ liệu IR đang dùng truy vết được tới nguồn và commit SHA.
- Một transmission có thể kiểm thử xác định, không cần phần cứng.
- APK release truy vết được tới commit, toolchain pin và artifact ký.
- Database hỏng/không tương thích không thể thay thế database tốt đang hoạt động.

