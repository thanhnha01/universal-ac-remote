# UI / UX Production Contract

## Mục tiêu

Tài liệu này là contract production cho giao diện Universal A/C Remote. Khi task yêu cầu redesign hoặc sửa UX, Codex phải sửa Compose production code thật và nối với runtime hiện có. Preview/mockup chỉ hỗ trợ phát triển, không được coi là hoàn tất.

## Visual direction

- Material 3, nền trắng/xanh rất nhạt, xanh dương/cyan là màu chính, navy cho text.
- Card bo tròn lớn, border/shadow nhẹ, spacing thoáng, typography rõ.
- Touch target tối thiểu 48dp, portrait-first, one-handed friendly, responsive từ khoảng 360dp trở lên.
- Không lạm dụng gradient, blur hoặc animation.
- Nếu người dùng cung cấp ảnh tham chiếu, bám sát hierarchy, spacing, component density và visual tone của ảnh đó.

## Navigation chính

Bottom navigation production:
- Trang chủ
- Remote
- Dò tìm
- Cài đặt

Không để debug/diagnostic screen chiếm vai trò flow chính; diagnostics chỉ là công cụ phụ trong Settings.

## Thêm máy lạnh

Màn Add A/C phải ưu tiên:
1. Tìm hãng/model/remote.
2. Tôi biết model.
3. Tôi không biết model → Dò remote 1000-in-1.
4. Nhập file .ir.

Search result mặc định ưu tiên profile transmit được. Profile không transmit được không được chiếm phần lớn màn hình; có thể ẩn dưới mục phụ hoặc hiển thị disabled với lý do ngắn.

Không dùng thuật ngữ kỹ thuật làm headline cho người dùng.

## Dò 1000-in-1

Flow bắt buộc:
1. Chuẩn bị máy lạnh.
2. Chọn candidate transmit được.
3. Safe probe không được bắt đầu bằng OFF nếu có lựa chọn ON/stateful.
4. Phát thử một candidate và chờ người dùng.
5. Có phản ứng → khóa candidate.
6. Wizard xác minh từng bước.
7. Thứ tự ưu tiên: Temperature → Mode → Fan → Swing dọc → Swing ngang → Power cuối.
8. FULL/PARTIAL/NO MATCH rõ ràng.
9. Có thể thử candidate kế tiếp nếu chỉ partial.

Không phát hàng loạt mã tự động. Không hiển thị raw exception.

## Remote control

Remote phải hoạt động giống remote vật lý:
- Power: bấm là phát ngay.
- Temp +/-: cập nhật desired state và phát ngay.
- Mode/Fan/Swing/Special: bấm/toggle là phát ngay.
- Protocol stateful gửi full state frame theo state mới.
- Raw profile gửi command tương ứng.
- Không có nút "Gửi lệnh" chung trong remote production, trừ khi một profile cụ thể bắt buộc transactional apply và có lý do kỹ thuật được document.

Chỉ render capability profile thực sự hỗ trợ. Không hiển thị room temperature nếu không có nguồn đo thật.

## Saved Remote / Device Detail

- Tên người dùng đặt tách khỏi model kỹ thuật.
- Hiển thị trạng thái verified/partial/unverified và transmittable rõ ràng, không trộn với trạng thái hardware IR.
- Rename/Delete/Open/Retest phải hoạt động thật.
- Profile đã save nhưng không còn transmit được phải chuyển sang trạng thái cần kiểm tra lại, không được vẫn ghi "Đã xác minh".

## Settings

Chỉ giữ CTA có chức năng thật:
- trạng thái IR hardware
- kiểm tra cập nhật app
- diagnostics
- import .ir
- provenance/source info khi cần

Không hiển thị database-only updater nếu backend/runtime chưa hỗ trợ.

## Error UX

End-user message phải ngắn, tiếng Việt, hướng hành động. Technical exception để log/test nội bộ. Ví dụ:
- "Không thể phát tín hiệu IR."
- "Hồ sơ này chưa thể dùng trên thiết bị."
- "Không tìm thấy hồ sơ có thể phát cho hãng này."

## Definition of Done cho UI task

Một UI task chỉ hoàn tất khi:
- production screen đã thay đổi thật, không chỉ Preview;
- dữ liệu lấy từ runtime/catalog thật;
- action chính hoạt động và không có CTA giả;
- unit/lint/assembleDebug PASS;
- screen/flow bị ảnh hưởng được kiểm tra functional;
- phần cần thiết bị thật được ghi riêng là REQUIRES REAL DEVICE TEST.
