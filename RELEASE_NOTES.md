# Mi Tool v1.0.63 — Sửa âm báo sạc và bỏ các mục lặp

- Mỗi sự kiện **Cắm sạc, Rút sạc, Đạt ngưỡng, Đầy pin, Quá nhiệt** chỉ có một lựa chọn: Tắt / Chuông / Giọng nói / Chuông + giọng nói. Bỏ hai cụm công tắc gây nhầm lẫn; giữ trạng thái có hiệu lực từ cấu hình cũ.
- Thêm chế độ chuông cho cắm/rút sạc. Hai mục này trước đây chỉ điều khiển giọng nói, không phải chuông thông thường.
- Kiểm tra bộ đọc và dữ liệu tiếng Việt; xử lý lỗi khởi tạo, lỗi phát và trường hợp bộ đọc không phản hồi. Không tự đổi sang ngôn ngữ khác rồi báo sẵn sàng.
- Nếu giọng nói không hoạt động, dùng chuông dự phòng một lần cho sự kiện. Chế độ cả hai không phát thêm chuông dự phòng khi chuông đã được phát.
- Gộp xử lý sự kiện nguồn vào dịch vụ theo dõi để không phát trùng giữa các broadcast và không phụ thuộc tuổi thọ của receiver. Không bỏ mất cắm/rút nhanh do giới hạn 3 giây cũ.
- Dùng hàng đợi giọng nói, tránh câu sau cắt mất câu trước. Thêm thời hạn chờ và ngăn giọng nói phát muộn sau khi đã chuyển sang chuông dự phòng.
- Tách chuông nghe thử khỏi chuông thật, tránh đóng bảng/chuyển sang Ambient làm ngắt âm báo sạc. Hiển thị cảnh báo âm lượng báo thức bằng 0 và nút mở cài đặt âm lượng/giọng đọc.
- Có nút Thử chuông và Thử giọng nói, hiển thị trạng thái lỗi cụ thể. Chuông hoạt động độc lập với bộ đọc tiếng Việt.

Kiểm tra unit test, lint và build Release; kiểm tra Android bắt đầu phát chuông khi cắm/rút, TTS bằng bộ đọc thử nghiệm và chuông dự phòng khi bộ đọc lỗi. Bộ đọc thử nghiệm chỉ tạo PCM để kiểm tra hợp đồng TTS, không kiểm tra chất lượng đọc tiếng Việt. Chưa kiểm tra nghe trực tiếp trên Xiaomi 17 Ultra/HyperOS 4.

Tải **Mi-Tool-v1.0.63-Release.apk**. Muốn có tiếng báo cắm sạc ngay: vào Cảnh báo pin và sạc → Khi cắm sạc → Chuông, rồi bấm Thử chuông.
