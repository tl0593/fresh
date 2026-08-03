package com.fresh.common.base;

import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {

    private Long total;
    private List<T> records;

    public static <T> PageVO<T> of(Long total, List<T> records) {
        PageVO<T> page = new PageVO<>();
        page.setTotal(total);
        page.setRecords(records);
        return page;
    }
}
