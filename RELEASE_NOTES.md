# Mi Tool v1.0.67 — Tinh chỉnh toàn diện giao diện UI/UX & Sửa lỗi vận hành

- **Chuẩn hóa giao diện & bố cục 4 tab chính:**
  - Tinh chỉnh bảng màu Sáng/Tối sắc nét, đồng bộ tiêu đề lớn (`26sp`) kèm mô tả ngắn trên cả 4 tab (**Tổng quan**, **Tiện ích**, **Pin**, **Cài đặt**) và thêm đường phân cách tinh tế phía trên thanh điều hướng dưới.
  - Làm mới tab **Tổng quan**: thẻ Pin & Nhiệt độ bấm được để mở nhanh tab Pin, nhãn Shizuku bấm được để mở nhanh cài đặt Shizuku, và 4 nút **Thao tác nhanh** dẫn thẳng tới các công cụ dùng nhiều nhất (*Chống nóng Game*, *Tần số quét Hz*, *DNS riêng tư*, *Thông báo*) thay vì lặp lại các tab ở thanh điều hướng.
  - Sắp xếp lại danh mục **Tiện ích** thành 4 nhóm hợp lý (*Hiệu năng & Trò chơi*, *Pin & Thông báo*, *Mạng & Kết nối*, *Hệ thống & Tiện ích*) kèm khung icon bo góc đồng bộ.
  - Gom bố cục bottom sheet **Cảnh báo pin & sạc** thành 3 thẻ bo góc rõ ràng với lưới nút 2x2 gọn gàng; đồng bộ bo góc cho các bottom sheet Tần số quét, Đồng hồ giây và Chống nóng Game.
- **Sửa lỗi chức năng & tối ưu ổn định:**
  - Sửa lỗi nút cấp quyền `WRITE_SECURE_SETTINGS` qua Shizuku bị vô hiệu hóa sớm ngay khi mới cấp quyền Shizuku.
  - Chỉ tải danh sách ứng dụng hao pin khi người dùng đang mở tab Pin, tránh chạy truy vấn nặng không cần thiết mỗi lần quay lại app.
  - Đồng bộ làm mới và giải phóng tham chiếu `refreshHzSheetState` trong vòng đời Activity, cập nhật ngay tóm tắt khi bật/tắt màn hình sạc Ambient và hiển thị đúng trạng thái tối ưu chống nóng trên thẻ Trò chơi.
- Kiểm tra tự động bằng unit test, lint và bản dựng Release APK qua GitHub Actions.

Tải **Mi-Tool-v1.0.67-Release.apk** sau khi quy trình Build and Release APK trên GitHub Actions hoàn tất.
