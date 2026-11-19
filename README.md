# gRPC với Spring Boot: từ `.proto` đến bốn kiểu gọi RPC

Dự án này là bài thực hành gRPC bằng Java 25 và Spring Boot. Có ba module Maven độc lập: `protobuff` để làm quen với Protocol Buffers, `stock-trading-server` để cung cấp dịch vụ giao dịch mẫu, và `stock-trading-client` để gọi dịch vụ. README đi theo năm phần: **(1) tạo file proto, (2) unary, (3) server streaming, (4) client streaming, (5) bidirectional streaming**. Các ví dụ được giải thích theo **mã hiện có trong repo**, đồng thời chỉ ra cách phát triển thành ứng dụng thực tế.

## Bắt đầu: gRPC hoạt động ra sao?

gRPC là cơ chế gọi thủ tục từ xa: ở client, ta gọi một phương thức trên **stub**; lời gọi được mã hóa, gửi tới server, thực thi ở phương thức tương ứng rồi chuyển kết quả về. Dự án dùng Protocol Buffers (Protobuf) làm định dạng message và HTTP/2 làm giao thức vận chuyển. File `.proto` là hợp đồng chung quy định tên service, tên RPC và cấu trúc message. Code Java sinh từ hợp đồng giúp hai bên dùng đúng kiểu dữ liệu. `onNext` gửi **một message**, `onCompleted` báo **kết thúc luồng**, `onError` báo **lỗi**. Với streaming, các message trong cùng một luồng được giữ thứ tự; hai chiều gửi và nhận có thể tiến hành độc lập.

```text
Client Java                   HTTP/2 + Protobuf                    Server Java
StockClientService   ->  StockRequest / StockOrder   ->  StockTradingServiceImpl
  stub.method(...)   <-  StockResponse / ...         <-  responseObserver.onNext(...)
```

**Cấu trúc repo**

| Đường dẫn | Vai trò |
| --- | --- |
| `protobuff/src/main/proto/employee.proto` | Message `Employee`, `Department` và service `EmployeeService` để học cú pháp proto; module này **chưa** triển khai server/client cho service đó. |
| `stock-trading-server/src/main/proto/stock_trading.proto` | Hợp đồng bốn RPC mà server triển khai. |
| `stock-trading-server/src/main/java/com/doph/service/StockTradingServiceImpl.java` | Logic server, kế thừa base class sinh tự động. |
| `stock-trading-server/src/main/java/com/doph/entity/Stock.java` và `repository/StockRepository.java` | Entity JPA và truy vấn MySQL cho unary. |
| `stock-trading-client/src/main/proto/stock_trading.proto` | Bản sao hợp đồng để client tự sinh code; cần giữ đồng bộ với server. |
| `stock-trading-client/src/main/java/com/doph/service/StockClientService.java` | Blocking stub, async stub và các lời gọi ví dụ. |
| `stock-trading-client/src/main/java/com/doph/StockTradingClientApplication.java` | `CommandLineRunner` chọn ví dụ chạy khi khởi động. |
| `docker-compose.yml`, `sql/init.sql` | MySQL và dữ liệu chứng khoán mẫu. |

### Chuẩn bị và chạy thử

- JDK **25**, Docker/Compose và kết nối để Maven tải dependency lần đầu. Mỗi thư mục module có Maven Wrapper riêng (`mvnw.cmd` trên Windows, `./mvnw` trên Unix); nếu wrapper không chạy trong môi trường của bạn, dùng lệnh `mvn` của Maven đã cài. Các module dùng Spring Boot `4.1.2` theo `pom.xml`.
- Docker Compose mở MySQL tại `localhost:3306`, database `stock_trading`, tài khoản `root/password`. Đây là thông tin **demo**; cấu hình server nằm trong `stock-trading-server/src/main/resources/application.properties`.
- Server gRPC lắng nghe `localhost:9090` theo `stock-trading-server/src/main/resources/application.yml`. Client dùng tên kết nối `stockService` tại địa chỉ này với `PLAINTEXT` trong `stock-trading-client/src/main/resources/application.yml`. Hai cấu hình `userService` và `orderService` trong file client chưa được dùng bởi mã hiện tại.

