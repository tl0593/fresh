package com.fresh.common.dto.mq;

import com.fresh.common.dto.MqBaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderEventMqDTO extends MqBaseDTO {

    private String orderNo;
    private Long orderId;
    private Long userId;
    private BigDecimal payAmount;
    private Integer eventType;
}
