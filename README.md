# 📱 Mi Tool (Tool Xiaomi)

Bộ tiện ích mở rộng tối ưu chuyên biệt cho điện thoại **Xiaomi, Redmi, POCO** chạy **HyperOS** hoặc **MIUI**.

![GitHub Release](https://img.shields.io/github/v/release/congdu28/Tool-Xiaomi?color=orange&style=flat-square)
![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

---

## ✨ Tính Năng Nổi Bật

### 1. 🕒 Đồng hồ giây linh hoạt (3 Phương thức)
- **Cách 1: Thanh thông báo kéo xuống (Notification Drawer):**
  - **100% KHÔNG CẦN ADB / KHÔNG CẦN MÁY TÍNH**.
  - Ghim trực tiếp đồng hồ giây mượt mà vào bảng thông báo hệ thống của Xiaomi, hiển thị kèm % pin và nhiệt độ.
- **Cách 2: Thanh trạng thái góc đỉnh máy (Status Bar SystemUI):**
  - Kích hoạt hiển thị trực tiếp vào góc đồng hồ của hệ thống HyperOS & MIUI qua quyền `WRITE_SECURE_SETTINGS` (cấp qua Shizuku không dây ngay trên máy hoặc ADB).
  - Không tốn pin, không chạy service nền.
- **Cách 3: Cửa sổ nổi kéo thả (Floating Window):**
  - Tùy chọn kéo thả tự do đè lên màn hình mọi ứng dụng.

### 2. ⚡ Ước lượng sạc & Giám sát pin từ cảm biến hệ thống
- Tự động nhận diện khi cắm sạc (AC Nhanh, USB, Sạc không dây).
- **Ước lượng thời gian sạc đầy (ETA):** Thuật toán đường cong sạc giảm dòng thông minh sau 80% kết hợp API `BatteryManager.computeChargeTimeRemaining()`.
- **Đo công suất tức thời:** Hiển thị công suất sạc tính theo **Watt** ($V \times A$) và nhiệt độ pin (°C).
- **Cảnh báo pin:** Báo chuông khi đạt 80% (bảo vệ pin), khi sạc đầy 100%, hoặc quá nhiệt (> 42°C).

### 3. 🔄 Tự Động Kiểm Tra & Cài Đặt Bản Cập Nhật (In-App Updater)
- Tự động kiểm tra bản phát hành mới nhất từ GitHub khi mở ứng dụng.
- Nút kiểm tra thủ công trong tab **Cài đặt**.
- Khi có bản mới, ứng dụng tự động hiển thị danh sách thay đổi và nút **"Tải & Cài đặt ngay"** để cập nhật trực tiếp trên máy mà không cần mở trình duyệt web.

### 4. 📱 Giao diện 3 Tab hiện đại & Chống tràn viền
- Phân chia bố cục khoa học: **[Đồng Hồ] - [Pin & Sạc] - [Cài Đặt]**.
- Xử lý triệt để vùng an toàn (Window Insets) chống tai thỏ/nốt ruồi che khuất và chống cấn phím điều hướng khi cuộn.
- Dung lượng APK siêu nhẹ (~2.0 MB).

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
