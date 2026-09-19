# AGENTS.md

## Phạm vi dự án

Repository này dành riêng cho **Universal A/C Remote for Android**: ứng dụng Android cá nhân sử dụng IR blaster tích hợp của thiết bị (mục tiêu đầu tiên là OnePlus 15) để điều khiển máy lạnh.

Các ràng buộc không được phá vỡ:

- Chỉ Android; không đưa ESP32 hoặc phần cứng IR ngoài vào dự án.
- GitHub là repository chính. GitHub Actions build APK; GitHub Releases phát hành APK đã ký.
- Không commit signing key, password, token hoặc secret dưới bất kỳ hình thức nào.

## Trạng thái milestone hiện tại

- Repository đã qua milestone M0.
- Các ràng buộc dành riêng cho M0 không được áp dụng cho task ở milestone/phạm vi hiện tại, trừ khi task explicitly yêu cầu quay lại M0.
- Được phép thêm hoặc sửa production Android source code khi cần để hoàn thành task hiện tại.
- Không được dùng quy tắc M0 cũ để né việc sửa logic, UI, scanner, transmitter, persistence, updater hoặc các phần production khác nằm đúng trong scope task.
- Mọi thay đổi vẫn phải tuân thủ nguyên tắc: sửa tối thiểu cần thiết, không mở rộng feature ngoài yêu cầu, không refactor không liên quan.

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
- Nếu người dùng yêu cầu trực tiếp sửa/nâng cấp UI/UX, đó là scope hiện tại và phải được triển khai ngay; không được viện ROADMAP/M11 để hoãn hoặc chỉ chỉnh tài liệu.
- Với task UI/UX, phải sửa production Compose code và nối vào dữ liệu/runtime thật; không được chỉ thêm mockup, Preview, placeholder hoặc CTA không hoạt động.
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

- ROADMAP là kế hoạch tham chiếu, không phải cơ chế chặn một yêu cầu trực tiếp của người dùng. Task được yêu cầu rõ ràng có quyền thực hiện phần UI/UX, scanner, transmitter hoặc persistence cần thiết dù roadmap lịch sử đặt phần polish ở milestone sau.
- Chỉ làm đúng milestone/task được yêu cầu và không tự triển khai feature khác ngoài scope.
- Nếu một task được xác định rõ là M0, thì M0 chỉ gồm tài liệu, chính sách repository và thiết kế CI/CD; không dò phần cứng, không phát IR, không nhập database lớn, không tạo ứng dụng production.
- Quy tắc M0 ở dòng trên chỉ áp dụng cho task M0; không phải ràng buộc toàn cục của repository sau khi đã qua M0.
- Mọi thay đổi ảnh hưởng an toàn phát IR, signing, updater hoặc supply chain phải được nêu rõ trong pull request.
