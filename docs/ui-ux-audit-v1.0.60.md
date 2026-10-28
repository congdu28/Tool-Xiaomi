# Rà soát UI/UX và logic v1.0.60

## Bố cục và thành phần

Bốn điểm đến cố định: Tổng quan, Tiện ích, Pin, Cài đặt. Danh mục 10 tiện ích dùng chung thành phần FeatureCatalog, FeatureSearch và item_feature; giữ các ID hành động cũ để nối logic hiện có. Tìm kiếm chuẩn hóa dấu tiếng Việt và yêu cầu mọi từ khóa khớp. Tổng quan ưu tiên bốn hành động thường dùng; thông tin phần cứng thu gọn.

Màu trung tính sáng/tối, màu nhấn có tương phản, biểu tượng vector thống nhất. Nút bấm chiều cao tối thiểu 48dp, chữ nhiều dòng khi cần. Các bảng tiện ích mở rộng tối đa 92% màn hình, tiêu đề và nút đóng cố định bên ngoài vùng cuộn; điều chỉnh khi bàn phím hiện. Nhập DNS/Hz dùng TextInputLayout với lỗi ngay tại trường và không đóng khi dữ liệu sai. Tách phần lớn nội dung tĩnh thành resource; vẫn còn một số chuỗi động trong mã Java.

## Các lỗi và thay đổi hành vi

- RAM/widget không còn chạy dọn ứng dụng hàng loạt hoặc tạo số đo giả. Widget mở trang bộ nhớ.
- Không chọn sẵn các ứng dụng khi mở màn hình khắc phục thông báo.
- Joyose/PowerKeeper chạy ngoài luồng giao diện, phản hồi sau kết quả và có xác nhận trước thay đổi. Bỏ lệnh hiệu năng/nhiệt khỏi thao tác gộp. Nội dung thành công không hứa FPS; khôi phục chỉ bật lại Joyose.
- DNS dùng trạng thái đọc lại thay vì viền gợi ý mặc định. Nhập sai tên miền vẫn giữ dữ liệu.
- Cảnh báo pin được đồng bộ độc lập với trạng thái đồng hồ. Cấu hình mới không tự bật giám sát nền; giữ lựa chọn đã lưu của người dùng cũ.
- Dung lượng/SOH không đoán theo số chu kỳ hoặc điện lượng tại thời điểm pin chưa đầy; cache số liệu phần cứng và bỏ shell khỏi đường đọc giao diện.
- Ước lượng thời gian dùng dựa phiên thực tối thiểu 30 phút, sụt ít nhất 2%, không sạc, thời gian hợp lệ; không đủ thì đang thu thập. Không giả định phút trên mỗi phần trăm.
- Đo sạc không kết luận chuẩn sạc/chất lượng cáp hoặc tự suy công suất củ sạc từ hiệu suất giả định.
- Tải cập nhật dùng application context cho receiver; không tải trang HTML như APK và tránh mở bảng trên Activity đã hủy.

## Phạm vi xác minh

Chạy unit test cho tìm kiếm và chính sách ước lượng, cùng các bài kiểm tra pin, nhịp cập nhật, khôi phục Hz và phân tích tụt pin hiện có. Chạy Android Lint và build Release có R8. Script scripts/ui_smoke.py kiểm tra bốn tab, mười bảng tiện ích, lỗi nhập DNS, đường hủy xác nhận và dịch vụ cảnh báo độc lập đồng hồ; ảnh sáng/tối, font 1.3 trên Android 15 giả lập.

Kết quả cục bộ: 21 unit test đạt, 0 lỗi Lint, còn 392 cảnh báo (chủ yếu tài nguyên chưa dùng và chuỗi động chưa tách hết). Build Release và xác minh chữ ký thành công. Kiểm tra tương tác được chạy theo nhóm; đã xác nhận cảnh báo pin tiếp tục khi tắt đồng hồ.

Không có thiết bị Xiaomi thật kết nối trong lượt kiểm tra này. Giả lập không xác minh được component HyperOS 4, quyền Shizuku thực, tác động nhiệt/pin hoặc FPS. Các chỉ số và xác nhận phải được kiểm tra lại trên điện thoại với quyền thực tế. Còn cảnh báo lint không chặn build; không tuyên bố toàn bộ ứng dụng không còn lỗi.
