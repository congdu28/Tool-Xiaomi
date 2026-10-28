# Mi Tool v1.0.60 — Giao diện mới và sửa logic tiện ích

- Sắp xếp lại bốn trang Tổng quan, Tiện ích, Pin, Cài đặt. Nhóm tiện ích theo mục đích; thêm tìm kiếm tiếng Việt có hoặc không dấu.
- Thống nhất thẻ tính năng, biểu tượng, màu sáng/tối, nút bấm và bảng điều khiển. Nút đóng luôn ở đầu bảng; thông báo và hướng dẫn mô tả đúng thao tác thực hiện.
- DNS riêng tư: trạng thái chọn theo cấu hình hệ thống, kiểm tra tên miền DoT, giữ nội dung khi nhập sai. Không báo sao chép thành áp dụng thành công.
- Thông báo: người dùng chủ động chọn ứng dụng. Không tự chọn hàng loạt ứng dụng để miễn giới hạn chạy nền.
- RAM: thay thao tác dọn/tăng tốc bằng xem số đo và mở quản lý ứng dụng. Bỏ dọn nền hàng loạt gây xung đột với nhu cầu nhận thông báo.
- Joyose/PowerKeeper: xác nhận trước thay đổi có tác động; chạy lệnh ngoài luồng giao diện, kiểm tra kết quả. Không tự ép chế độ hiệu năng/nhiệt và không hứa mở khóa FPS. Bật lại Joyose không phục hồi dữ liệu PowerKeeper đã xóa.
- Pin: chỉ hiển thị dung lượng/SOH khi đọc được; ước lượng thời gian dùng cần đủ dữ liệu phiên thực. Phép đo sạc không suy ra chuẩn sạc hoặc chất lượng cáp từ công suất pin.
- Dịch vụ cảnh báo độc lập với đồng hồ; mặc định mới không tự bật các chức năng nền. Cải thiện vòng đời tải cập nhật và xử lý liên kết trang phát hành.

## Kiểm tra

Unit test, Android Lint và build Release; kiểm tra giao diện trên Android 15 giả lập, chế độ sáng/tối và cỡ chữ lớn. Chức năng đặc thù HyperOS/Shizuku vẫn cần kiểm tra trên điện thoại thật; chưa xác nhận nguyên nhân nóng máy của Xiaomi 17 Ultra.

Cài **Mi-Tool-v1.0.60-Release.apk** để cập nhật.
