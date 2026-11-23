# Mi Tool v1.0.66 — Tối ưu chống nóng theo hồ sơ game (Thiên CPU / Thiên GPU)

- Phân loại tự động đặc tính tải của từng game trong **Chống quá nhiệt & Ổn định FPS**:
  - **Game thiên CPU** (Liên Quân Mobile, Honor of Kings, Vương Giả Vinh Diệu, LMHT: Tốc Chiến, Mobile Legends, Roblox, Play Together, Minecraft): giữ nguyên **100% độ phân giải gốc** để không làm mờ giao diện/bản đồ nhỏ, tập trung giảm tải CPU qua khóa trần FPS (`60`/`90 FPS`), đồng bộ Hz màn hình, biên dịch ART `speed-profile` và giữ Joyose bảo vệ nhiệt.
  - **Game thiên GPU (3D nặng)** (Genshin Impact, Honkai: Star Rail, Zenless Zone Zero, Wuthering Waves, PUBG Mobile, COD Mobile, CarX Street, Free Fire MAX): áp dụng thêm giảm tỉ lệ dựng hình GPU (`80%` / `85%`) kết hợp trần FPS.
- Bổ sung hủy tác vụ biên dịch nền (`cmd package bg-dexopt-job --cancel`) cùng `am kill-all` để giảm tranh chấp CPU nền giữa trận mà vẫn giữ nguyên thông báo.
- Sửa nhận diện trạng thái can thiệp GameManager chính xác hơn và hiển thị rõ nhãn `(Thiên CPU)` / `(Thiên GPU)` ngay cạnh tên game.
- Kiểm tra tự động bằng unit test, lint và bản dựng Release APK qua GitHub Actions.

Tải **Mi-Tool-v1.0.66-Release.apk** sau khi quy trình Build and Release APK trên GitHub Actions hoàn tất.
