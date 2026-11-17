# Bật Firebase cho AI Study Mentor

App Android hiện dùng dự án `aistudymentor-78f34` trong `app/google-services.json`.

1. Mở Firebase Console cho dự án trên. Vào **Authentication → Sign-in method** và bật **Email/Password**.
2. Vào **Firestore Database** và tạo cơ sở dữ liệu mặc định nếu chưa có.
3. Trong **Firestore Database → Rules**, kiểm tra các quy tắc đang dùng và thêm phần dành cho app từ `firestore.rules`. Với dự án chỉ dùng cho app này, có thể dùng toàn bộ file. Bấm **Publish**.
4. Chạy bản debug. Tạo tài khoản, vào **Cá nhân → Chỉnh thiết lập**, lưu tùy chọn, rồi thử hỏi Vilao và tạo một bài luyện tập.
5. Mở **Lịch sử**, đánh dấu một bài và trả lời quiz. Đăng xuất rồi đăng nhập lại để kiểm tra lịch sử, bookmark, đáp án đã chọn và tiến độ. Trong Firestore Console, kiểm tra dữ liệu tại `users/{uid}/studyEntries`.

Firebase Authentication quản lý tài khoản và mật khẩu. Firestore lưu tùy chọn trong `users/{uid}`, và mỗi câu hỏi, lời giải hoặc bài trắc nghiệm trong một tài liệu riêng. Quy tắc chỉ cho tài khoản đã đăng nhập đọc hoặc ghi dữ liệu thuộc UID của chính mình. Thống kê được tính từ lịch sử. Ảnh đề gốc và khóa Vilao không được ghi vào Firestore.