Mở ba terminal ở **thư mục gốc repo**:

```powershell
# Terminal 1: khởi động MySQL (chờ healthcheck báo healthy)
docker compose up -d
docker compose ps
```

```powershell
# Terminal 2: chạy server
cd stock-trading-server
mvn spring-boot:run
```

```powershell
# Terminal 3: chạy client; hiện mặc định gọi liveTrading
cd stock-trading-client
mvn spring-boot:run
```

Nếu dùng Maven Wrapper, thay `mvn` bằng `.\mvnw.cmd` trên Windows hoặc `./mvnw` trên macOS/Linux. Muốn chạy một ví dụ khác, mở `StockTradingClientApplication.run(...)`, **bỏ comment đúng một lời gọi** trong bốn lời gọi `getStockPrice("VCB")`, `subscribeStockPrice("VCB")`, `placeBulkOrders()`, `startLiveTrading()` và comment các lời gọi còn lại, rồi chạy lại client. Client là chương trình chạy một lượt khi khởi động, **không có REST endpoint hay giao diện web**. Các ví dụ bất đồng bộ trả quyền điều khiển ngay sau khi mở luồng; nếu JVM dừng sớm, cần giữ tiến trình sống/chờ callback kết thúc trong ứng dụng thực tế.

**Lưu ý dữ liệu:** `sql/init.sql` tạo bảng `stocks` và chèn các mã AAPL, GOOGL, MSFT, NVDA, TSLA, FPT, VCB khi volume MySQL được khởi tạo **lần đầu**. `spring.jpa.hibernate.ddl-auto=update` cũng có thể cập nhật schema từ entity. Nếu đã có volume cũ, sửa `init.sql` rồi `docker compose up` sẽ không tự chạy lại script; hãy cập nhật DB bằng SQL hoặc chủ động tạo volume mới nếu dữ liệu cũ không cần giữ.

## Phần 1 — Tạo file `.proto` và hiểu code được sinh ra

### 1.1. Định nghĩa hợp đồng

Đặt file proto dưới `src/main/proto/`. Module `protobuff` dùng `employee.proto` để minh họa cú pháp:

```proto
syntax = "proto3";
option java_multiple_files = true;
option java_package = "com.doph.protobuff";
option java_outer_classname = "EmployeeProto";

import "google/protobuf/timestamp.proto";

message Employee {
  int32 id = 1;
  string name = 2;
  double salary = 3;
  repeated Department departments = 4;
  map<string, string> addressMap = 5;
  bool isActive = 6;
  bytes profilePicture = 7;
  google.protobuf.Timestamp joinDate = 8;
}
```

`syntax = "proto3"` chọn cú pháp proto3. `message` khai báo kiểu dữ liệu; số `= 1`, `= 2` là **field number** dùng trên wire, phải duy nhất trong một message và nên giữ ổn định khi nâng phiên bản. `repeated` là danh sách, `map` là bảng khóa/giá trị, `bytes` là dữ liệu nhị phân, `Timestamp` là message chuẩn được import. `package` trong proto đặt namespace của hợp đồng gRPC; `java_package` đặt package Java được sinh. Với `java_multiple_files = true`, mỗi message có class Java riêng; `EmployeeProto` giữ thông tin descriptor của file. Trong proto3, các trường scalar không được gán có giá trị mặc định (ví dụ `int32` là `0`, `string` là `""`); cần `optional` hoặc kiểu phù hợp nếu nghiệp vụ phải phân biệt “chưa gửi” với giá trị mặc định.

`employee.proto` còn có `Department`, `EmployeeRequest`, `EmployeeList`, `Empty` và ba RPC `getEmployee`, `addEmployee`, `getAllEmployees`. **Chúng mới là khai báo**, chưa có lớp `EmployeeServiceImpl` hay client gọi các RPC đó. Module giao dịch mới là ví dụ đầy đủ client/server.

Trong `stock_trading.proto`, bốn dòng sau định nghĩa **cùng một service với bốn dạng luồng**:

