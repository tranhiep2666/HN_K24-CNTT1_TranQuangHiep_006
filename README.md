Công nghệ AI sử dụng : Gemini

Phần I : TÁI CẤU TRÚC HỆ THỐNG ĐỂ DỄ MỞ RỘNG

1. Lịch sử prompt AI

- Prompt 1 : "Tôi có một hàm processTransfer trong lớp TransferService xử lý chuyển tiền ví điện tử. Hiện tại class này đang vi phạm nguyên tắc Open/Closed Principle vì mỗi lần thêm ngân hàng mới hoặc thay đổi hình thức thông báo (SMS sang Push Notification) đều phải sửa trực tiếp bằng if-else. Hãy phân tích lỗi cấu trúc này và đề xuất giải pháp refactor bằng Java."

- Prompt 2: "Bây giờ hãy sinh code hoàn chỉnh cho tôi. Hãy tách riêng chiến lược tính phí và định tuyến ngân hàng thành mẫu Strategy Pattern. Với phần thông báo, hãy dùng giải pháp không làm ảnh hưởng đến hàm tính toán cốt lõi."

- Prompt 3: "Phần thông báo dùng interface NotificationService truyền trực tiếp vào hàm vẫn khiến hàm core phải gọi thủ công. Hãy cải tiến bằng cách sử dụng cơ chế Event-Driven (ApplicationEventPublisher của Spring) để khi giao dịch thành công sẽ bắn ra một Event, bên thông báo tự lắng nghe. Hãy cập nhật lại code."

2. Phân tích lỗi AI ở lần sinh đầu tiên & Cách khắc phục

- Điểm AI làm sai/chưa tối ưu: Ở lần sinh đầu tiên (sau Prompt 1 & 2), AI đã tách được TransferStrategy ra rất tốt, tuy nhiên với logic thông báo, AI chỉ đơn giản là inject thêm một NotificationService vào trong TransferService và gọi notificationService.send(...) ở cuối hàm. Điều này chưa tối ưu vì hàm cốt lõi processTransfer vẫn phải chủ động biết và gọi dịch vụ thông báo. Nếu tương lai muốn tắt thông báo hoặc thêm kênh thông báo mới (Email, Webhook), ta vẫn phải can thiệp vào luồng điều hướng của lớp dịch vụ chính.

- Cách khắc phục: Tôi đã ra lệnh cho AI (ở Prompt 3) chuyển đổi sang kiến trúc hướng sự kiện (Event-Driven) bằng cách sử dụng ApplicationEventPublisher của Spring Framework. Khi giao dịch thành công, hệ thống chỉ phát đi một TransactionSuccessEvent. Việc xử lý gửi SMS, Push hay Email sẽ do các lớp Listener độc lập đảm nhận, hàm core hoàn toàn cô lập.

Phần II: DEBUGGING BẢO MẬT VÀ XỬ LÝ LỖI HỆ THỐNG

1. Phân tích nguyên nhân gốc rễ (Root Cause Analysis)

Khi UserRegistrationService ném ra ngoại lệ IllegalArgumentException, ngoại lệ này không được bắt ở bất kỳ đâu trong UserController. Theo luồng mặc định của Spring Web, ngoại lệ bị đẩy ngược lên Container (Tomcat). Do không có cơ chế chặn bắt, Spring Boot sẽ kích hoạt cấu hình mặc định là BasicErrorController để xử lý. Vì đây là một ngoại lệ chưa được định nghĩa ánh xạ mã HTTP, server coi đó là một lỗi không mong muốn từ hệ thống và tự động trả về HTTP Status 500 Internal Server Error kèm theo trang HTML trắng (White-label Error Page). Điều này vừa gây trải nghiệm xấu cho người dùng, vừa làm lộ thông tin Stack Trace của hệ thống, dẫn đến nguy cơ bảo mật nghiêm trọng.

2. Lịch sử Prompt (Prompt Chain)

- Prompt 1: "Ứng dụng Spring Boot của tôi đang bị quăng lỗi 500 White-label Error Page khi registerUser ném ra IllegalArgumentException. Hãy giải thích tại sao luồng request lại bị thế và cho tôi giải pháp giải quyết triệt để dùng @RestControllerAdvice."

- Prompt 2: "Hãy sinh cho tôi mã nguồn lớp Global Exception Handler trả về format JSON chính xác là {"error": "INVALID_INPUT", "message": "..."} và mã HTTP Status là 400 Bad Request."

3. Phân tích lỗi AI ở lần sinh đầu tiên & Cách khắc phục

- Điểm AI làm sai/chưa tối ưu: Lần đầu tiên, AI sinh ra một class sử dụng một Map chung chung kiểu Map<String, String> để trả về lỗi. Điều này không tối ưu vì thiếu tính đồng nhất dữ liệu (Type-safe), gây khó khăn khi tái sử dụng cho các loại Exception khác nhau trong toàn dự án.

- Cách khắc phục: Tôi đã yêu cầu AI tạo riêng một DTO chuyên biệt đặt tên là ErrorResponse để đóng gói cấu trúc dữ liệu trả về cố định, giúp hệ thống chuẩn hóa định dạng thông điệp gửi ra Frontend.

4. Tại sao không nên sử dụng try-catch rải rác ở các Controller?

