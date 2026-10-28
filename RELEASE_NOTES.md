# Mi Tool v1.0.61 — Dọn RAM và quản lý ứng dụng đang chạy

- Thêm màn hình **Chọn ứng dụng để dọn RAM** trong tiện ích Bộ nhớ và ứng dụng.
- Đọc danh sách tiến trình thực qua Shizuku, gom theo ứng dụng/UID; không dùng danh sách app đã cài hoặc app gần đây để giả lập trạng thái đang chạy.
- Chọn từng app hoặc chọn tất cả app có thể dừng; không tự chọn sẵn. Tùy chọn giữ lại ứng dụng được lưu cho các lần sau, mặc định giữ các app thông báo được khuyến nghị.
- Bảo vệ Mi Tool, Shizuku, bàn phím, launcher, app miễn giới hạn pin/Doze, ứng dụng hệ thống và UID dùng chung. Kiểm tra lại quyền và bảo vệ trước mỗi lần dừng.
- Xác nhận trước khi buộc dừng: có thể gián đoạn tác vụ chưa lưu và ngừng nhận thông báo đến khi mở lại app; không xóa dữ liệu ứng dụng.
- Chạy thao tác ngoài luồng giao diện. Chỉ báo đã dừng sau khi lệnh thành công và không còn tiến trình UID đó tại lần kiểm tra lại; báo riêng bỏ qua/chưa xác nhận.
- Sửa bộ chờ lệnh Shizuku dùng chung: không hủy lệnh sớm khi tiến trình chưa kết thúc. Có kiểm thử hồi quy cho lỗi này.
- Đóng bảng sẽ hủy các ứng dụng chưa xử lý sau lệnh hiện tại.
- Đo RAM trống trước/sau và hiển thị chênh lệch có dấu. Biến động RAM toàn hệ thống không được trình bày như lượng RAM giải phóng chắc chắn từ các app được dọn.

Cần Shizuku đang chạy và đã cấp quyền để liệt kê/dừng app khác. Không có dọn tự động định kỳ. Android có thể sử dụng lại RAM trống; tính năng không bảo đảm tăng tốc hoặc giảm nhiệt.

28/28 unit test đạt, lint không có lỗi chặn build, build Release thành công. Đã kiểm tra dừng ứng dụng thực sự và bảo vệ app miễn Doze qua Shizuku 13.6.0 trên APK Release, Android 15 giả lập. Khả năng hoạt động qua Shizuku trên HyperOS 4 vẫn cần xác minh trên Xiaomi thật.

Tải **Mi-Tool-v1.0.61-Release.apk** để cập nhật.
