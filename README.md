# 📱 Mi Tool (Tool Xiaomi)

Bộ tiện ích mở rộng tối ưu chuyên biệt cho điện thoại **Xiaomi, Redmi, POCO** chạy **HyperOS** hoặc **MIUI**.

![GitHub Release](https://img.shields.io/github/v/release/congdu28/Tool-Xiaomi?color=orange&style=flat-square)
![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

---

## ✨ Tính Năng Nổi Bật

### 1. 🕒 Đồng hồ giây trên thanh trạng thái hệ thống (Native System Status Bar Clock)
- Kích hoạt hiển thị trực tiếp **Giờ : Phút : Giây** (`HH:mm:ss`) trên thanh thông báo / thanh trạng thái của hệ điều hành Xiaomi HyperOS & MIUI.
- **Ưu điểm vượt trội:**
  - Do chính **SystemUI** của hệ thống vẽ ra, không cần vẽ cửa sổ ảo đè lên màn hình.
  - **Không tốn một chút pin hay CPU nào**, không cần chạy service nền.
  - Đồng bộ chuẩn xác 100% với đồng hồ mạng của hệ thống.
  - Được bật/tắt an toàn thông qua quyền hệ thống `WRITE_SECURE_SETTINGS` (cấp 1 lần duy nhất qua máy tính ADB hoặc Shizuku).

### 2. ⚡ Ước lượng sạc & Giám sát pin từ API hệ thống (Smart Charging)
- Tự động nhận diện khi cắm sạc (`AC Nhanh`, `USB`, `Sạc không dây`).
- **Ước lượng thời gian sạc đầy (ETA):** Sử dụng API `BatteryManager.computeChargeTimeRemaining()` của Android kết hợp đường cong sạc pin giảm dòng sau 80% để tính toán thời gian chính xác (*Ví dụ: Còn ~24 phút • Đầy lúc 21:15*).
- **Thông báo sạc hệ thống:** Tự động gửi thông báo native khi vừa cắm sạc, và tự hủy khi rút sạc mà không cần chạy nền liên tục.
- **Đo công suất tức thời:** Hiển thị công suất sạc tính theo **Watt** ($V \times A$) và nhiệt độ pin (°C).
- **Cảnh báo pin thông minh:** Báo chuông khi đạt 80% (bảo vệ pin), khi sạc đầy 100%, hoặc cảnh báo quá nhiệt (> 42°C).

### 3. 🛠️ Khắc phục tối ưu cho HyperOS & Xiaomi
- Tương thích hoàn toàn với Android 14+ (HyperOS) với đầy đủ cờ bảo mật `RECEIVER_NOT_EXPORTED`.
- Khởi động tức thì dưới 100ms, không gây nóng máy hay xung đột tiến trình.
- Tích hợp sẵn nút sao chép lệnh ADB và kiểm tra quyền trực tiếp trong ứng dụng.

---

## 🔑 Hướng Dẫn Cấp Quyền Đồng Hồ Giây Hệ Thống (Chỉ cần làm 1 lần)

Để bật được giây trực tiếp trên thanh trạng thái của Xiaomi:
1. Trên điện thoại Xiaomi: Vào **Cài đặt** -> **Cài đặt bổ sung** -> **Tùy chọn nhà phát triển**:
   - Bật **Gỡ lỗi USB**.
   - Bật **Gỡ lỗi USB (Cài đặt bảo mật)**.
2. Kết nối điện thoại với máy tính (hoặc mở ứng dụng Shizuku / LADB trên điện thoại) và chạy lệnh:
```bash
adb shell pm grant app.cd.mitool android.permission.WRITE_SECURE_SETTINGS
```
3. Mở ứng dụng **Mi Tool**, bấm **Kiểm tra lại quyền** và gạt bật **Đồng hồ giây hệ thống**. Giờ trên thanh trạng thái sẽ lập tức nhảy thêm giây!

### 4. 🛠️ Hướng dẫn tối ưu đặc thù cho Xiaomi (HyperOS / MIUI)
Tích hợp sẵn các nút tắt 1-chạm mở trực tiếp cài đặt hệ thống của Xiaomi:
1. **Cấp quyền Cửa sổ nổi (Overlay):** Cho phép đồng hồ và banner sạc nổi trên các ứng dụng khác.
2. **Quyền Tự khởi chạy (Autostart):** Đảm bảo tiện ích tự bật lại khi khởi động lại máy.
3. **Tiết kiệm pin - Không giới hạn (No restrictions):** Ngăn MIUI/HyperOS vô tình tắt tiến trình ngầm.
4. **Quyền hiển thị trên màn hình khóa.**
5. **Phím tắt mở thông tin pin ẩn của Xiaomi.**

---

## 📥 Tải Về & Cài Đặt

1. Vào mục [Releases](https://github.com/congdu28/Tool-Xiaomi/releases) của repository.
2. Tải file **`Tool-Xiaomi-Release.apk`** về điện thoại.
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
