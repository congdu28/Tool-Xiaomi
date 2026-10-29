# Mi Tool v1.0.62 — Sửa thông báo khi cắm sạc

- Thêm công tắc **Thông báo pin và sạc** riêng trong Tiện ích → Cảnh báo pin và sạc. Không cần bật đồng hồ để xem trạng thái sạc.
- Hiển thị quyền thông báo đang được phép, bị Android chặn hoặc kênh pin/sạc đang tắt; thêm nút mở cài đặt hệ thống. Không tự tạo kênh mới để vượt lựa chọn chặn của người dùng.
- Sửa nút tắt đồng hồ từ ô cài đặt nhanh, lối tắt và thông báo: không dừng các cảnh báo pin hay chế độ thông báo sạc còn được bật.
- Khôi phục dịch vụ đúng các chế độ đã chọn khi cập nhật/khởi động máy. Đồng bộ khi cắm/rút sạc, không tự bật đồng hồ.
- Phân biệt còn kết nối nguồn sạc với đang nạp điện: pin đầy hoặc tạm dừng sạc không bị mô tả như đã rút cáp. Không gọi sạc nhanh khi thiếu số đo công suất.
- Sửa ngưỡng báo chuông khi mức pin nhảy qua nhiều phần trăm, tránh bỏ lỡ ngưỡng. Không báo đầy pin do rút sạc ở mức 100%, không phát nhiều chuông cùng một lần cập nhật. Dừng âm báo khi rút sạc/dừng theo dõi.
- Thông báo có nút mở đúng phần cài đặt pin/sạc. Khi tắt toàn bộ chức năng theo dõi, dịch vụ và thông báo được gỡ.

Sau khi rút sạc, chế độ này chuyển sang hiển thị trạng thái dùng pin để tiếp tục theo dõi. Có thể tắt công tắc nếu không cần. Tính năng dọn RAM và ứng dụng đang chạy của v1.0.61 vẫn được giữ.

35/35 unit test đạt, lint không có lỗi chặn build. Đã kiểm tra các luồng cắm/rút sạc, pin đầy, tắt đồng hồ, chặn/cấp lại quyền trên APK Release, Android 15 giả lập. Chưa kiểm tra trực tiếp trên Xiaomi 17 Ultra/HyperOS 4.

Tải **Mi-Tool-v1.0.62-Release.apk** để cập nhật.
