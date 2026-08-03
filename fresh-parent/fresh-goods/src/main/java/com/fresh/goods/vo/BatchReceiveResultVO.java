package com.fresh.goods.vo;

import lombok.Data;

@Data
public class BatchReceiveResultVO {

    private int successCount;
    private int failCount;
    private String message;
}
