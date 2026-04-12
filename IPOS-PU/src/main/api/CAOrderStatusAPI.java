package main.api;

import java.time.LocalDateTime;

public interface CAOrderStatusAPI {

    String getOrderStatus(String orderId);

    LocalDateTime listUpdatedOrders(LocalDateTime since);
}