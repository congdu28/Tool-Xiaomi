# Tool Xiaomi — Quy tắc phát hành sau khi sửa code

Mỗi khi hoàn tất một đợt chỉnh sửa code/tài nguyên trong dự án này (trừ khi người dùng yêu cầu rõ không push/release):

1. **Tăng phiên bản** trong `app/build.gradle`:
   - Tăng `versionCode` lên `+1`.
   - Tăng `versionName` theo định dạng `1.0.<versionCode>` (ví dụ `1.0.66` → `1.0.67`).
2. **Cập nhật ghi chú phát hành** trong `RELEASE_NOTES.md`:
   - Tóm tắt ngắn gọn các thay đổi chính của phiên bản mới và ghi đúng tên file `Mi-Tool-v<versionName>-Release.apk`.
3. **Kiểm tra trước khi push** bằng lệnh tương đương GitHub Actions:
   - Chạy `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`.
   - Chỉ tiếp tục khi cả 3 bước đều **PASS** (`BUILD SUCCESSFUL`). Nếu lỗi, sửa dứt điểm rồi chạy lại.
4. **Commit, tạo tag và push lên `origin`**:
   - Commit toàn bộ thay đổi liên quan kèm version trong commit message (ví dụ `fix(...): ... (v1.0.67)`).
   - Tạo tag `v<versionName>` (ví dụ `v1.0.67`).
   - Push cả nhánh `main` và tag mới: `git push origin main v<versionName>` để kích hoạt `.github/workflows/build-and-release.yml` tự động phát hành APK trên GitHub Releases.
5. **Báo cáo kết quả**:
   - Đính kèm commit hash, tag đã push, đường dẫn APK cục bộ (`app/build/outputs/apk/release/app-release.apk`) và link trang Releases.
