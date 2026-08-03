package com.fresh.goods.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CategoryTreeVO {

    private Long id;
    private Long parentId;
    private String catName;
    private String icon;
    private Integer sort;
    private List<CategoryTreeVO> children = new ArrayList<>();
}
