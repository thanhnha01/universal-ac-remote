# Upstream Sources

## Chính sách chung

Nguồn upstream chỉ được nhập qua pull request có phạm vi rõ ràng. Mỗi snapshot/bundle phải pin **full commit SHA**, ghi URL repository, đường dẫn đã lấy, thời điểm kiểm tra, license/attribution, importer version và hash của output normalized.

Build ứng dụng không được clone nhánh upstream, resolve `latest`, hoặc phụ thuộc tính sẵn sàng của upstream. Upstream check chỉ tạo báo cáo/PR; không tự động đưa dữ liệu mới vào release.

## Registry nguồn dự kiến

| Source ID | Vai trò dự kiến | Cách dùng ban đầu | Trạng thái M0 |
|---|---|---|---|
| `irremoteesp8266` | Protocol AC và implementation tham chiếu/JNI | Chọn một protocol và tập file tối thiểu | Chưa nhập; chưa có SHA pin |
| `smartir` | Mapping thiết bị/profile và code climate | Import tập con sau review schema/license | Chưa nhập; chưa có SHA pin |
| `flipper-irdb` | Raw IR và metadata thiết bị | Import record `.ir` chọn lọc | Chưa nhập; chưa có SHA pin |
| `irplus` | Profile/model và raw codes | Adapter riêng, import chọn lọc | Chưa nhập; chưa có SHA pin |
| `lirc` | Định dạng/config IR bổ sung | Chỉ đánh giá khi tới M9 | Chưa nhập |
| `broadlink` | Định dạng packet bổ sung | Chỉ parser/converter khi cần; không dùng hub | Chưa nhập |
| `pronto` | Chuỗi Pronto Hex | Chỉ đánh giá/import ở M9 | Chưa nhập |

“Chưa có SHA pin” là đúng vì M0 không tải hay vendor dữ liệu. SHA trở thành bắt buộc ngay trong PR đầu tiên sử dụng source đó.

## Manifest snapshot dự kiến

Khi implementation bắt đầu, repository sẽ có manifest máy đọc được với các trường tối thiểu sau (đường dẫn/tên file sẽ được quyết định ở milestone tương ứng):

```yaml
sourceId: flipper-irdb
repository: <canonical HTTPS URL>
commitSha: <40-character full SHA>
license: <reviewed SPDX expression or documented status>
selectedPaths: []
importerVersion: <pinned version>
schemaVersion: <normalized schema version>
generatedContentSha256: <sha256>
```

Không dùng placeholder này làm manifest production.

## Quy trình cập nhật

1. `upstream-check.yml` đọc SHA đang pin và query remote ở job chỉ đọc.
2. Nếu có commit mới, workflow tạo artifact/report hoặc issue/PR đề xuất; không merge tự động.
3. Người duy trì chọn phạm vi file cần thiết, không quét đệ quy toàn repository lớn.
4. Kiểm tra license và thay đổi schema/protocol.
5. Import trong môi trường pin toolchain, tạo normalized diff có kích thước kiểm soát.
6. Chạy parser/schema/semantic validation, duplicate checks và fixture/golden tests.
7. Review thủ công provenance và các thay đổi bất thường.
8. Merge PR để cập nhật đồng thời SHA, output và fixtures.

## Trạng thái record

- `candidate`: parse được nhưng chưa đủ review/test.
- `validated`: qua schema, safety và fixture cần thiết.
- `quarantined`: mâu thuẫn, thiếu license/provenance hoặc có dữ liệu bất thường.
- `deprecated`: giữ để truy vết nhưng không được resolver chọn mặc định.

Chỉ `validated` được đóng gói trong APK/database release.

## Quy tắc license và attribution

License được đánh giá theo từng source và đôi khi từng file. Không giả định license của code áp dụng giống hệt database hoặc ngược lại. Source chưa rõ quyền phân phối có thể dùng làm tài liệu nghiên cứu cục bộ nhưng không được commit/đóng gói/phát hành cho tới khi giải quyết.

## Bảo vệ supply chain

- Clone/fetch ở workflow riêng với quyền tối thiểu; không thực thi script upstream mặc định.
- Pin mọi action theo full SHA và dependency/tool version.
- Hạn chế kích thước download, timeout và file types.
- Treat tên file, metadata và nội dung upstream là input không tin cậy.
- Không để pull request không tin cậy truy cập signing secrets.
- Lưu report validation làm CI artifact để audit.

