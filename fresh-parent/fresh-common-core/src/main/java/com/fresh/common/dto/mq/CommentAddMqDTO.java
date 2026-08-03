package com.fresh.common.dto.mq;

import com.fresh.common.dto.MqBaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CommentAddMqDTO extends MqBaseDTO {

    private Long commentId;
    private Long orderItemId;
    private Long goodsId;
    private Long userId;
}
