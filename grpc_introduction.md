# Tổng quan về gRPC, Kiến trúc hệ thống và Cơ chế Protobuf / HTTP/2

## 1. Bảng tổng hợp kiến thức Anh – Việt (gRPC & Kiến trúc hệ thống)

| Chủ đề (Topic) | Nội dung Tiếng Anh (Chuẩn hóa) | Tiếng Việt & Diễn giải chi tiết |
| :--- | :--- | :--- |
| **gRPC Definition**<br>*(Định nghĩa gRPC)* | **gRPC (gRPC Remote Procedure Call)** is a high-performance, open-source RPC framework created by Google. It enables different applications to communicate efficiently across services, even when built with different programming languages. | **gRPC** là một framework giao tiếp từ xa hiệu năng cao do Google phát triển. Nó cho phép các ứng dụng gọi hàm trực tiếp từ một service khác như thể đang gọi hàm cục bộ (local), ngay cả khi hai service viết bằng hai ngôn ngữ khác nhau (ví dụ: Python gọi sang Go hoặc Java). |
| **Monolith Architecture**<br>*(Kiến trúc nguyên khối)* | • **Tight coupling:** Components are heavily dependent on each other.<br>• **Failure impact:** A bug in one module can crash the entire application.<br>• **Deployment bottleneck:** Even a small change requires rebuilding and redeploying the whole system. | • **Liên kết chặt chẽ:** Các module nằm chung một codebase, phụ thuộc lẫn nhau nên rất khó tách rời để nâng cấp.<br>• **Ảnh hưởng diện rộng khi lỗi:** Một lỗi nhỏ (ví dụ tràn bộ nhớ ở tính năng phụ) có thể làm sập toàn bộ hệ thống.<br>• **Nút thắt triển khai:** Chỉ cần sửa 1 dòng code cũng phải build và deploy lại toàn bộ ứng dụng, tốn thời gian và dễ gây rủi ro. |
| **Microservices: REST**<br>*(Vi dịch vụ dùng REST)* | • **High network latency (HTTP/1.1):** Opens multiple connections or waits sequentially.<br>• **Data processing overhead:** Uses text-based JSON, which is bulky and slow to parse.<br>• **Request-Response Model:** Strictly synchronous one-to-one interaction. | • **Độ trễ mạng cao:** HTTP/1.1 xử lý tuần tự từng request trên một kết nối, dễ bị nghẽn cổ chai.<br>• **Chi phí xử lý dữ liệu lớn:** JSON dạng văn bản (text) lặp lại tên các trường (key) liên tục làm gói tin nặng, tốn CPU để chuyển đổi (parse) sang object.<br>• **Mô hình Hỏi – Đáp cứng nhắc:** Client gửi 1 request và phải chờ Server trả về 1 response mới xong lượt. |
| **Microservices: gRPC**<br>*(Vi dịch vụ dùng gRPC)* | • **Reduced network latency (HTTP/2):** Multiplexes multiple requests over a single TCP connection.<br>• **Protobuf:** Uses compact binary format for fast serialization and low bandwidth usage. | • **Giảm tối đa độ trễ mạng:** Nhờ HTTP/2 cho phép gửi nhận nhiều luồng dữ liệu song song trên duy nhất 1 kết nối TCP.<br>• **Tối ưu băng thông & tốc độ:** Dữ liệu được nén thành dạng nhị phân (binary) siêu nhỏ gọn thông qua Protobuf, giúp truyền tải cực nhanh giữa các microservices nội bộ. |
| **Protobuf**<br>*(Protocol Buffers)* | A language-neutral, platform-neutral mechanism for **serializing** (object $\rightarrow$ binary) and **deserializing** (binary $\rightarrow$ object) structured data. | Cơ chế trung lập ngôn ngữ dùng để **tuần tự hóa** (biến dữ liệu/object trong code thành chuỗi byte nhị phân để truyền qua mạng) và **giải tuần tự hóa** (biến chuỗi byte nhận được ngược lại thành object). |
| **HTTP/2** | The underlying transport protocol of gRPC that supports binary framing, header compression, and full-duplex multiplexing. | Giao thức truyền tải tầng mạng của gRPC, cho phép đóng khung dữ liệu nhị phân, nén phần tiêu đề (header) và truyền nhận hai chiều đồng thời. |
| **1. Unary RPC**<br>*(Giao tiếp đơn)* | **One request – One response:** The client sends a single request and receives a single response, similar to a standard function call. | **1 Yêu cầu – 1 Phản hồi:** Giống như gọi hàm thông thường hoặc REST. Client gửi một gói tin yêu cầu, Server xử lý và trả về một gói tin kết quả duy nhất.<br>*Ví dụ:* Lấy thông tin chi tiết của 1 user theo ID. |
| **2. Server Streaming**<br>*(Server phát luồng)* | **One request – Multiple responses:** The client sends one request, and the server keeps sending back a stream of messages until finished. | **1 Yêu cầu – Nhiều Phản hồi:** Client gửi yêu cầu một lần, sau đó Server mở luồng và liên tục đẩy nhiều gói dữ liệu về cho đến khi hết dữ liệu.<br>*Ví dụ:* Cập nhật bảng giá chứng khoán trực tiếp, tải danh sách hàng nghìn bản ghi theo từng đợt. |
| **3. Client Streaming**<br>*(Client phát luồng)* | **Multiple requests – One response:** The client continuously sends a stream of messages to the server, and the server returns a single response at the end. | **Nhiều Yêu cầu – 1 Phản hồi:** Client liên tục đẩy nhiều gói dữ liệu lên Server; sau khi Client báo đã gửi xong, Server mới tổng hợp và trả về 1 kết quả duy nhất.<br>*Ví dụ:* Upload file dung lượng lớn theo từng mảnh (chunk), thiết bị IoT gửi dữ liệu cảm biến liên tục để tính trung bình. |
| **4. Bidirectional Streaming**<br>*(Phát luồng hai chiều)* | **Multiple requests – Multiple responses:** Both client and server send independent streams of messages simultaneously over a single connection. | **Nhiều Yêu cầu – Nhiều Phản hồi:** Cả Client và Server đều có thể chủ động gửi và nhận dữ liệu liên tục, độc lập và đồng thời với nhau mà không cần chờ bên kia gửi xong.<br>*Ví dụ:* Ứng dụng chat thời gian thực, điều khiển game online, hệ thống định vị GPS tài xế. |

