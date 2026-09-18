# IR Protocol and Data Model

## Mục đích

Tài liệu này định nghĩa ranh giới chuẩn hóa giữa trạng thái máy lạnh, protocol/profile và API phát IR. Nó không định nghĩa timing, checksum hay model mapping cụ thể ở M0.

## Ba lớp biểu diễn

### 1. Trạng thái logic

`AcState` mô tả trạng thái mong muốn của máy lạnh. Capability của target quyết định trường nào hợp lệ. Giá trị không hỗ trợ phải tạo validation error hoặc cảnh báo giảm cấp có chủ đích.

### 2. Command đã encode

Protocol engine ánh xạ `AcState` sang frame/bit payload theo quy tắc đã xác minh. Profile engine có thể ánh xạ tập trạng thái hữu hạn sang command có sẵn. Cả hai phải tạo kết quả xác định từ cùng input, version và source data.

### 3. Transmission vật lý

`IrTransmission` chuẩn hóa:

```text
carrierFrequencyHz: integer
durationsMicros: [mark, space, mark, space, ...]
repeat: explicit policy/count
provenance: source + full commit SHA + normalized content hash
```

Quy ước bắt đầu bằng mark và xen kẽ mark/space phải được importer/encoder khai báo, không phỏng đoán. Khoảng gap và repeat frame phải được biểu diễn rõ ràng thay vì ghép tùy tiện.

## Contract của protocol engine

Một encoder dự kiến cung cấp:

- protocol/profile identity và version;
- capability được hỗ trợ;
- validation cho `AcState`;
- encode xác định sang `IrTransmission` hoặc lỗi có kiểu;
- fixture/golden vectors chứng minh frame, checksum và timing;
- provenance tới upstream commit/file hoặc fixture đã commit.

Không được suy ra checksum, bit order, toggle bit, timer encoding hoặc timing từ tên model. Nếu upstream mâu thuẫn, record bị quarantine cho tới khi có fixture hoặc kiểm tra phần cứng.

## Import raw IR

Pipeline import Flipper `.ir` dự kiến:

1. Parse cú pháp với giới hạn kích thước, số token và integer.
2. Nhận dạng raw hoặc protocol record; không âm thầm chuyển record không hiểu thành raw.
3. Chuẩn hóa đơn vị, carrier và mark/space theo contract nội bộ.
4. Validate cấu trúc và giới hạn an toàn.
5. Canonicalize để tạo content hash ổn định và phát hiện duplicate.
6. Gắn provenance/license; record thiếu thông tin đi vào quarantine.
7. Chạy fixture tests trước khi bundle được chấp nhận.

Importer không ghi đè dữ liệu đã duyệt chỉ vì upstream có record trùng tên.

## Protocol/profile/raw resolution

Thứ tự mặc định:

1. Native/Kotlin protocol encoder đã xác minh và hỗ trợ đủ capability cần thiết.
2. Profile có state mapping đầy đủ cho command đang yêu cầu.
3. Raw command đã validate cho thao tác cụ thể.
4. Báo không hỗ trợ; không phát dữ liệu được “ước lượng”.

Một profile có thể khai báo alias OEM/rebrand nhưng mapping phải có bằng chứng nguồn. Tên giống nhau không đủ để hợp nhất profile.

## Validation và giới hạn

Giá trị giới hạn cụ thể sẽ được xác định bằng test phần cứng và API ở M1+, không tự bịa ở M0. Validator phải hỗ trợ policy có version và kiểm tra tối thiểu:

- carrier dương và tương thích range thiết bị;
- durations dương, có số phần tử/ tổng thời gian trong giới hạn policy;
- không overflow khi chuyển sang Android `IntArray`;
- repeat và gap có giới hạn;
- schema/importer version tương thích;
- hash/provenance hiện diện với dữ liệu đóng gói hoặc tải về.

## Dò profile kiểu “1000 in 1”

Scanner không brute-force protocol parameter. Nó chỉ đi qua danh sách candidate đã validate và có provenance, ưu tiên theo hãng/khu vực/họ protocol để giảm số lần phát.

Yêu cầu an toàn:

- người dùng chọn thao tác thử ít rủi ro và xác nhận trước khi bắt đầu;
- countdown, nút dừng luôn hiển thị và dừng ngay hàng đợi;
- chỉ một transmission tại một thời điểm;
- rate limit, khoảng nghỉ và checkpoint xác nhận định kỳ;
- không chạy nền hoặc khi màn hình/flow không còn active;
- lưu cursor để tiếp tục, không tự động đánh dấu “đúng”;
- candidate thành công phải được người dùng xác nhận bằng quan sát máy lạnh;
- cảnh báo về thay đổi nguồn/mode/timer ngoài ý muốn.

Scanner không tuyên bố biết trạng thái thật của máy lạnh vì IR thường là giao tiếp một chiều.

## Deduplication

Deduplicate dựa trên canonical transmission hash cùng metadata cần thiết, không dựa duy nhất vào tên file/model. Các record byte-identical vẫn có thể giữ nhiều alias/provenance; dữ liệu phát chỉ lưu một payload chuẩn hóa.

