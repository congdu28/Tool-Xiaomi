# 📱 Mi Tool (Tool Xiaomi)

Bộ tiện ích mở rộng tối ưu chuyên biệt cho điện thoại **Xiaomi, Redmi, POCO** chạy **HyperOS** hoặc **MIUI**.

![GitHub Release](https://img.shields.io/github/v/release/congdu28/Tool-Xiaomi?color=orange&style=flat-square)
![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

---

## ✨ Tính Năng Nổi Bật

### 1. 🕒 Đồng hồ giây nổi (Realtime Floating Clock)
- Hiển thị **Giờ : Phút : Giây** (`HH:mm:ss`) nổi trực tiếp trên màn hình mọi lúc (qua ứng dụng khác, game, màn hình chính).
- Hỗ trợ chế độ **Mili-giây** (`HH:mm:ss.SS`) siêu chuẩn xác cho nhu cầu săn sale, flash sale, canh thời gian.
- Tự do kéo thả vị trí bất kỳ, tự động lưu vị trí đã chỉnh.
- Tùy chỉnh linh hoạt:
  - Cỡ chữ: Nhỏ, Vừa, Lớn.
  - Màu sắc: Trắng, Xanh lá Neon, Cam Xiaomi.
  - Tùy chọn hiển thị kèm % pin.
  - Chức năng **Khóa vị trí** để không vô tình chạm xê dịch khi vuốt màn hình.

### 2. ⚡ Ước lượng sạc & Giám sát pin thông minh (Smart Charging)
- Tự động nhận diện trạng thái khi cắm sạc (Củ sạc AC Nhanh, USB, Sạc không dây).
- **Tính toán thời gian sạc đầy (ETA):** Sử dụng kết hợp thuật toán tính toán đường cong sạc pin và API Android để đưa ra dự kiến chính xác (*Ví dụ: Còn ~24 phút, đầy lúc 21:15*).
- **Đo công suất tức thời (Watt):** Tính toán từ điện áp (V) và dòng điện sạc (mA).
- **Giám sát nhiệt độ pin (°C):** Cảnh báo màu trực quan (Mát < 38°C, Ấm 38-41°C, Nóng > 42°C).
- **Charging HUD (Dynamic Island):** Khi vừa cắm sạc, hiển thị viên thuốc nổi trong 5 giây báo nhanh % pin, thời gian dự kiến đầy và công suất sạc.

### 3. 🔔 Cảnh báo thông minh bảo vệ pin
- Báo chuông & rung khi pin sạc đến **80%** (ngưỡng tối ưu giúp kéo dài tuổi thọ cell pin).
- Báo chuông khi sạc đầy **100%**.
- Cảnh báo khi nhiệt độ pin vượt quá **42°C** (tránh chai pin do quá nhiệt khi vừa sạc vừa dùng hoặc chơi game nặng).

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