- Vi phạm nguyên tắc DRY (Don't Repeat Yourself): Nếu dự án có hàng trăm API, việc hàm nào cũng viết khối lệnh try-catch lặp đi lặp lại sẽ làm phình to mã nguồn một cách vô nghĩa.

- Làm mờ logic nghiệp vụ chính: Code xử lý chính của Controller bị che khuất bởi quá nhiều dòng lệnh xử lý lỗi, gây khó khăn cho việc bảo trì, đọc hiểu mã nguồn.

- Không đồng nhất cấu trúc dữ liệu: Các lập trình viên khác nhau trong đội nhóm có thể trả về cấu trúc lỗi khác nhau (người trả về chuỗi trực tiếp, người trả về object), làm Frontend gặp lỗi khi xử lý dữ liệu.

Phần III: PHÂN TÍCH VÀ THIẾT KẾ HỆ THỐNG VỚI AI

- Nhiệm vụ 1: Đề xuất Giải pháp Công nghệ (Tech Stack)

Prompt: "Tôi đang làm System Analyst cho dự án Monolithic Web có tên 'ArtExhibition Management'. Hệ thống cần quản lý 3 phân quyền, upload ảnh nghệ thuật, phân trang, tìm kiếm động và đặc biệt bán vé giới hạn có VIP ưu tiên đặt trước 24h, cần an toàn transaction chống tranh chấp đồng thời. Hãy đề xuất một Tech Stack Java hoàn chỉnh và giải thích lý do thuyết phục."

Nhận xét phản biện: 

ĐỒNG Ý phần lớn với đề xuất của AI. Việc chọn Spring Boot cho mô hình Monolithic giúp tối ưu thời gian phát triển, tận dụng tốt hệ sinh thái Spring Security để phân quyền 3 Role (Visitor, Artist, Admin) chặt chẽ.

BỔ SUNG VÀ PHẢN BIỆN BIỆN PHÁP CONCURRENCY: AI đề xuất Pessimistic Locking cho việc bán vé là chính xác tại thời điểm lượng truy cập cao để tránh hiện tượng Overbooking (bán vượt số vé hiện có). Tuy nhiên, nếu lượng người dùng mua vé VIP đồng thời quá lớn, khóa bi quan sẽ làm giảm hiệu năng hệ thống đáng kể do các luồng phải xếp hàng đợi nhau giải phóng khóa database. Tôi đề xuất cải tiến: Sử dụng giải pháp lai. Dùng Optimistic Locking (Khóa lạc quan với thuộc tính @Version) cho luồng mua vé thông thường giai đoạn đầu, kết hợp cơ chế hàng đợi hoặc Redis Distributed Lock ở tầng ứng dụng nếu hệ thống mở rộng quy mô lớn để bảo vệ Database khỏi việc bị nghẽn (Bottleneck).

- Nhiệm vụ 2: Phân tích Thực thể (Entity Analysis)

Prompt: "Hãy phân tích nghiệp vụ dự án ArtExhibition Management trên để xác định các thực thể (Entities) cốt lõi của Database kèm theo thuộc tính quan trọng. Lưu ý không dùng kiểu cấu trúc mảng (array) trong các thuộc tính cơ bản nhằm tối ưu hóa quan hệ dữ liệu."

Danh sách Thực thể (Entities):
User: id (PK), username, password, email, role (VISITOR, ARTIST, ADMIN), created_at.

ArtistProfile: id (PK), user_id (FK to User), biography, portfolio_url.

Artwork: id (PK), title, description, price, image_url, category, artist_id (FK to ArtistProfile), created_at.

Exhibition: id (PK), name, description, start_date, end_date, total_tickets, available_tickets, vip_early_access_start.

Ticket: id (PK), exhibition_id (FK to Exhibition), price, ticket_type (VIP, REGULAR).

Booking: id (PK), user_id (FK to User), ticket_id (FK to Ticket), quantity, total_price, status (PENDING, CONFIRMED, CANCELLED), booking_time.

- Nhiệm vụ 3: Thiết kế Sơ đồ quan hệ thực thể (ERD)

Prompt: "Dựa trên các thực thể trên, hãy viết đoạn mã bản vẽ sơ đồ quan hệ thực thể ERD bằng ngôn ngữ Mermaid để tôi có thể render ra hình ảnh."


Đoạn mã: 

erDiagram
    USER ||--o| ARTIST_PROFILE : "has"
    USER ||--o{ BOOKING : "places"
    ARTIST_PROFILE ||--o{ ARTWORK : "creates"
    EXHIBITION ||--o{ TICKET : "offers"
    TICKET ||--o{ BOOKING : "is_selected_in"

    USER {
        Long id PK
        String username
        String password
        String email
        String role
        Timestamp created_at
    }

    ARTIST_PROFILE {
        Long id PK
        Long user_id FK
        String biography
        String portfolio_url
    }

    ARTWORK {
        Long id PK
        String title
        String description
        Double price
        String image_url
        String category
        Long artist_id FK
    }

    EXHIBITION {
        Long id PK
        String name
        String description
        Timestamp start_date
        Timestamp end_date
        Integer total_tickets
        Integer available_tickets
        Timestamp vip_early_access_start
    }

    TICKET {
        Long id PK
        Long exhibition_id FK
        Double price
        String ticket_type
    }

    BOOKING {
        Long id PK
        Long user_id FK
        Long ticket_id FK
        Integer quantity
        Double total_price
        String status
        Timestamp booking_time
    }