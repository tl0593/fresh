package com.fresh.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AfterSaleAuditDTO {

    private Long id;
    /** 1通过 2驳回 */
    private Integer auditStatus;
    /** 通过时实际退款金额，为空则用 AI 建议金额 */
    private BigDecimal actualRefundMoney;
    private String remark;
}
