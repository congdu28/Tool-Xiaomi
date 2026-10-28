## Mi Tool v1.0.59 — Giảm tải nền và sửa logic theo dõi pin

- **Đổi tần số quét:** chỉ thay đổi cấu hình màn hình, bỏ tác dụng phụ bật speed_mode và sửa thermal_limit_refresh_rate. Lưu cấu hình trước khi ép Hz để khôi phục, giữ đúng giá trị và các khóa vốn không tồn tại. Auto ở bản nâng cấp chưa có bản lưu sẽ gỡ giới hạn Hz thay vì cố định 60/120 Hz. Chạy thao tác ở nền, kiểm tra tần số hỗ trợ và đọc lại giá trị trước khi lưu trạng thái thành công.
- **Thông báo và đồng hồ:** không tự bật cờ đồng hồ khi dịch vụ được khởi động để theo dõi pin. Đồng hồ giây mặc định tắt nếu chưa từng được chọn. Giảm nhịp cập nhật khi không cần giây, dùng lại số đo pin giữa các lần cập nhật đồng hồ, bỏ kiểm tra cập nhật mạng trong vòng ticker. Khởi tạo trạng thái màn hình thực tế và không lên lịch polling khi màn hình tắt.
- **Widget và giao diện:** bỏ cập nhật toàn bộ widget theo nhịp làm mới trang pin; không tạo alarm khi không có widget. Chỉ đọc cảm biến liên tục ở tab Pin đang hiển thị, giảm nhịp đọc và tránh truy vấn danh sách ứng dụng trùng nhau.
- **Số đo pin:** đổi microampere sang milliampere đúng cho cả dòng rất nhỏ lúc máy nghỉ; xử lý cảm biến không hỗ trợ. Bỏ công suất/dòng điện giả định và dừng đánh giá cáp khi thiếu số đo.
- **Chẩn đoán:** bỏ lịch sử pin tự dựng; biểu đồ bắt đầu thu thập số đo thật ở bản này. So sánh tốc độ tụt pin theo thời gian, gom nhiều mẫu liên tiếp và không suy luận xuyên qua lần sạc hoặc khoảng thiếu dữ liệu. Danh sách thời gian dùng ứng dụng không còn được trình bày như số đo hao pin theo ứng dụng.
- Bỏ xử lý trùng sự kiện cắm/rút sạc giữa dịch vụ thông báo và receiver.

### Kiểm tra và giới hạn

- Chạy unit test, Lint và build Release trước khi phát hành; có kiểm thử riêng cho số đo lúc nghỉ, nhịp polling, phân tích tụt pin và khôi phục cấu hình Hz.
- Chưa đo tác động nhiệt/pin trên điện thoại Xiaomi thật. Bản sửa xử lý tải thừa và dữ liệu gây hiểu nhầm trong Mi Tool; chưa xác định được ứng dụng hoặc phần cứng gây nóng trên từng máy.
- Việc bỏ lịch sử tự dựng khiến biểu đồ cần thu thập dữ liệu mới. Tốc độ tụt pin chưa chứng minh ứng dụng chạy nền là thủ phạm.
- Bản lưu cấu hình Hz chỉ có từ lần áp dụng ở bản mới. Với các thay đổi hiệu năng cũ, hãy kiểm tra và chọn **Cân bằng** trong Cài đặt pin hệ thống; app không đoán giá trị OEM ban đầu của các khóa hiệu năng/nhiệt.

Cài **Mi-Tool-v1.0.59-Release.apk** để cập nhật.
