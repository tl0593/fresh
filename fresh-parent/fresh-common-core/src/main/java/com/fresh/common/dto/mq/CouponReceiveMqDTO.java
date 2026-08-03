package com.fresh.common.dto.mq;

import com.fresh.common.dto.MqBaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CouponReceiveMqDTO extends MqBaseDTO {

    private Long userId;
    private Long templateId;
    private Long userCouponId;
}
