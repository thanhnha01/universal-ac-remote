# AGENTS.md

## Phạm vi dự án

Repository này dành riêng cho **Universal A/C Remote for Android**: ứng dụng Android cá nhân sử dụng IR blaster tích hợp của thiết bị (mục tiêu đầu tiên là OnePlus 15) để điều khiển máy lạnh.

Các ràng buộc không được phá vỡ:

- Chỉ Android; không đưa ESP32 hoặc phần cứng IR ngoài vào dự án.
- GitHub là repository chính. GitHub Actions build APK; GitHub Releases phát hành APK đã ký.
- Không commit signing key, password, token hoặc secret dưới bất kỳ hình thức nào.
- Không thêm production Android source code trong milestone M0.

## Quy tắc làm việc bắt buộc

- Chỉ thực hiện thay đổi nhỏ nhất cần thiết cho task hiện tại.
- Không refactor code không liên quan.
- Không tự bịa timing IR, checksum, protocol constant hoặc model mapping.
- Dữ liệu protocol phải lấy từ upstream đáng tin cậy hoặc test fixture đã commit.
- Không quét đệ quy toàn bộ repository upstream lớn nếu không cần.
- Một task chỉ giải quyết một nhóm chức năng rõ ràng.
- Chỉ chạy test liên quan đến code vừa thay đổi.
- Dừng ngay khi đạt acceptance criteria.
- Không tự ý làm milestone tương lai nếu chưa được yêu cầu.
- Không commit signing key, password hoặc secret.
- Không đưa ESP32 vào dự án.

## Kỷ luật nguồn upstream

- Mỗi lần nhập protocol, profile hoặc database phải ghi URL nguồn, commit SHA đầy đủ, giấy phép và đường dẫn/tệp đã dùng.
- Không phụ thuộc nhánh nổi (`main`, `master`) hoặc tag có thể bị di chuyển trong build phát hành.
- Dữ liệu mới phải qua parser validation, schema validation, kiểm tra giới hạn an toàn và fixture/test liên quan trước khi dùng.
- Không tải hoặc vendor toàn bộ database lớn nếu task chỉ cần một tập con đã xác định.
- Không trộn dữ liệu có giấy phép không tương thích; nếu chưa rõ giấy phép, chỉ ghi nhận nguồn và chưa nhập dữ liệu.

## Kỷ luật build và dependency

- Pin Android/Gradle/Kotlin/Compose/NDK và dependency bằng version catalog/lockfile hoặc cơ chế tương đương khi source code được khởi tạo.
- Không dùng dynamic version như `+`, `latest.release`, `latest.integration` hoặc tự resolve phiên bản mới nhất trong mỗi build.
- GitHub Actions bên thứ ba phải pin theo full commit SHA; ghi chú version/tag cạnh SHA để con người đọc được.
- Cập nhật toolchain/dependency chỉ qua Dependabot hoặc pull request riêng, có review và test phù hợp.
- Build release phải có nguồn gốc truy vết được tới Git commit và upstream manifest cụ thể.

## Giới hạn theo milestone

- Chỉ làm đúng milestone/task được yêu cầu và không triển khai trước milestone sau.
- Với M0, chỉ tài liệu, chính sách repository và thiết kế CI/CD; không dò phần cứng, không phát IR, không nhập database lớn, không tạo ứng dụng production.
- Mọi thay đổi ảnh hưởng an toàn phát IR, signing, updater hoặc supply chain phải được nêu rõ trong pull request.

