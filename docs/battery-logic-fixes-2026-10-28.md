# Rà soát logic nhiệt/pin — 28/10/2026

## Nguyên nhân trong code và bản sửa

1. Đổi Hz tự bật speed_mode và thay đổi thermal_limit_refresh_rate: bỏ các thay đổi ngoài phạm vi màn hình; gom lệnh, lưu và khôi phục cấu hình gốc của 8 khóa refresh, chạy ở nền và xác minh readback. Chưa có snapshot ở bản cũ thì Auto bỏ giới hạn min/peak và yêu cầu chế độ hệ thống, không giả định màn hình 120Hz. Các giá trị hiệu năng/nhiệt đã đổi ở bản cũ không tự phục hồi vì không có dữ liệu gốc.
2. Dịch vụ thông báo mặc định bật đồng hồ và luôn đặt cờ true khi khởi động: mặc định opt-in, chỉ ACTION_START bật cờ; chế độ cảnh báo pin không tự bật đồng hồ. Polling màn hình tắt bằng 0; clock 1s khi được chọn, sạc 5s, pin 60s; cảnh báo vẫn theo broadcast.
3. Mỗi giây dựng notification đọc lại dữ liệu pin và định kỳ kiểm tra mạng: cache đọc tối đa mỗi 5s khi chạy ticker, bỏ kiểm tra cập nhật từ ticker/service. Kiểm tra cập nhật thủ công/Activity và lịch updater hiện có vẫn hoạt động.
4. Đồng hồ/HUD khởi tạo isScreenOn=true kể cả lúc máy nghỉ: lấy trạng thái PowerManager và chặn post khi màn hình tắt; chống trùng runnable HUD.
5. Widget cập nhật từ updateSotView mỗi 15s: tách khỏi nhịp UI; giữ cập nhật theo sự kiện và lịch widget. Không đặt lịch khi không có widget.
6. Cảm biến UI đọc ở mọi tab và truy vấn UsageStats có thể trùng: chỉ ticker ở tab Pin, nhịp 5s; coalesce truy vấn, chặn render dữ liệu bộ lọc cũ/Activity đã hủy.
7. Dòng nhỏ dưới 10.000 microampere bị coi như milliampere: thống nhất đơn vị theo BatteryManager; bỏ dữ liệu công suất/dòng giả định, xử lý sentinel Integer.MIN_VALUE.
8. History tự tạo 24h từ dữ liệu không đo được: dùng khóa lịch sử measured v3, chỉ lưu điểm thực; throttle trước khi parse JSON. Phân tích khoảng liên tục >=15 phút, tránh gán nhầm một mức tụt chậm thành bất thường. Các khoảng cách >30 phút/sạc/nhảy đồng hồ không được coi là đo liên tục.
9. % pin ứng dụng được suy ra từ thời gian foreground và giả định 11%/giờ: bỏ con số này, hiển thị đúng thời gian dùng; không gán nguyên nhân chạy nền chỉ từ biểu đồ mức pin.
10. Power events bị xử lý ở cả receiver và service: receiver sở hữu xử lý cắm/rút sạc, tránh reset SOT/voice hai lần.

## Kiểm tra thiết bị cần thực hiện

- So sánh 30–60 phút dùng nhẹ trên cùng mạng/độ sáng giữa hai bản; đo bằng thống kê pin hệ thống. Không dùng dữ liệu tự dựng của bản cũ làm baseline.
- Khởi động/restart service khi màn hình tắt: không có ticker chạy định kỳ; battery broadcast vẫn có thể xử lý cảnh báo.
- Khi chỉ bật cảnh báo pin, cờ đồng hồ vẫn tắt; chọn đồng hồ mới cập nhật giây. Test tắt/mở màn hình nhiều lần không nhân đôi runnable.
- Thử đổi Hz có/không quyền, tần số không hỗ trợ, và Auto: đọc lại Settings; phục hồi đúng cấu hình trước lần ép Hz. Xác nhận không ghi speed_mode/thermal_limit_refresh_rate từ luồng đổi Hz.
- Giữ ứng dụng nền 30 phút; thử widget 0/1/nhiều chiếc, thêm/xóa widget; kiểm tra lịch được hủy sau khi xóa hết.
- Chưa xác nhận tương thích HyperOS 4 hoặc tác động nhiệt/pin thực tế vì không có điện thoại ADB kết nối.

## Kết quả kiểm tra local

- 15/15 unit test đạt (9 test bổ sung cho logic nhiệt/pin và khôi phục Hz).
- Lint 0 lỗi; vẫn có cảnh báo cũ về text/UI/API cần xử lý theo đợt riêng.
- Build Release với R8 thành công. Chưa đo mức giảm nhiệt/tiêu thụ thực tế trên thiết bị.
