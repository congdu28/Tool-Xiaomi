# Mi Tool (Tool Xiaomi)

Tiện ích Android dành cho Xiaomi, Redmi và POCO chạy MIUI/HyperOS. Một số thao tác cần Shizuku, quyền hệ thống hoặc hỗ trợ của ROM.

[Phát hành và tải APK](https://github.com/congdu28/Tool-Xiaomi/releases/latest)

## Giao diện

- **Tổng quan:** trạng thái thiết bị, pin và các lối tắt thường dùng; thông tin phần cứng có thể thu gọn.
- **Tiện ích:** tìm kiếm có/không dấu; các nhóm Kết nối và thông báo, Màn hình và đồng hồ, Pin và bộ nhớ, Công cụ nâng cao.
- **Pin:** số đo cảm biến, lịch sử thu thập và ước lượng khi đủ dữ liệu. Thời gian sử dụng ứng dụng không phải số đo điện năng tiêu thụ theo ứng dụng.
- **Cài đặt:** Shizuku, quyền chạy nền, giao diện, widget và cập nhật.

## Chức năng

DNS riêng tư dùng tên miền DoT; thay đổi tự động cần quyền phù hợp, nếu thiếu quyền sẽ hướng dẫn thao tác hệ thống. Khắc phục trễ thông báo áp dụng cho các ứng dụng được người dùng chọn, kiểm tra từng kết quả; không đảm bảo thay thế mọi giới hạn của HyperOS.

Tần số quét kiểm tra mức màn hình hỗ trợ và lưu cấu hình để khôi phục. Đồng hồ, lớp phủ, thông báo pin và sạc, cảnh báo pin là các lựa chọn riêng. Mỗi sự kiện cắm/rút sạc, đạt ngưỡng, đầy pin hoặc quá nhiệt chỉ có một lựa chọn âm báo: Tắt, Chuông, Giọng nói, hoặc cả hai. Có thử chuông/giọng nói, báo âm lượng báo thức bằng 0 và chuông dự phòng khi giọng Việt không hoạt động. Bật **Thông báo pin và sạc** trong Tiện ích → Cảnh báo pin và sạc; mục này kiểm tra quyền Android/kênh thông báo và có nút mở cài đặt. Thông báo tiếp tục hiển thị trạng thái pin sau khi rút sạc. Màn hình RAM cho xem số đo và chọn ứng dụng đang chạy để buộc dừng qua Shizuku, có xác nhận trước khi thực hiện. Người dùng có thể giữ lại app; app được miễn Doze, bàn phím, launcher, ứng dụng hệ thống và UID dùng chung được bảo vệ. Không chạy dọn tự động.

Các thao tác Joyose, xóa dữ liệu PowerKeeper và đóng băng ứng dụng cần đọc kỹ xác nhận. FPS thực tế phụ thuộc trò chơi, ROM và phần cứng. Bật lại Joyose không hoàn tác dữ liệu đã xóa. Ứng dụng không tự tắt bảo vệ nhiệt để xử lý nóng máy.

Số đo sạc phản ánh phía pin khi cảm biến hỗ trợ; không xác nhận chuẩn sạc, công suất củ sạc hoặc chất lượng cáp. Dung lượng thiết kế/SOH chưa đọc được sẽ hiển thị chưa có dữ liệu.

## Cài đặt và quyền

Tải APK từ Releases, mở bằng trình cài đặt Android. Chỉ bật quyền thông báo, lớp phủ hoặc truy cập sử dụng khi cần tính năng tương ứng. Có thể cài [Shizuku](https://shizuku.rikka.app) và cấp quyền từ phần Cài đặt của Mi Tool.

Cấp quyền ghi cài đặt bảo mật bằng ADB nếu cần:

```sh
adb shell pm grant app.cd.mitool android.permission.WRITE_SECURE_SETTINGS
```

Quyền này không mở mọi hoạt động đặc thù ROM. App đọc lại trạng thái và báo kết quả khi có thể.

## Phát triển

Java 17, Android SDK 35, minSdk 24. Khai báo SDK trong `local.properties` hoặc `ANDROID_HOME`.

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

Kiểm tra giao diện trên máy giả lập riêng sau khi cài APK:

```sh
python scripts/ui_smoke.py --adb PATH_TO_ADB --serial emulator-5554
```

Script chỉ chấp nhận serial giả lập và đặt lại dữ liệu Mi Tool trên giả lập; lưu ảnh và cây giao diện vào `app/build/ui-qa`. Không xác nhận các thao tác hệ thống có tác động dữ liệu.

Quy trình GitHub Actions chạy unit test, lint và build APK khi đẩy tag `v*`; ghi chú phát hành lấy từ `RELEASE_NOTES.md`. Xem các báo cáo rà soát trong `docs/`.