```proto
service StockTradingService {
  rpc getStockPrice(StockRequest) returns (StockResponse);
  rpc subscribeStockPrice(StockRequest) returns (stream StockResponse);
  rpc bulkStockOrder(stream StockOrder) returns (OrderSummary);
  rpc liveTrading(stream StockOrder) returns (stream TradeStatus);
}
```

Từ khóa `stream` trước kiểu request nghĩa là client gửi nhiều message; trước kiểu response nghĩa là server gửi nhiều message. Không có `stream` nghĩa là đúng một message ở chiều đó. Hai file `stock_trading.proto` ở server và client hiện **giống nhau**. Khi thay hợp đồng, cần cập nhật và build cả hai bên; về lâu dài nên tách proto thành module/package dùng chung để tránh lệch phiên bản.

### 1.2. Maven sinh những gì?

Các `pom.xml` dùng `io.github.ascopes:protobuf-maven-plugin`. Chạy trong từng module:

```powershell
mvn compile
```

Sau bước sinh code, xem `target/generated-sources/protobuf/`:

```text
com/doph/grpc/
  StockRequest.java, StockResponse.java, StockOrder.java,
  OrderSummary.java, TradeStatus.java
  *OrBuilder.java
  StockTradingProto.java
  StockTradingServiceGrpc.java
```

Ở module `protobuff` sẽ có `Employee.java`, `Department.java`, `EmployeeProto.java`, `EmployeeServiceGrpc.java` và các message/helper tương ứng. `*OrBuilder` là interface phục vụ API của message/builder; `*Proto` chứa descriptor. Các class message có `newBuilder()`, setter trên builder, `build()` và getter trên object đã xây. Ví dụ:

```java
StockRequest request = StockRequest.newBuilder()
    .setStockSymbol("VCB")
    .build();
```

`StockTradingServiceGrpc` sinh ra `StockTradingServiceImplBase` cho server, `StockTradingServiceBlockingStub` và `StockTradingServiceStub` cho client (cùng các API hỗ trợ khác). **Không sửa file dưới `target/`**: lần build sau sẽ sinh lại. Ta **kế thừa** `ImplBase`, **override** bốn RPC và gọi phương thức trên stub; message Protobuf được **xây bằng builder**, không kế thừa để thêm nghiệp vụ. `@GrpcService` đánh dấu implementation để Spring đăng ký lên gRPC server. Client dùng `@GrpcClient("stockService")` để tiêm stub theo cấu hình kết nối; annotation này đến từ `net.devh:grpc-client-spring-boot-starter`, trong khi server dùng Spring gRPC starter.

### 1.3. Một lời gọi đi qua mạng

1. Client xây message Java theo `.proto`, gọi stub. Stub và gRPC runtime serialize message thành byte Protobuf, gửi trên HTTP/2 tới phương thức `/stocktrading.StockTradingService/<tên RPC>`.
2. Server nhận byte, giải mã thành message Java và gọi override trong `StockTradingServiceImpl`.
3. Server xử lý nghiệp vụ, xây response, gọi `responseObserver.onNext(...)`; runtime mã hóa và gửi về client. Server gọi `onCompleted()` để đóng thành công hoặc `onError(...)` để trả lỗi.
4. Client giải mã response. Blocking stub trả object khi unary xong; async stub gọi callback `onNext`/`onError`/`onCompleted` trên observer của client.

Protobuf dùng field number/kiểu wire để mã hóa; HTTP/2 chở các frame và cho phép nhiều luồng RPC dùng chung kết nối. Giá trị `System.out.println` trong project chỉ **hiển thị message đã được giải mã**, không phải định dạng dữ liệu thực sự truyền trên mạng. Không có bằng chứng benchmark trong repo để khẳng định gRPC luôn nhanh hơn REST một tỷ lệ cố định.

## Phần 2 — Unary: một request, một response

**Hợp đồng:** `rpc getStockPrice(StockRequest) returns (StockResponse);`

