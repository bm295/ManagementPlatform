Feature: Xử lý đơn bán hàng theo quy trình Order-to-Cash
  In order to vận hành hoạt động bán hàng nhất quán giữa các phòng ban
  As a nhân viên kinh doanh
  I want to xác nhận đơn bán hàng trong hệ thống ERP
  So that giữ hàng cho khách, ghi nhận công nợ và chuyển đơn sang bước thực hiện

  Scenario: Xác nhận đơn bán hàng khi khách hàng còn hạn mức tín dụng và kho đủ hàng
    Given khách hàng "Công ty An Phát" đang hoạt động
    And khách hàng còn hạn mức tín dụng 100.000.000 VND
    And kho "Hồ Chí Minh" còn 10 sản phẩm "ERP-PRO"
    And có đơn bán hàng nháp "SO-2026-0001" gồm 2 sản phẩm "ERP-PRO" với tổng giá trị 20.000.000 VND
    When nhân viên kinh doanh xác nhận đơn bán hàng "SO-2026-0001"
    Then trạng thái đơn bán hàng "SO-2026-0001" phải là "Đã xác nhận"
    And 2 sản phẩm "ERP-PRO" phải được giữ tại kho "Hồ Chí Minh"
    And công nợ phải thu dự kiến của khách hàng phải tăng thêm 20.000.000 VND
    And một yêu cầu xuất kho phải được tạo cho đơn bán hàng "SO-2026-0001"
