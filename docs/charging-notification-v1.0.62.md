# Khắc phục cắm sạc không hiện thông báo — v1.0.62

## Các đường lỗi trong mã

Cấu hình lockscreen_notif_enabled đã tồn tại nhưng không còn công tắc trong giao diện. Khi đồng hồ tắt và cờ thông báo sạc chưa được bật, BatteryReceiver không mở dịch vụ. Công tắc Thông báo pin và sạc mới cho người dùng bật riêng chức năng này và kiểm tra quyền Android/kênh thông báo.

ACTION_STOP từ ô cài đặt nhanh/lối tắt/nút trên thông báo từng dừng toàn bộ NotificationClockService, dù các cảnh báo pin vẫn được bật. Nay tắt đồng hồ chỉ cập nhật cờ đồng hồ và đồng bộ các chế độ còn lại. Khi tất cả chế độ đều tắt, dừng foreground service và xóa thông báo. Không dùng thao tác vuốt thông báo để âm thầm tắt cảnh báo pin.

Khởi động/cập nhật gói và cắm/rút sạc dùng cùng chính sách yêu cầu dịch vụ, gồm thông báo sạc, cảnh báo ngưỡng/đầy/nhiệt độ và giọng nói. Không tự bật đồng hồ và không tự đổi quyền hoặc tạo kênh khác để vượt lựa chọn chặn của người dùng.

## Trạng thái và cảnh báo

BatteryInfo phân biệt isPlugged (còn kết nối nguồn) với isCharging (đang nạp điện). Thông báo không trình bày pin đầy hoặc tạm ngừng sạc như vừa rút cáp. Khi không có số đo watt không gán nhãn sạc nhanh. Giữ lại hành vi thông báo trạng thái pin sau khi rút sạc; phần mô tả công tắc giải thích rõ điều này.

BatteryAlertPolicy phát chuông ngưỡng khi mức pin đã đạt/vượt ngưỡng, không giới hạn trong cửa sổ 5%. Chỉ phát cảnh báo đầy khi còn kết nối nguồn, không lặp qua mỗi cập nhật. Chuông đầy ưu tiên hơn chuông ngưỡng trong cùng một sự kiện, rung/chuông chỉ gọi một lần; cảnh báo nhiệt có hysteresis 42/40°C. Rút sạc đặt lại phiên và dừng âm đang phát.

## Xác minh

35 unit test đạt, không có lỗi Android Lint chặn build, assembleRelease thành công. Các bài mới kiểm tra nhảy ngưỡng 79→86%, đầy pin khi còn kết nối nguồn, không báo đầy do rút sạc ở 100%, đặt lại phiên, đổi ngưỡng, nhiệt độ và độc lập giữa đồng hồ/cảnh báo.

scripts/charging_notification_smoke.py dùng giả lập riêng Android 15, đặt lại dữ liệu Mi Tool và mô phỏng pin bằng dumpsys battery. Kiểm tra bật thông báo sạc khi đồng hồ tắt; đọc NotificationRecord của chính Mi Tool, cắm sạc, đầy pin, rút sạc; bật/tắt lối tắt đồng hồ không dừng cảnh báo; tắt từng chế độ và dừng sau chế độ cuối; thu hồi/cấp lại quyền thông báo. Lưu ảnh và cây giao diện ở app/build/ui-qa, reset mô phỏng pin trong finally.

Dumpsys activity services có thể kèm Last ANR service của một ứng dụng khác. Bộ kiểm tra chỉ chấp nhận dòng * ServiceRecord đang hoạt động của đúng NotificationClockService; không dùng kiểm tra tồn tại chuỗi ServiceRecord chung.

Chưa có log hoặc kiểm tra trực tiếp trên Xiaomi 17 Ultra/HyperOS 4 của người dùng. Các bản sửa xử lý các nguyên nhân được tìm thấy trong app, không xác nhận mọi giới hạn của ROM đã được giải quyết. Thông báo màn hình khóa còn phụ thuộc quyền/kênh và cài đặt riêng của hệ thống.

Kiểm thử tích hợp trên APK Release đã đạt: thông báo đang sạc xuất hiện khi đồng hồ tắt, đầy pin vẫn là kết nối nguồn, rút sạc chuyển sang trạng thái pin, lối tắt đồng hồ giữ cảnh báo độc lập, tắt toàn bộ dừng dịch vụ/thông báo; trạng thái quyền bị chặn/cấp lại hiển thị đúng. Ảnh đã ghi nhận thông báo thực sự hiển thị bởi Android System UI.