---

## 2. Giải thích chuyên sâu cơ chế cốt lõi: Protobuf & HTTP/2

Sức mạnh tốc độ của gRPC (nhanh hơn REST/JSON từ 5 đến 10 lần) đến từ sự kết hợp giữa **Protobuf ở tầng dữ liệu** và **HTTP/2 ở tầng truyền tải**.

### A. Cơ chế của Protocol Buffers (Protobuf)
Thay vì truyền dữ liệu dưới dạng văn bản thuần túy như JSON hay XML, Protobuf hoạt động dựa trên **hợp đồng dữ liệu nghiêm ngặt (`.proto` file)** và **mã hóa nhị phân (Binary Encoding)**.

1. **Định nghĩa cấu trúc bằng file `.proto` (IDL - Interface Definition Language):**
   Cả Client và Server đều dùng chung một file thiết kế `.proto`. Trong đó, mỗi trường dữ liệu được gán một **con số định danh duy nhất (Field Tag)** thay vì dùng tên chữ.
   ```protobuf
   message User {
     int32 id = 1;       // Số 1 đại diện cho trường "id"
     string name = 2;    // Số 2 đại diện cho trường "name"
   }
   ```
2. **Cách Serialize (Đóng gói) siêu gọn nhẹ:**
   * **Với JSON:** Chuỗi `{"id": 105, "name": "An"}` tốn **25 bytes** vì phải gửi kèm cả tên biến `"id"`, `"name"`, dấu ngoặc kép, dấu hai chấm.
   * **Với Protobuf:** Nó loại bỏ hoàn toàn tên biến, chỉ ghép **Số thứ tự trường + Kiểu dữ liệu + Giá trị** thành chuỗi nhị phân (hex/byte). Dữ liệu trên chỉ còn khoảng **6 bytes** (nhỏ hơn ~4 lần).
