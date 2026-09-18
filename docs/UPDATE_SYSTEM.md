# Update System

## Hai kênh độc lập

Hệ thống cập nhật chia thành:

1. **App update:** kiểm tra GitHub Releases và thông báo APK mới.
2. **Database update (M10):** cập nhật bundle IR đã ký mà không cài APK mới.

Hai kênh dùng manifest, version và trust policy riêng. Database update không được phép tải/thi hành native code hoặc thay đổi protocol engine executable.

## App update qua GitHub Releases

### Luồng dự kiến

1. Chạy khi người dùng yêu cầu hoặc theo lịch thưa, tôn trọng thiết lập opt-in và điều kiện mạng.
2. Gọi GitHub Releases API qua HTTPS cho repository chính xác đã cấu hình cố định.
3. Bỏ qua draft; chỉ xét prerelease nếu người dùng chọn kênh prerelease.
4. Parse tag/version nghiêm ngặt và so với version đang cài, không so chuỗi tùy ý.
5. Kiểm tra asset đúng tên/content type và metadata release mong đợi.
6. Thông báo release notes, version và link; người dùng chủ động tải/cài.
7. Android Package Installer và platform signature check là lớp xác thực cuối khi update cùng application ID/signing certificate.

Không cài im lặng, không yêu cầu quyền Accessibility/Device Owner để né xác nhận của Android. Nếu nguồn cài đặt ngoài bị chặn, UI chỉ hướng dẫn người dùng tới thiết lập hệ thống phù hợp.

### Bảo mật và riêng tư

- Không gửi danh sách thiết bị/profile IR lên GitHub.
- Timeout, rate limit/backoff và cache ETag/last-check để tránh gọi API quá mức.
- Không tin release title/body làm dữ liệu điều khiển; render text an toàn.
- Không chọn asset theo “file APK đầu tiên”; kiểm tra quy ước tên và checksum/provenance asset.
- URL tải phải thuộc host GitHub/asset host được allowlist theo policy triển khai.
- Thất bại kiểm tra update không được ảnh hưởng chức năng remote offline.

GitHub Releases không thay thế Android APK signing. Release checksum giúp phát hiện lỗi/tamper khi tải; khả năng update hợp lệ vẫn phụ thuộc signing certificate của app được bảo vệ và ổn định.

## Future database-only updater

### Artifact

Một database release dự kiến gồm:

- manifest canonical có `formatVersion`, `databaseVersion`, `minAppVersion`, `maxAppVersion` nếu cần;
- full upstream provenance/commit SHA hoặc hash tham chiếu manifest nguồn;
- bundle content SHA-256, kích thước và compression;
- chữ ký detached bằng khóa phát hành database riêng;
- bundle normalized chỉ chứa data, không chứa executable/shared library/script.

### Luồng cài đặt an toàn

1. Tải manifest và signature vào staging.
2. Xác minh chữ ký bằng public key pin trong APK.
3. Kiểm tra version monotonic/rollback policy, schema compatibility, kích thước và hash.
4. Tải bundle với giới hạn kích thước; xác minh hash trước giải nén.
5. Giải nén an toàn: cấm absolute path, traversal, symlink và decompression bomb.
6. Validate toàn bộ schema, record và transmission policy trong staging.
7. Ghi atomically rồi chuyển con trỏ active; giữ last-known-good để rollback.
8. Nếu load/smoke validation thất bại, tự quay lại bundle trước và ghi lỗi cục bộ.

### Trust và key rotation

Khóa ký database không phải APK keystore. Public key và key ID được pin trong app; rotation cần manifest được ký bởi khóa cũ hoặc cập nhật APK với trust set mới. GitHub Release/token không đủ để tạo bundle hợp lệ nếu không có private signing key.

### Tương thích

App phải luôn có database bundled tối thiểu hoặc last-known-good để hoạt động offline. Database mới không được yêu cầu engine/schema mà app cũ không hiểu. Record không hợp lệ bị từ chối theo toàn bundle hoặc partition được thiết kế rõ; không dùng trạng thái nửa cập nhật.

## Những vấn đề để lại cho milestone sau

- Repository/release channel cuối cùng cho database và quy ước asset.
- Thuật toán/chương trình ký manifest cụ thể và quy trình giữ/rotate key.
- Chính sách retention/rollback version và delta update.
- UX opt-in, lịch kiểm tra và hỗ trợ prerelease.

