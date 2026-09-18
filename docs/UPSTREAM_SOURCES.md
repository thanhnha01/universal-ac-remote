# Upstream Sources

## Chính sách chung

Nguồn upstream chỉ được nhập qua pull request có phạm vi rõ ràng. Mỗi snapshot/bundle phải pin **full commit SHA**, ghi URL repository, đường dẫn đã lấy, thời điểm kiểm tra, license/attribution, importer version và hash của output normalized.

Build ứng dụng không được clone nhánh upstream, resolve `latest`, hoặc phụ thuộc tính sẵn sàng của upstream. Upstream check chỉ tạo báo cáo/PR; không tự động đưa dữ liệu mới vào release.

## Registry nguồn dự kiến

| Source ID | Vai trò dự kiến | Cách dùng ban đầu | Trạng thái M0 |
|---|---|---|---|
| `irremoteesp8266` | Protocol AC và implementation tham chiếu/JNI | Pinned native source subset và generated support inventory | M5 snapshot tại commit `1e2f0f3ef0a93cbf2a8ddb2e95130f8f4c584b3f` |
| `smartir` | Mapping thiết bị/profile và code climate | Import tập con sau review schema/license | Chưa nhập; chưa có SHA pin |
| `flipper-irdb` | Raw IR và metadata thiết bị | Import record `.ir` chọn lọc | Chưa nhập; chưa có SHA pin |
| `irplus` | Profile/model và raw codes | Adapter riêng, import chọn lọc | Chưa nhập; chưa có SHA pin |
| `lirc` | Định dạng/config IR bổ sung | Chỉ đánh giá khi tới M9 | Chưa nhập |
| `broadlink` | Định dạng packet bổ sung | Chỉ parser/converter khi cần; không dùng hub | Chưa nhập |
| `pronto` | Chuỗi Pronto Hex | Chỉ đánh giá/import ở M9 | Chưa nhập |

“Chưa có SHA pin” là đúng vì M0 không tải hay vendor dữ liệu. SHA trở thành bắt buộc ngay trong PR đầu tiên sử dụng source đó.

## B10.5 pinned catalog snapshots

The v1.0 catalog inputs are locked in [upstream-lock.json](../upstream-lock.json); license metadata is in [licenses.json](../data/upstreams/licenses.json). Catalog builds read only these exact local snapshots and never resolve a branch tip. `tools/sync_upstreams.py --update-candidates` records branch tips separately; `--promote-candidates` requires a PASS catalog report that names the exact candidate SHA.

- SmartIR is MIT licensed at `e4df2957ad915536f41ffb39daa96886d7cfe040`; only `codes/climate/*.json` is selected. Its product name is not used for app branding.
- Flipper-IRDB is CC0-1.0 at `d126fb1b6f1e114c52b4a8c19839ea65e3a9c24d`; only `ACs/**/*.ir` is considered. Files are excluded if their first-add commit is before cutoff `2319685` or cannot be proven. The resolved full cutoff SHA is recorded in `data/upstreams/flipper-exclusions.json`.
- irplus is pinned for audit but marked `EXCLUDED_LICENSE_UNCLEAR`; no profiles are ingested.

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

## M5 IRremoteESP8266 generic AC engine

- Repository: `https://github.com/crankyoldgit/IRremoteESP8266`
- Pinned commit: `1e2f0f3ef0a93cbf2a8ddb2e95130f8f4c584b3f`
- License: LGPL-2.1-only. The upstream `LICENSE.txt` is shipped unchanged at `app/src/main/cpp/IRremoteESP8266-LICENSE.txt`; source copyright headers are retained.
- Selected implementation: `IRac.cpp`/`.h`, `IRsend.cpp`/`.h`, and the AC family senders for LG, Gree, Panasonic A/C, Daikin, Midea, Fujitsu, Mitsubishi A/C, and Samsung A/C. A single JNI function validates protocol IDs through upstream `strToDecodeType()` and `IRac::isProtocolSupported()`, resolves named models through `IRac::strToModel()`, and calls `IRac::sendAc()`. The exact vendored file list and enabled `SEND_*` flags are in [UPSTREAM_MANIFEST.txt](../app/src/main/cpp/UPSTREAM_MANIFEST.txt).
- `SupportedProtocols.md` is stored as a snapshot from the same full commit. [generate_protocol_catalog.py](../tools/generate_protocol_catalog.py) emits [PROTOCOL_CATALOG.md](PROTOCOL_CATALOG.md): generic-enabled families, detailed families needing sender/capability validation, and detailed variants outside `IRac::isProtocolSupported()`. Catalog discovery never automatically admits a new protocol to the APK.
- Carrier frequency is captured from upstream `IRsend::enableIROut()` after upstream normalization to Hz. Mixed-frequency transmissions are rejected as requiring special handling. Temperature and capability metadata are currently explicitly reviewed for the eight enabled family IDs.
- Test vector: the deterministic waveform in `app/src/androidTest/assets/lg_8808721.output.txt` is copied from `test/ir_LG_test.cpp`'s LG A/C `0x8808721` `outputStr()` assertion. The upstream test associates it with power on, Cool, 22°C, Medium fan and a 38 kHz carrier.
- Timing generation: `SWIGLIB` routes upstream `IRsend::mark/space` to its process-global `timingList`; JNI serializes access and obtains the captured sequence through `IRac.getTiming()`. Small local SWIGLIB-only changes also clear/capture `carrierFrequencyList`; protocol framing, checksums and mark/space timing remain upstream implementation.
- Source files: [IRac.cpp at pinned commit](https://github.com/crankyoldgit/IRremoteESP8266/blob/1e2f0f3ef0a93cbf2a8ddb2e95130f8f4c584b3f/src/IRac.cpp), [IRsend.cpp at pinned commit](https://github.com/crankyoldgit/IRremoteESP8266/blob/1e2f0f3ef0a93cbf2a8ddb2e95130f8f4c584b3f/src/IRsend.cpp), [LG implementation](https://github.com/crankyoldgit/IRremoteESP8266/blob/1e2f0f3ef0a93cbf2a8ddb2e95130f8f4c584b3f/src/ir_LG.cpp), and [upstream LG tests](https://github.com/crankyoldgit/IRremoteESP8266/blob/1e2f0f3ef0a93cbf2a8ddb2e95130f8f4c584b3f/test/ir_LG_test.cpp).