3. **Cách Deserialize (Giải mã) cực nhanh:**
   Khi bên nhận đọc chuỗi byte, nhờ đã có sẵn file `.proto` làm "từ điển giải mã", máy tính biết ngay: *"Byte đầu tiên mang tag số 1 $\rightarrow$ gán vào biến `id`; byte tiếp theo mang tag số 2 $\rightarrow$ gán vào biến `name`"*. Quá trình này đọc thẳng vào bộ nhớ mà không tốn CPU phân tích cú pháp chuỗi (string parsing) như JSON.
4. **Sinh code tự động đa ngôn ngữ:** Từ một file `.proto`, công cụ `protoc` tự động sinh ra các class/struct và hàm gọi mạng cho hàng chục ngôn ngữ khác nhau, đảm bảo tính nhất quán tuyệt đối.

---

### B. Cơ chế của HTTP/2
REST truyền thống thường chạy trên HTTP/1.1, vốn gặp vấn đề **Head-of-Line (HOL) Blocking** (request trước chưa xong thì request sau phải xếp hàng chờ, hoặc trình duyệt phải mở nhiều kết nối TCP gây tốn tài nguyên). gRPC tận dụng triệt để 3 cơ chế của HTTP/2:

1. **Single TCP Connection & Multiplexing (Đa luồng trên 1 kết nối duy nhất):**
   * Client và Server chỉ cần thiết lập **1 kết nối TCP duy nhất** và giữ kết nối đó liên tục (persistent connection).
   * Trên đường ống TCP đó, HTTP/2 chia nhỏ dữ liệu thành nhiều **Stream (luồng)** độc lập. Hàng trăm request và response có thể chạy đan xen, song song cùng lúc mà không chặn đường nhau.
2. **Binary Framing Layer (Tầng đóng khung nhị phân):**
   * Mọi thông điệp Protobuf sau khi nén nhị phân sẽ được HTTP/2 cắt nhỏ thành các khung (Frame) tiêu chuẩn như `HEADERS Frame` (chứa metadata, tên hàm cần gọi) và `DATA Frame` (chứa payload Protobuf). Việc xử lý các khung nhị phân này giúp máy chủ định tuyến và xử lý cực kỳ hiệu quả.
3. **Header Compression – HPACK (Nén tiêu đề):**
   * Trong các cuộc gọi API lặp đi lặp lại, phần Header (như `Authorization`, `Content-Type`, `User-Agent`) thường giống hệt nhau. HTTP/2 dùng thuật toán **HPACK** để lưu bảng chỉ mục (index table) các header đã gửi; ở các lần gọi sau, nó chỉ gửi mã số tham chiếu thay vì gửi lại toàn bộ chuỗi header dài dòng.

---

### C. Luồng hoạt động kết hợp (End-to-End Flow)

Khi **Service A (Client - viết bằng Python)** gọi hàm `GetUser(id=105)` từ **Service B (Server - viết bằng Go)**:

1. **Gọi cục bộ (Stub Call):** Code Python gọi hàm `stub.GetUser(id=105)`.
2. **Protobuf Serialization:** Thư viện Protobuf nén dữ liệu `id=105` thành chuỗi byte nhị phân nhỏ gọn.
3. **HTTP/2 Framing:** Chuỗi byte này được bọc vào HTTP/2 `DATA Frame`, đi kèm `HEADERS Frame` chứa đường dẫn hàm `/UserService/GetUser` (đã nén bằng HPACK).
4. **Multiplexed Transport:** Các frame được đẩy qua kết nối TCP đang mở sẵn tới Service B (chạy song song với hàng loạt request khác).
5. **Server Processing:** Service B nhận các frame, ghép lại, dùng Protobuf giải mã (Deserialize) chuỗi byte thành struct `UserRequest` trong Go, thực thi logic truy vấn database.
6. **Phản hồi ngược lại:** Kết quả `UserResponse` lại được Protobuf nén thành nhị phân $\rightarrow$ đóng khung HTTP/2 $\rightarrow$ trả về cho Service A trên chính luồng (Stream ID) ban đầu.