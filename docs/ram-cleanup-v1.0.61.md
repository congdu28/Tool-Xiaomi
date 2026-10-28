# Dọn RAM và ứng dụng đang chạy — v1.0.61

## Hành vi

Vào Tiện ích → Bộ nhớ và ứng dụng → Chọn ứng dụng để dọn RAM. Danh sách đọc `ps -A -n -o UID,PID,NAME` qua Shizuku, ghép UID với PackageManager của hồ sơ hiện tại. Chỉ hiển thị ứng dụng người dùng có tiến trình tại lần kiểm tra; không dùng lịch sử UsageStats để suy ra đang chạy. Các tiến trình biệt lập không ánh xạ được sang gói không được suy đoán.

Không tự chọn app. Có chọn tất cả app có thể dừng và tùy chọn giữ lại lưu riêng theo gói. Các mẫu app thông báo khuyến nghị được giữ mặc định, có thể thay đổi lựa chọn giữ; các app đã được miễn Doze được bảo vệ cứng. Cũng bảo vệ Mi Tool, Shizuku, bàn phím, launcher, gói hệ thống, UID dùng chung và hồ sơ người dùng khác. Trước mỗi thao tác kiểm tra lại thông tin gói, UID, trạng thái bảo vệ và tiến trình. Không tắt bảo vệ nhiệt hay xóa dữ liệu app.

`am force-stop --user USER PACKAGE` chỉ chạy sau xác nhận. Không còn tiến trình UID và mã thoát 0 mới được tính là đã dừng; báo riêng chưa xác nhận và bỏ qua. Đo RAM trống trước/sau, hiển thị biến động có dấu và giải thích đây là số đo toàn hệ thống. Đóng bảng hủy phần chưa thực hiện sau lệnh đang xử lý. Không có alarm/dịch vụ dọn định kỳ. Widget tiếp tục mở trang RAM để người dùng chọn thao tác.

## Lỗi dùng chung phát hiện khi kiểm tra

Shizuku có thể chuyển trạng thái exitValue khi tiến trình chưa kết thúc thành IllegalArgumentException. Bộ ProcessRunner trước đây chỉ bắt IllegalThreadStateException nên có thể hủy lệnh ngay khi nó còn chạy. Chuyển sang waitFor trên luồng chờ riêng có giới hạn thời gian, đồng thời đọc hai pipe. Giữ timeout và giới hạn bộ đệm; kiểm thử cả lỗi chờ từ xa, mã thoát thất bại, pipe lớn và lệnh treo. Các tính năng DNS/thông báo cũng dùng bộ chờ này.

## Kiểm tra

Chạy unit test, lint và assembleRelease. Các bài kiểm tra mới bao gồm phân tích UID/PID, từ chối đầu vào shell, bảo vệ UID hệ thống/chung/khác hồ sơ và hồi quy ProcessRunner.

Kiểm thử tích hợp chỉ trên emulator-5554, Android 15. Cài Shizuku chính thức, khởi chạy và cấp quyền cho Mi Tool. Hai ứng dụng thử không có mã nguồn thực thi riêng, chỉ dùng android.app.Activity, được khai báo trong scripts/fixtures/ram-cleanup/target.xml và protected.xml. Dùng aapt package với android.jar để đóng gói từng manifest rồi ký bằng khóa thử bất kỳ và cài vào giả lập. Không dùng các gói thử này trên điện thoại cá nhân.

Sau khi cài hai fixture, chạy:

```sh
python scripts/ram_cleanup_smoke.py --adb PATH_TO_ADB --serial emulator-5554
```

Script kiểm tra không chọn sẵn, bảo vệ app miễn Doze, lưu tùy chọn giữ qua tải lại, bỏ giữ/chọn, hủy xác nhận, dừng tiến trình app thử và giữ nguyên tiến trình app thử được bảo vệ. Lưu ảnh trong app/build/ui-qa. Script không chạy trên serial điện thoại và chỉ chọn gói thử app.cd.mitool.ramprobe.target để dừng.

Chưa kiểm tra trực tiếp trên Xiaomi 17 Ultra/HyperOS 4. Giới hạn của ROM/Shizuku có thể làm thao tác không thành công; app phải báo chưa xác nhận thay vì thành công giả.

## Kết quả xác minh

28/28 unit test đạt, không có lỗi Android Lint chặn build, assembleRelease thành công. Luồng tích hợp qua Shizuku 13.6.0 đã đạt trên cả APK Debug và Release: lưu giữ ứng dụng, tải lại, hủy xác nhận, dừng app thử thực sự và giữ nguyên app thử được miễn Doze. Đã xác minh versionCode 61/versionName 1.0.61 và giữ nguyên chứng thư ký cập nhật.
