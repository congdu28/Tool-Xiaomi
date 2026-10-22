# 📱 Mi Tool (Tool Xiaomi)

Bộ tiện ích mở rộng tối ưu chuyên biệt cho điện thoại **Xiaomi, Redmi, POCO** chạy **HyperOS** hoặc **MIUI**.

![GitHub Release](https://img.shields.io/github/v/release/congdu28/Tool-Xiaomi?color=orange&style=flat-square)
![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

---

## ✨ Tính Năng Nổi Bật

### 1. ⚡ Tích Hợp Shizuku Trực Tiếp (1-Chạm Không Cần Máy Tính)
- **Tự động cấp quyền `WRITE_SECURE_SETTINGS`**: Không cần cắm cáp vào máy tính hay gõ lệnh ADB phức tạp. Chỉ cần mở Shizuku -> Bấm 1 nút trong Mi Tool là xong!
- **1-Chạm Vô Hiệu Hóa / Bật Lại Joyose**: Gỡ bỏ triệt để thủ phạm bóp xung và khóa 89 FPS trong Liên Quân Mobile (AOV) và các game nặng.

### 2. 🔋 Tùy Chỉnh Cảnh Báo Pin & Âm Thanh Cá Nhân Hóa
- **Thanh trượt chọn mức % bảo vệ pin**: Tự do kéo chọn mức sạc mong muốn (50% - 95%, mặc định 80%) thay vì cố định một mức.
- **Tùy chọn file âm thanh / nhạc chuông**: Chọn bất kỳ bản nhạc chuông hoặc file âm thanh từ máy kèm tính năng nghe thử trực tiếp.
- **Cảnh báo đa cấp**: Chuông báo đạt ngưỡng bảo vệ, chuông báo sạc đầy 100%, cảnh báo quá nhiệt (> 42°C), và giọng nói thông báo Tiếng Việt.

### 3. 🎛️ Quick Settings Tiles Bảng Điều Khiển
- **Tile Thông báo trạng thái Mi-Tool**: Bật/tắt thanh trạng thái sạc nhanh từ bất kỳ đâu.
- **Tile Đổi Tần số quét (60Hz / 120Hz)**: Chuyển đổi nhanh giữa 60Hz (tiết kiệm pin) và 120Hz/144Hz (siêu mượt) ngay trên thanh điều khiển kéo xuống.

### 4. 🕒 Đồng hồ giây linh hoạt (3 Phương thức)
- **Cách 1: Thanh thông báo kéo xuống (Notification Drawer):**
  - **100% KHÔNG CẦN ADB / KHÔNG CẦN MÁY TÍNH**.
  - Ghim trực tiếp đồng hồ giây mượt mà vào bảng thông báo hệ thống của Xiaomi, hiển thị kèm % pin và nhiệt độ.
- **Cách 2: Thanh trạng thái góc đỉnh máy (Status Bar SystemUI):**
  - Kích hoạt hiển thị trực tiếp vào góc đồng hồ của hệ thống HyperOS & MIUI qua quyền `WRITE_SECURE_SETTINGS` (cấp tự động qua Shizuku hoặc ADB).
  - Không tốn pin, không chạy service nền.
- **Cách 3: Cửa sổ nổi kéo thả (Floating Window):**
  - Tùy chọn kéo thả tự do đè lên màn hình mọi ứng dụng.

### 5. ⚡ Ước lượng sạc & Giám sát pin từ cảm biến hệ thống
- Tự động nhận diện khi cắm sạc (AC Nhanh, USB, Sạc không dây).
- **Ước lượng thời gian sạc đầy (ETA):** Thuật toán đường cong sạc giảm dòng thông minh sau 80% kết hợp API `BatteryManager.computeChargeTimeRemaining()`.
- **Đo công suất tức thời:** Hiển thị công suất sạc tính theo **Watt** ($V \times A$) và nhiệt độ pin (°C).

### 6. 🔄 Tự Động Kiểm Tra & Cài Đặt Bản Cập Nhật (In-App Updater)
- Tự động kiểm tra bản phát hành mới nhất từ GitHub khi mở ứng dụng.
- Nút kiểm tra thủ công trong tab **Cài đặt**.
- Khi có bản mới, ứng dụng tự động hiển thị danh sách thay đổi và nút **"Tải & Cài đặt ngay"** để cập nhật trực tiếp trên máy mà không cần mở trình duyệt web.

### 7. 🔔 Khắc Phục Trễ Thông Báo (Fix Delayed Notifications)
- **Chống kill app nhắn tin**: Giải quyết triệt để lỗi chậm thông báo và lỡ cuộc gọi trên HyperOS/MIUI cho Zalo, Messenger, Telegram, WhatsApp, Gmail, Teams...
- **1-Chạm tối ưu hàng loạt**: Bỏ qua tối ưu hóa pin (Doze whitelist), cho phép chạy nền và bật tự khởi chạy (MIUI Autostart) qua Shizuku.
- **Lối tắt bảo vệ**: Hướng dẫn khóa ứng dụng trong đa nhiệm (Recent Apps) để bảo vệ 100% không bị dọn RAM.

### 8. 🎮 Mini Game HUD Nổi (FPS & Hiệu Năng Thời Gian Thực)
- **Lớp phủ nổi siêu nhẹ**: Đo FPS thực tế, tần số quét màn hình, % CPU, % RAM, Nhiệt độ và Công suất xả ($W$) khi chơi game.
- **Chạm chuyển chế độ**: Dễ dàng đổi giữa chế độ Gọn (Compact) và Bảng điều khiển (Expanded).
- **Quick Settings Tile**: Bật/tắt HUD nổi ngay từ bảng điều khiển kéo xuống của Xiaomi mà không cần thoát game.

---

## 🔑 Hướng Dẫn Cấp Quyền Shizuku / ADB (Chỉ làm 1 lần)

### Cách 1: Qua Shizuku (Khuyên Dùng - Hoàn Toàn Trên Điện Thoại)
1. Cài đặt và kích hoạt ứng dụng [Shizuku](https://shizuku.rikka.app) qua Ghép đôi không dây (Wireless Debugging) ngay trên điện thoại.
2. Mở **Mi Tool**, bấm nút **"⚡ 1-Chạm Cấp Quyền WRITE_SECURE_SETTINGS"**.
3. Chọn Cho phép trong popup của Shizuku. Toàn bộ tính năng hệ thống sẽ hoạt động ngay lập tức!

### Cách 2: Qua Máy Tính (ADB truyền thống)
```bash
adb shell pm grant app.cd.mitool android.permission.WRITE_SECURE_SETTINGS
```

---

## 📥 Tải Về & Cài Đặt

1. Vào mục [Releases](https://github.com/congdu28/Tool-Xiaomi/releases) của repository.
2. Tải file **`Mi-Tool-vX.X.XX-Release.apk`** về điện thoại.
3. Mở file APK để cài đặt.
4. Mở ứng dụng **Mi Tool**, cấp quyền **Hiển thị trên ứng dụng khác** và làm theo hướng dẫn tối ưu cho Xiaomi.

---

## 🛠️ Hướng Dẫn Tự Biên Dịch (Build Project)

Dự án sử dụng Gradle và Android SDK tiêu chuẩn:

```bash
# Clone repository
git clone https://github.com/congdu28/Tool-Xiaomi.git
cd Tool-Xiaomi

# Chạy test
./gradlew testDebugUnitTest

# Biên dịch Debug APK
./gradlew assembleDebug

# Biên dịch Release APK (Đã ký sẵn với keystore tích hợp)
./gradlew assembleRelease
```

File APK xuất ra tại: `app/build/outputs/apk/release/app-release.apk`.
