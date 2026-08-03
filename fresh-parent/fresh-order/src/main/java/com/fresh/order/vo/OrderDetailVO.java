package com.fresh.order.vo;

import com.fresh.order.entity.OrderItem;
import com.fresh.order.entity.OrderMain;
import lombok.Data;

import java.util.List;

@Data
public class OrderDetailVO {

    private OrderMain order;
    private List<OrderItem> items;
}