| Chiều | Dữ liệu |
| --- | --- |
| Client → server | `StockRequest.stock_symbol`, ví dụ `"VCB"`. |
| Server → client | `StockResponse`: `stock_symbol`, `price`, `timestamp`. |

**Server:** `StockTradingServiceImpl` override `getStockPrice(StockRequest request, StreamObserver<StockResponse> responseObserver)`. Phương thức lấy `request.getStockSymbol()`, gọi `stockRepository.findByStockSymbol(...)`, lấy giá và `lastUpdated` từ bảng `stocks`, tạo `StockResponse` bằng builder, rồi gọi `responseObserver.onNext(response)` **một lần** và `responseObserver.onCompleted()`. Đó là điểm kết thúc RPC thành công.

**Client:** `StockClientService.getStockPrice(String)` tạo `StockRequest`, sau đó gọi `serviceBlockingStub.getStockPrice(request)`. Lời gọi chặn thread hiện tại đến khi có response hoặc lỗi. Trong `StockTradingClientApplication.run`, bỏ comment đoạn `System.out.println(...getStockPrice("VCB"))` để xem kết quả. `VCB` có trong dữ liệu seed; giá trả về là giá trong DB tại thời điểm truy vấn.

```text
Client                          Server                         MySQL
getStockPrice("VCB") ────────> getStockPrice(request) ──────> SELECT stocks WHERE stock_name='VCB'
StockResponse       <───────── onNext(response)       <────── Stock(price, lastUpdated)
RPC kết thúc        <───────── onCompleted()
```

**Giới hạn hiện tại:** mã dùng `orElse(null)` rồi truy cập `stockEntity.getPrice()`, nên mã không tồn tại có thể gây `NullPointerException` thay vì lỗi gRPC có chủ đích. Cũng cần xử lý `lastUpdated` null. Trong dịch vụ thực, hãy trả `Status.NOT_FOUND` hoặc lỗi nghiệp vụ phù hợp, xác thực quyền truy cập, đặt deadline ở client và ghi log/metrics. Unary phù hợp tra cứu giá hiện tại, số dư hoặc chi tiết một đơn hàng khi mỗi yêu cầu chỉ cần một kết quả.

## Phần 3 — Server streaming: một request, nhiều response

**Hợp đồng:** `rpc subscribeStockPrice(StockRequest) returns (stream StockResponse);`

**Server:** override `subscribeStockPrice(request, responseObserver)` lấy mã cổ phiếu rồi lặp `i = 0` đến `10` (**11 message**). Mỗi vòng tạo `StockResponse` với giá ngẫu nhiên trong khoảng `[0, 200)`, thời gian `Instant.now()`, gọi `onNext`, rồi ngủ một giây. Cuối vòng gọi `onCompleted`. Trong mã có truy vấn `stockRepository.findByStockSymbol(...)`, nhưng kết quả **không được dùng** để tạo giá và không có ghi DB. Đây là dữ liệu giả để quan sát luồng, không phải feed giá thật; ngay cả khi mã không có trong DB, phương thức hiện vẫn có thể phát giá giả.

**Client:** `StockClientService.subscribeStockPrice("VCB")` tạo một `StockRequest`, gọi `asyncStub.subscribeStockPrice(request, responseObserver)`. Callback client nhận từng `StockResponse` qua `onNext` và in giá; `onCompleted` in `Stream completed`; `onError` in lỗi. Bỏ comment `stockClientService.subscribeStockPrice("VCB")` trong `run` để thử. Lời gọi async không chờ đủ 11 phản hồi trước khi phương thức Java trả về.

```text
Client                                        Server
StockRequest("VCB") ───────────────────────> subscribeStockPrice(...)
onNext(response 1) <──────────────────────── onNext(giá giả 1)
onNext(response 2) <──────────────────────── onNext(giá giả 2)
...                                           ...
onNext(response 11) <─────────────────────── onNext(giá giả 11)
onCompleted() <───────────────────────────── onCompleted()
```

