# Âm báo sạc và giao diện sự kiện — v1.0.63

## Nguyên nhân trong mã

Hai công tắc cắm/rút sạc cũ chỉ bật TTS. Không có giọng đọc tiếng Việt hoặc lỗi bộ đọc có thể khiến im lặng: kết quả speak bị bỏ qua, lỗi khởi tạo giữ instance chưa sẵn sàng, tự rơi sang ngôn ngữ mặc định và thiếu callback lỗi. QUEUE_FLUSH trong khi xả hàng đợi có thể làm câu cuối cắt các câu trước. Khai báo truy vấn TTS service đã được bổ sung theo Android API.

Chuông thật và nghe thử dùng chung activeRingtone. MainActivity.onPause và đóng bảng gọi stopAlertTone, có thể cắt chuông khi chuyển sang Ambient. Nay previewRingtone riêng và chỉ ngừng preview ở vòng đời giao diện; dịch vụ giữ quyền sở hữu âm báo tự động.

## Cấu hình và xử lý

AlertAudioSettings đọc trạng thái hiệu lực của các cờ chuông/giọng nói cũ. Mỗi sự kiện có mode bit OFF/RING/VOICE/BOTH. Đổi một sự kiện chụp lại cả năm mode trước, ghi atomically các cờ cũ và master tổng để không bật lại các cờ giọng nói từng bị master tắt. Chuông cắm/rút thêm cờ riêng, mặc định tắt.

PowerConnectionPolicy gộp battery-change, power broadcast và lệnh đánh thức dịch vụ. Sticky ban đầu không phát tiếng; sự kiện nguồn thực khi khởi động lạnh được nhận một lần; đối chiếu trạng thái pin hiện tại để bỏ lệnh nguồn cũ tới muộn. Receiver không trực tiếp phát TTS nữa. Dịch vụ giữ chạy khi có bất kỳ mode âm báo được bật, kể cả chỉ chuông kết nối.

TTS kiểm tra tiếng Việt, dùng âm lượng báo thức, QUEUE_ADD, kiểm tra mã trả về và UtteranceProgressListener. Khởi tạo/chờ bắt đầu có timeout 8 giây. Fallback tối đa một lần mỗi request; dừng hàng đợi bị treo trước khi chuyển dự phòng để tránh giọng phát muộn. Không tự tăng âm lượng hoặc bỏ qua Không làm phiền. Chuông tùy chọn lỗi sẽ thử các âm mặc định hệ thống. Hiển thị cảnh báo âm lượng báo thức bằng 0.

## Kiểm tra

PowerAudioPolicyTest kiểm tra sticky, gộp broadcast, đánh thức lạnh, cắm/rút nhanh và trạng thái hiệu lực của cấu hình cũ. Giữ các bài ngưỡng/full/temperature, service lifecycle, Shizuku, RAM, DNS hiện có.

scripts/battery_audio_smoke.py đặt lại dữ liệu trên giả lập riêng và kiểm tra cắm/rút ở chế độ giọng nói khi không có bộ đọc Việt: MiToolAudio phải ghi ring_started=true, mỗi event một lần. Đây là kiểm tra Ringtone.isPlaying của Android, không chỉ gọi hàm hoặc hiển thị thông báo.

scripts/fixtures/tts là bộ đọc thử nghiệm sinh PCM, không đọc tiếng Việt thật. scripts/build_tts_fixture.py dùng JDK17/SDK35/build-tools34 để tạo APK fixture, yêu cầu khóa thử và mật khẩu qua MITOOL_QA_SIGNING_PASSWORD. Chỉ cài vào giả lập. scripts/tts_engine_smoke.py tạm đổi bộ đọc mặc định rồi khôi phục trong finally; xác nhận hai onStart cho cắm/rút, lỗi synthesis được chuyển sang một chuông dự phòng và hiện lỗi trong giao diện. Fixture không được đóng gói trong Mi Tool.

Đã thử trường hợp engine không hoàn tất callback: watchdog chuyển dự phòng sau 8 giây. Đã thử riêng error + done theo hợp đồng SynthesisCallback để xác minh đường onError. Không khẳng định đã nghe âm thanh thực trên điện thoại người dùng; cần kiểm tra lại volume/DND/bộ đọc của HyperOS 4.

Kết quả bản Release: 41 unit test đạt, 0 lỗi lint; APK versionCode 63, versionName 1.0.63 dùng cùng chứng thư phát hành. Giả lập API35 không cho đặt âm lượng báo thức về 0 (tối thiểu 1), nên phần UI cảnh báo mức 0 chưa được xác minh trực tiếp.
