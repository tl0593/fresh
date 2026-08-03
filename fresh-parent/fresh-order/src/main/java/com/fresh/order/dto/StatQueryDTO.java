package com.fresh.order.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StatQueryDTO {

    private LocalDate startDate;
    private LocalDate endDate;
}