**Ứng dụng thực:** bảng giá realtime, trạng thái xử lý công việc dài, thông báo tồn kho. Nguồn giá thường là message broker hoặc hệ thống market data; server giữ subscription, phát **khi có dữ liệu mới**, xử lý client hủy kết nối, deadline, backpressure và cleanup. Vòng `sleep` đồng bộ trong demo chiếm thread xử lý; không nên dùng nguyên mẫu đó cho nhiều người đăng ký.

## Phần 4 — Client streaming: nhiều request, một response

**Hợp đồng:** `rpc bulkStockOrder(stream StockOrder) returns (OrderSummary);`

**Client:** `placeBulkOrders()` tạo `StreamObserver<OrderSummary>` để chờ kết quả, gọi `asyncStub.bulkStockOrder(responseObserver)` để lấy `StreamObserver<StockOrder>` dành cho **chiều gửi**. Client gửi năm `StockOrder` mẫu (`ORD-001` đến `ORD-005`) bằng `requestObserver.onNext(order)`, cách nhau 500 ms, rồi gọi `requestObserver.onCompleted()` để báo hết dữ liệu. Chỉ sau tín hiệu này server mới tổng hợp và trả `OrderSummary`. Bỏ comment `stockClientService.placeBulkOrders()` trong `run` để thử.

**Server:** override `bulkStockOrder(responseObserver)` trả về một `StreamObserver<StockOrder>` mới cho mỗi RPC. `onNext` tăng `totalOrders`; nếu `quantity > 0` và `price > 0`, cộng `quantity * price` vào `totalAmount` và tăng `successCount`; nếu không thì chỉ in “Invalid order skipped”. `onCompleted` tạo **một** `OrderSummary`, gửi qua `responseObserver.onNext(summary)` và đóng bằng `responseObserver.onCompleted()`. `onError` của request observer hiện chỉ ghi lỗi client/stream. Với năm order mẫu, kết quả dự kiến là `total_orders = 5`, `success_count = 5`, `total_amount = 6205.65` (sai số hiển thị nhỏ có thể xảy ra vì `double`). Tổng này cộng cả BUY và SELL, **không** trừ SELL và **không** có tác động đến DB.

```text
Client                                      Server
onNext(ORD-001) ──────────────────────────> onNext: cộng dồn
onNext(ORD-002) ──────────────────────────> onNext: cộng dồn
... ORD-003, ORD-004, ORD-005 ...
onCompleted() ────────────────────────────> onCompleted: tạo summary
onNext(OrderSummary) <───────────────────── onNext(summary)
onCompleted()      <─────────────────────── onCompleted()
```

**Ứng dụng thực:** nhập nhiều lệnh, tải dữ liệu theo lô, gửi sự kiện cảm biến rồi nhận báo cáo tổng hợp. Server cần validate từng mục, xác thực, kiểm tra rủi ro/số dư, chống xử lý lặp (`order_id`), lưu giao dịch và trả kết quả chi tiết nếu có mục thất bại. Tiền nên dùng đơn vị nhỏ nhất dạng số nguyên hoặc decimal phù hợp, không dùng `double` cho giá trị thanh toán. Đối với batch rất lớn, cần giới hạn kích thước, kiểm soát bộ nhớ và lỗi giữa chừng.

## Phần 5 — Bidirectional streaming: hai chiều cùng phát luồng

**Hợp đồng:** `rpc liveTrading(stream StockOrder) returns (stream TradeStatus);` (“dicrectional” trong yêu cầu được hiểu là **bidirectional streaming**, luồng hai chiều).

**Client:** `startLiveTrading()` mở RPC bằng `asyncStub.liveTrading(responseObserver)`; kết quả trả về là `requestObserver` để gửi order. Client gửi sáu order mẫu (`ORD-101` đến `ORD-106`), cách nhau 800 ms, trong khi observer phía client có thể nhận `TradeStatus` **ngay sau từng order**. Sau khi gửi đủ, client gọi `requestObserver.onCompleted()`. Đây là ví dụ **đang bật mặc định** trong `StockTradingClientApplication.run`.

