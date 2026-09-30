# Bài lớn Kotlin Collections — Store Analytics

## Bối cảnh

Bạn đang viết phần xử lý dữ liệu cho một ứng dụng bán hàng. Ứng dụng có sản phẩm, khách hàng, đơn hàng và các sự kiện người dùng. Hãy dùng Kotlin Collections để làm sạch dữ liệu, cập nhật tồn kho và tạo báo cáo cho màn hình quản trị.

Toàn bộ câu hỏi dùng chung các model trong `store/model`, dữ liệu trong `store/data/StoreSampleData.kt` và lời giải trong `store/exercise`. Nên làm lần lượt vì kết quả của các câu đầu sẽ được tái sử dụng ở những câu sau.

## Quy tắc

- Không thay đổi collection đầu vào, trừ khi đề yêu cầu thao tác với bản sao mutable.
- Ưu tiên collection function thay cho vòng lặp khi cách viết vẫn dễ đọc.
- Mọi hàm phải xử lý được collection rỗng.
- `page` bắt đầu từ 1; `pageSize` và số lượng cần lấy phải hợp lệ.
- Tiền dùng `Long` để tránh tràn số khi cộng nhiều đơn hàng.

## Phần A — Chuẩn bị dữ liệu

### Câu 1 — Giỏ hàng mutable

Tạo bản sao mutable của giỏ hàng, thêm một sản phẩm, xóa sản phẩm đầu tiên có `productId` được chỉ định, rồi trả về `List` chỉ đọc. Dùng `toMutableList`, `add`, `indexOfFirst`, `removeAt` và `toList`.

### Câu 2 — Truy cập an toàn và lập chỉ mục

1. Lấy sản phẩm theo index; index sai trả về `null`.
2. Tạo ba bảng tra cứu: `id -> Product`, `id -> name`, và `Product -> số tag`.
3. Quan sát sự khác nhau giữa `associateBy`, `associate` và `associateWith`.

### Câu 3 — Set và phép toán tập hợp

1. Lấy tập hợp category duy nhất của catalog.
2. Với hai nhóm sản phẩm, lấy tất cả tag, tag chung và tag chỉ có ở nhóm thứ nhất bằng `union`, `intersect`, `subtract`.

### Câu 4 — Lọc và sắp xếp catalog

1. Lấy sản phẩm còn hàng, sắp xếp theo giá tăng dần rồi theo tên.
2. Từ kết quả đó, lấy các sản phẩm nằm ở vị trí chẵn bằng `filterIndexed`.
3. Chia catalog thành nhóm còn hàng và hết hàng bằng `partition`.

### Câu 5 — Làm sạch dữ liệu nullable và trùng lặp

Từ danh sách khách hàng, chỉ giữ email không null/rỗng; trim email; loại trùng không phân biệt chữ hoa/thường nhưng giữ bản ghi đầu tiên. Dùng `mapNotNull` và `distinctBy`.

## Phần B — Xử lý đơn hàng

### Câu 6 — Làm phẳng và lọc theo kiểu

1. Lấy toàn bộ `OrderItem` từ các đơn hoàn tất bằng `flatMap`.
2. Viết cách tương đương bằng `map` rồi `flatten`.
3. Từ luồng `StoreEvent`, chỉ lấy `productId` của `ProductViewed` bằng `filterIsInstance`.
4. Từ danh sách từ khóa nullable, bỏ null bằng `filterNotNull` và bỏ chuỗi rỗng bằng `filterNot`.

### Câu 7 — Tổng tiền và Map

1. Tính tổng một đơn hàng bằng `sumOf`; mã sản phẩm không tồn tại có giá 0.
2. Tạo `Map<orderId, total>` cho mọi đơn bằng `associate`.
3. Lọc Map để chỉ giữ đơn có tổng tiền từ một ngưỡng cho trước bằng `filterValues`.

### Câu 8 — Nhóm dữ liệu

