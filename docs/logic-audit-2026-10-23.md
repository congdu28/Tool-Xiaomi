# Rà soát logic ngày 23/10/2026

## Đã sửa

- DNS: chuyển thao tác đặc quyền sang nền, chờ kết quả, kiểm tra mode/hostname sau khi ghi; ghi nhà cung cấp trước mode; thử trả về cấu hình trước nếu áp dụng dở dang. Không tự đóng bảng DNS hoặc tự chuyển màn hình khi thiếu quyền. Có lựa chọn cấp quyền Shizuku và hướng dẫn cấu hình thủ công. Android dưới 9 báo không hỗ trợ. Kiểm tra hostname DoT và chặn địa chỉ IP/ký tự shell.
- Thông báo: chỉ tác động các gói được chọn và có cài đặt; xác minh miễn Doze và AppOps chạy nền. Bỏ các mã AppOps số, BOOT_COMPLETED không phải AppOp, gói com.google.android.gms.persistent (tên process), và danh sách MILLET không có xác minh. Tự khởi chạy/giới hạn pin MIUI cần kiểm tra trong giao diện ROM. Có lối tắt quyền thông báo. Nhãn miễn Doze không còn được trình bày như trạng thái tối ưu đầy đủ.
- Sheet thông báo cập nhật lại quyền Shizuku và badge sau khi quay về từ Cài đặt. Menu lấy quyền hiện tại. Bỏ cập nhật danh sách ứng dụng đã đóng/hủy Activity.
- Shell: đọc stdout/stderr đồng thời, giới hạn bộ đệm và chờ tối đa 15 giây cho Shizuku/Root. Khắc phục nguy cơ đầy pipe gây treo.
- Debloat: thực thi ở nền, callback về main thread, kiểm tra trạng thái gói sau mỗi thao tác và thống kê thành công từng ứng dụng.
- FPS: lệnh tắt Joyose và xóa PowerKeeper qua Shizuku dừng khi bước đầu thất bại. HUD bỏ phép đếm Choreographer của app làm FPS game; nguồn phần cứng gắn nhãn FPS (HW), còn nguồn dự phòng hiển thị Hz. Bỏ vòng callback chạy mỗi khung hình.
- Cấp quyền hệ thống chỉ báo thành công khi quyền đã được xác minh. Mở Cài đặt buộc dừng thủ công không còn được coi là đã dừng ứng dụng.
- RAM: chỉ báo đã dùng Shizuku khi lệnh thành công. Updater kiểm tra trạng thái DownloadManager trước khi mở APK.
- Cập nhật mô tả và README, sửa cảnh báo định dạng chuỗi HUD, bỏ build output khỏi Git.

## Giới hạn và các mục cần làm tiếp

Rà soát này gồm DNS, thông báo, các helper đặc quyền, HUD, debloat, tải cập nhật và kiểm tra tĩnh luồng service/receiver. Build/test không xác nhận mọi tính năng trên mọi ROM.

- Game Mode vẫn thay đổi nhiều khóa hệ thống, còn các đường Root đồng bộ; cần gom thành tác vụ nền và lưu snapshot cấu hình để khôi phục chính xác thay vì dùng giá trị 60/120 Hz cố định. Xóa dữ liệu PowerKeeper không thể hoàn tác bằng cách bật lại Joyose. Cần thử trên thiết bị trước khi thay đổi hành vi này.
- FPS (HW) là nguồn sysfs của thiết bị, có thể phản ánh tần số hiển thị; không mặc định coi là FPS riêng của game.
- Keystore phát hành và mật khẩu hiện có trong repo. Cần xử lý khóa ký cùng kế hoạch duy trì đường nâng cấp APK cho người dùng hiện tại.
- Một số service/receiver bắt và bỏ qua ngoại lệ; cần thử boot, hạn chế chạy nền và overlay trên MIUI/HyperOS thực tế.
- Shizuku vẫn gọi newProcess qua reflection; cần thử bản Release trên phiên bản Shizuku đang dùng.

## Kiểm tra

- Unit tests mới: chuẩn hóa hostname DoT, hostname sai/IP/ký tự shell; tiến trình xuất nhiều dữ liệu cả stdout/stderr, mã thoát lỗi, tiến trình bị treo.
- Debug và Release (R8) build thành công; 6/6 unit tests đã chạy thành công. Lint cuối: 0 lỗi, 1.038 cảnh báo (chủ yếu hardcoded text và tài nguyên/giao diện). Đã sửa 11 lỗi Lint, gồm XML thừa, định dạng chuỗi %, service lookup, tint và receiver; giữ nhánh Intent của TileService cho Android dưới 14 với chú thích tương thích. Không có thiết bị ADB kết nối lúc kiểm tra.

## Kịch bản kiểm tra trên máy Xiaomi

1. Chưa cấp Shizuku/ADB/Root: chọn Cloudflare. Bảng DNS giữ nguyên, hiện hướng dẫn cấp quyền hoặc cấu hình thủ công, không báo đã áp dụng.
2. Cấp Shizuku, chọn lại Cloudflare: đọc lại private_dns_mode=hostname và private_dns_specifier=1dot1dot1dot1.cloudflare-dns.com trong hệ thống. Thử Auto và Off, đóng/mở lại app để kiểm tra.
3. Tắt Shizuku hoặc từ chối Root giữa thao tác: không báo thành công; không đứng giao diện. Thử hostname sai, IP, tên dài và thao tác bấm liên tiếp.
4. Mạng chặn cổng DoT 853: cấu hình có thể lưu được nhưng mạng không kết nối; chọn Auto/Off để phục hồi. Trạng thái cấu hình không xác nhận máy chủ đã truy cập được.
5. Thông báo: chọn một ứng dụng, để một ứng dụng khác không chọn; kiểm tra chỉ ứng dụng đã chọn được thay đổi. Thu hồi Shizuku giữa batch phải báo các mục chưa hoàn tất.
6. Kiểm tra quyền thông báo, Tự khởi chạy và pin MIUI; quay lại sheet và kiểm tra badge Doze. Gửi tin nhắn thử sau khi tắt màn hình 10–15 phút. Badge Doze không chứng minh thông báo sẽ đến đúng hạn.
7. Thử đóng băng/khôi phục một gói an toàn; thử gói không có quyền thay đổi để kiểm tra số thành công. Hủy tải APK và xác nhận app không mở trình cài đặt với file lỗi.

Tham khảo nền tảng: https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/master/src/com/android/settings/network/PrivateDnsPreferenceController.java và https://developer.android.com/training/monitoring-device-state/doze-standby.
