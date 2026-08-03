package com.fresh.common.dto.mq;

import com.fresh.common.dto.MqBaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class IntegralChangeMqDTO extends MqBaseDTO {

    private Long userId;
    private Integer changeNum;
    private Long orderId;
    private String remark;
}