1. Chỉ lấy đơn hoàn tất.
2. Nhóm theo `customerId` bằng `groupBy`.
3. Dùng `mapValues` để tính tổng tiền mỗi khách hàng.

### Câu 9 — Cập nhật tồn kho

Sao chép tồn kho sang `MutableMap`, trừ số lượng của các đơn hoàn tất, rồi trả về Map chỉ đọc. Không cho tồn kho xuống dưới 0.

### Câu 10 — Kiểm tra dữ liệu

Tạo kết quả kiểm tra gồm:

- `any`: có đơn nào rỗng không;
- `all`: mọi item có số lượng dương và tham chiếu tới sản phẩm tồn tại;
- `none`: không có đơn hoàn tất nào thuộc khách hàng không tồn tại.

Danh sách đơn rỗng không được coi là “mọi đơn đều hợp lệ”.

## Phần C — Phân tích và trình bày

### Câu 11 — Aggregate, fold và reduce

Với các đơn hoàn tất, tính số đơn, doanh thu, trung bình, đơn thấp nhất, đơn cao nhất và tổng số sản phẩm. Dùng `count`, `sum`, `average`, `minOrNull`, `maxOrNull`, `fold` và `reduceOrNull`.

### Câu 12 — Tìm kiếm và phân trang

Tìm sản phẩm theo tên không phân biệt hoa/thường, sắp xếp theo tên, sau đó dùng `drop` và `take` để lấy trang yêu cầu. Trang ngoài phạm vi trả về List rỗng.

### Câu 13 — takeWhile và dropWhile

Sắp xếp sản phẩm còn hàng theo giá. Chia thành nhóm có giá không vượt quá ngân sách và nhóm vượt ngân sách bằng `takeWhile` và `dropWhile`.

### Câu 14 — chunked, windowed và zipWithNext

1. Chia danh sách đơn hoàn tất thành các lô giao hàng bằng `chunked`.
2. Tính doanh thu của mọi cửa sổ gồm `N` đơn liên tiếp bằng `windowed`.
3. Tính mức thay đổi doanh thu giữa hai đơn liên tiếp bằng `zipWithNext`.

### Câu 15 — mapIndexed, joinToString, zip và unzip

1. Tạo chuỗi đánh số các đơn hoàn tất bằng `mapIndexed` và `joinToString`.
2. Ghép tên khách hàng với số tiền đã chi bằng `zip`.
3. Tách kết quả trở lại hai List bằng `unzip`.

### Câu 16 — Sequence

1. Dùng `asSequence` để lấy `N` sản phẩm còn hàng có giá cao nhất.
2. Dùng `generateSequence` tạo kế hoạch nhập hàng, mỗi đợt gấp đôi đợt trước.
3. Chỉ thực thi khi gọi terminal operation `toList`.

### Câu 17 — Dashboard tổng hợp

Tạo một `Dashboard` gồm:

- danh sách sản phẩm còn hàng đã sắp xếp;
- tổng số đơn hoàn tất và tổng doanh thu;
- ba khách hàng chi tiêu cao nhất;
- doanh thu theo category;
- các cảnh báo sản phẩm có tồn kho sau bán nhỏ hơn hoặc bằng 5.

Câu này phải kết hợp `filter`, `flatMap`, `mapNotNull`, `groupBy`, `mapValues`, `associateBy`, `sumOf`, `sortedByDescending` và `take`.

## Cách làm đề

1. Giữ nguyên `store/model`, `store/result`, dữ liệu mẫu và `CollectionsCapstoneRunner.kt`.
2. Xóa thân các hàm mang nhãn `Câu 01` đến `Câu 17` trong `store/exercise`, rồi tự cài đặt lại.
3. Chạy `main` của `CollectionsCapstoneRunner`; khi không có lỗi từ `check(...)`, toàn bộ ví dụ đã đạt.
4. Có thể làm lần lượt theo từng nhóm: catalog, order, analytics và dashboard.
5. Cuối cùng, thêm test cho đầu vào rỗng, dữ liệu trùng, id không tồn tại và trang ngoài phạm vi.

