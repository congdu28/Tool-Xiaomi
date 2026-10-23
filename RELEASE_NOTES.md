## Mi Tool v1.0.58 — Sửa DNS riêng tư và tối ưu thông báo

- **DNS riêng tư:** giữ bảng lựa chọn khi áp dụng, chạy lệnh ở nền và xác minh cấu hình trước khi báo thành công. Ghi tên máy chủ trước khi bật chế độ DoT. Có hướng dẫn cấp quyền Shizuku hoặc cấu hình thủ công khi thiếu quyền. Kiểm tra hostname, từ chối địa chỉ IP và dữ liệu không hợp lệ; thông báo rõ trên Android dưới 9.
- **Khắc phục trễ thông báo:** chỉ xử lý ứng dụng được chọn; xác minh miễn Doze và quyền chạy nền cho từng ứng dụng. Bỏ các lệnh AppOps theo số và thay đổi hệ thống chưa được xác minh. Thêm lối tắt quyền thông báo; cập nhật trạng thái sau khi cấp quyền hoặc trở về từ Cài đặt. Tự khởi chạy và giới hạn pin MIUI được kiểm tra riêng trong giao diện hệ thống.
- **Shizuku/Root:** giới hạn thời gian chờ lệnh, đọc stdout/stderr đồng thời để hạn chế treo do đầy bộ đệm.
- **Debloat:** chạy ở nền, kiểm tra trạng thái đóng băng/khôi phục và báo số ứng dụng thực sự thành công.
- **HUD:** hiển thị Hz khi không có nguồn FPS phần cứng; nguồn phần cứng được ghi rõ FPS (HW). Bỏ phép đếm khung hình của HUD làm FPS game.
- **Cập nhật và tiện ích:** kiểm tra tải APK thành công trước khi mở trình cài đặt; sửa xác nhận quyền, trạng thái RAM và thao tác buộc dừng thủ công.
- Sửa XML, định dạng chuỗi và các lỗi Lint; cập nhật tài liệu kiểm tra.

### Kiểm tra và lưu ý

- Đã build Debug/Release, kiểm tra chữ ký APK, chạy 6 unit test và Lint (0 lỗi). Workflow phát hành chạy lại test, Lint và build trước khi đăng APK.
- Chưa kiểm tra trên điện thoại Xiaomi thật. Khả năng nhận thông báo còn phụ thuộc quyền thông báo, cấu hình pin/tự khởi chạy của ROM và kết nối mạng.
- DNS tự động cần WRITE_SECURE_SETTINGS, Shizuku hoặc Root. Khi dùng cách thủ công, cần dán tên máy chủ trong Cài đặt và bấm Lưu. Lưu cấu hình thành công chưa xác nhận kết nối DoT thành công.
- FPS (HW) phụ thuộc nguồn sysfs trên thiết bị và có thể phản ánh tần số hiển thị.

Cài file **Mi-Tool-v1.0.58-Release.apk** bên dưới để cập nhật.
