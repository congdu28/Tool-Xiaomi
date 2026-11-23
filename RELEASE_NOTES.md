# Mi Tool v1.0.64 — Tối ưu Shizuku chống quá nhiệt & ổn định FPS

- Chuyển trọng tâm tối ưu game sang cơ chế giảm tải nhiệt qua Shizuku để hạn chế nóng máy gây tụt FPS khi chơi Liên Quân Mobile và các game khác:
  - Thêm các cấu hình khuyến nghị (**Mát 60 FPS · Scale 80%**, **Cân bằng 90 FPS · Scale 85%**) và chế độ **Tùy chỉnh** qua Android `GameManager` (`cmd game mode` trên Android 12+, `cmd game set --fps --downscale` trên Android 13+) kèm bước đọc lại `cmd game list-configs` để xác minh kết quả thực tế.
  - Giữ Joyose và dữ liệu Pin & Hiệu suất hoạt động mặc định để bảo vệ đường cong nhiệt của ROM (tự bật lại Joyose nếu trước đó đang bị tắt); tách thao tác tắt Joyose & xóa PowerKeeper thành mục phá giới hạn có cảnh báo riêng cho trường hợp dùng tản nhiệt ngoài.
  - Hỗ trợ kiểm tra trạng thái dexopt và biên dịch ART `speed-profile` (không ép biên dịch lại nếu đã đạt chuẩn để tránh nóng máy), dọn cache tiến trình nền an toàn (`am kill-all`), đồng bộ tần số quét màn hình khi tấm nền hỗ trợ và khôi phục mặc định khi cần.
- Bổ sung theo dõi trạng thái nhiệt hệ thống (`PowerManager.getCurrentThermalStatus`) trong bảng tối ưu game và trên Game HUD nổi; giảm tần suất đọc `/proc/stat` và công suất pin khi HUD ở chế độ thu gọn.
- Bổ sung unit test cho `GameThermalPolicy` (chưa chạy Gradle tại máy local do chưa cài JDK/Android SDK; sẽ kiểm tra test, lint và build Release APK trên GitHub Actions).

Tải **Mi-Tool-v1.0.64-Release.apk** sau khi quy trình Build and Release APK trên GitHub Actions hoàn tất.