**Server:** override `liveTrading(responseObserver)` trả về observer nhận order. Mỗi `onNext(order)` in order, tạo một `TradeStatus` với trạng thái `EXECUTED`; riêng `quantity <= 0` thì `FAILED` và message `Invalid quantity`. Server gửi `responseObserver.onNext(tradeStatus)` ngay trong lần xử lý đó. Khi client đóng chiều gửi, `onCompleted()` phía server đóng chiều trả. Trong bộ dữ liệu mẫu, `ORD-104` có quantity `0` nên nhận `FAILED`; năm order còn lại nhận `EXECUTED` theo **logic demo**, kể cả `ORD-106` (FPT). Không có thao tác ghi DB, thanh toán hay khớp lệnh thật. `TradeStatus.timestamp` dùng `LocalDateTime` không kèm múi giờ; các phần khác dùng định dạng thời gian khác nhau, nên hệ thống thật cần chuẩn hóa thời gian.

```text
Client                                      Server
onNext(ORD-101) ──────────────────────────> onNext(order): tạo EXECUTED
onNext(status ORD-101) <──────────────────── onNext(status)
onNext(ORD-104, quantity=0) ──────────────> onNext(order): tạo FAILED
onNext(status ORD-104) <──────────────────── onNext(status)
... hai bên tiếp tục gửi/nhận trên cùng RPC ...
requestObserver.onCompleted() ─────────────> requestObserver.onCompleted()
responseObserver.onCompleted() <──────────── responseObserver.onCompleted()
```

Ví dụ hiện tại trả một trạng thái cho mỗi order, nhưng **giao thức không bắt buộc tỷ lệ 1:1**: server có thể gửi `PENDING`, rồi `EXECUTED`/`FAILED` sau khi bộ máy xử lý xong; đồng thời client vẫn gửi order khác. `order_id` giúp ghép trạng thái với order vì phản hồi ở hệ thống thật có thể đến lệch thời điểm. Ứng dụng thực gồm giao dịch thời gian thực, chat, điều phối thiết bị, telemetry hai chiều. Khi triển khai thật cần xử lý xác thực, thứ tự và trạng thái lệnh, hủy stream, tái kết nối, idempotency, backpressure và lưu kết quả bền vững. `EXECUTED` trong demo chỉ là chuỗi phản hồi, **không có nghĩa đã khớp lệnh tại sàn**.

## So sánh nhanh và hướng mở rộng

| Kiểu RPC | Request | Response | Stub dùng trong repo | Ví dụ trong repo | Tình huống thực |
| --- | --- | --- | --- | --- | --- |
| Unary | 1 | 1 | Blocking | `getStockPrice` | Tra cứu thông tin tại một thời điểm. |
| Server streaming | 1 | N | Async | `subscribeStockPrice` | Theo dõi thay đổi liên tục. |
| Client streaming | N | 1 | Async | `bulkStockOrder` | Gửi batch và nhận báo cáo cuối. |
| Bidirectional streaming | N | N | Async | `liveTrading` | Trao đổi hai chiều theo thời gian thực. |

Để biến bài demo thành dịch vụ thật: (1) quản lý `.proto` ở một nơi và có chiến lược versioning; thêm field mới bằng **số mới**, không đổi ý nghĩa/sử dụng lại số cũ, (2) thay dữ liệu giả và `println` bằng nguồn dữ liệu, persistence, logging/metrics, (3) trả lỗi gRPC có chủ đích cho mã không tồn tại và dữ liệu không hợp lệ, (4) dùng TLS, xác thực và quyền truy cập thay cho `PLAINTEXT`, (5) đặt deadline, xử lý hủy luồng và điều tiết tốc độ, (6) dùng kiểu tiền tệ phù hợp, trạng thái đơn hàng đáng tin cậy và idempotency. Những mục này là **hướng mở rộng**, chưa có trong mã nguồn hiện tại.

Các tài liệu bổ sung của repo: [`grpc_introduction.md`](grpc_introduction.md) về khái niệm gRPC/HTTP/2 và [`proto_json.md`](proto_json.md) về Protobuf so với JSON. README này mô tả hành vi chính xác của code mẫu; các con số hiệu năng tổng quát trong tài liệu khác cần được đo trên workload thực trước khi áp dụng.
