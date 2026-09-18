# CI/CD Design

## Mục tiêu

GitHub Actions sẽ tạo APK có thể tái tạo ở mức thực tế, tách build kiểm tra khỏi signing/release và không lấy dependency/toolchain “latest” trong mỗi lần chạy. GitHub Releases là kênh phát hành APK đã ký.

Workflow kiểm tra `.github/workflows/build.yml` chạy trên pull request/push. `.github/workflows/build-latest.yml` chỉ đọc branch tip của nguồn ACTIVE/được chấp thuận mỗi ngày và tạo report; workflow này không promote SHA, sửa lock, ký hay phát hành. `.github/workflows/release.yml` chỉ phát hành khi push tag SemVer `vMAJOR.MINOR.PATCH`.

## Nguyên tắc chung

- Actions pin bằng full commit SHA, kèm comment tag/version để dễ review.
- Pin JDK, Gradle wrapper distribution checksum, Android Gradle Plugin, Kotlin, Compose, Android SDK/NDK/CMake và dependencies.
- Không dùng dynamic version hoặc tải upstream IR trong build APK.
- Quyền mặc định `contents: read`; chỉ job release có quyền ghi cần thiết.
- `pull_request` từ fork/untrusted code không được nhận signing secrets.
- Cache key bao gồm lockfiles/wrapper/toolchain; cache không là nguồn chân lý.
- Artifact có checksum, commit SHA, version và build metadata; log không in secret.
- Concurrency hủy build kiểm tra cũ, nhưng không hủy release đang chạy.

## `build.yml`

**Mục đích:** kiểm tra pull request và push tới nhánh chính.

- Trigger: `pull_request`, `push` vào nhánh mặc định, `workflow_dispatch`.
- Permissions: `contents: read`.
- Checkout pin SHA; setup JDK/Android từ version pin.
- Xác minh Gradle wrapper/checksum và dependency lock/verification metadata.
- Chạy lint, unit test và assemble debug cho module bị ảnh hưởng khi hợp lý.
- Upload test report và debug APK có retention ngắn; debug APK không phải release.
- Không có signing secret, không publish GitHub Release.

Triển khai hiện tại dùng JDK Temurin 17 đã pin, Gradle Wrapper 8.9 của repository và chạy `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`. Artifact lấy đúng từ `app/build/outputs/apk/debug/app-debug.apk`.

Acceptance: build sạch từ checkout mới, không network resolution ngoài artifact/dependency đã pin theo lock policy, failure rõ ràng khi lockfile lệch.

## `build-latest.yml`

**Mục đích:** phát hiện sớm incompatibility với toolchain/dependency mới mà không làm mất tính reproducible của build chính.

- Trigger: schedule và `workflow_dispatch`; không chạy trên mọi build.
- Dùng matrix thử các candidate version được khai báo rõ trong workflow/PR, không dùng chuỗi `latest` mơ hồ.
- `continue-on-error` có thể áp dụng cho lane thử nghiệm, nhưng kết quả phải tạo report dễ thấy.
- Không sửa file, không auto-merge, không release/sign.
- Dependabot/PR riêng mới là đường cập nhật version pin chính thức.

Tên workflow biểu thị “compatibility check gần mới nhất”, không cho phép runtime build chính tự resolve latest.

## `upstream-check.yml`

**Mục đích:** phát hiện commit mới của nguồn IR và chuẩn bị review.

- Trigger: schedule vừa phải và `workflow_dispatch`.
- Đọc source registry/manifest, so full pinned SHA với remote.
- Không clone toàn bộ lịch sử hoặc quét đệ quy database lớn; dùng API/shallow fetch/path scope khi đủ.
- Với thay đổi được chọn: tải giới hạn, normalize trong sandbox, validate schema/safety/license metadata và chạy fixtures.
- Xuất report/diff artifact. Có thể tạo issue/PR bằng token quyền hẹp, nhưng không merge tự động.
- Không publish database, APK hoặc truy cập signing key.

## `release.yml`

**Mục đích:** build, ký và phát hành APK chính thức lên GitHub Releases.

- Trigger ưu tiên: tag version bảo vệ hoặc `workflow_dispatch` với version đã tồn tại trong source; môi trường `release` cần approval.
- Xác minh tag/version/commit, working source và test gate.
- Build từ source một lần bằng toolchain pin; release APK không lấy upstream mới.
- Signing material lấy từ GitHub Actions encrypted secrets/environment, ghi vào thư mục tạm trong job và xóa sau khi ký.
- Không bao giờ upload keystore/password dưới dạng artifact hoặc log.
- Xác minh chữ ký APK, package/application ID, version code/name và cài đặt smoke test khi có emulator/device lane.
- Tạo SHA-256 checksum và provenance/build manifest.
- Tạo GitHub Release, đính kèm APK đã ký, checksum và release notes; không ghi đè asset của release đã công bố nếu hash khác.
- Release job có `contents: write`; các job trước giữ `contents: read`.

Secret dự kiến: keystore dạng base64/encrypted payload, alias và password tách riêng. Tên secret cụ thể được quyết định ở M2 và phải được mask.

## Versioning và kênh phát hành

- Tag/release dùng SemVer `vMAJOR.MINOR.PATCH`.
- Release workflow tính `versionCode = MAJOR * 1,000,000 + MINOR * 1,000 + PATCH + 1`; MAJOR phải ≤ 2000, MINOR/PATCH < 1000. Trước build, code phải lớn hơn mọi stable release đã công bố. Đây là mapping xác định, tăng đơn điệu theo SemVer và không phân tích version từ commit/message.
- Pre-release không được updater coi là stable trừ khi người dùng chọn kênh tương ứng.
- `versionName` khớp tag không có tiền tố `v`.
- GitHub Release bị draft/prerelease phải được xử lý rõ, không chọn chỉ vì timestamp mới hơn.

## Dependabot

`.github/dependabot.yml` kiểm tra Gradle và GitHub Actions định kỳ, giới hạn PR đang mở và không auto-merge. Mỗi PR vẫn phải qua build/test/review; update lớn của toolchain nên tách khỏi update dữ liệu IR.

## Reproducibility và audit

Mỗi release cần ghi commit ứng dụng, toolchain versions, dependency lock hash, normalized IR bundle hash, upstream manifest hash và APK SHA-256. Rebuild có thể khác ở metadata ký/timestamp; mục tiêu là giải thích được sai khác và đối chiếu unsigned build trước khi ký.
