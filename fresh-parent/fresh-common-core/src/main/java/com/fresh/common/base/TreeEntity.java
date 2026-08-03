package com.fresh.common.base;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TreeEntity<T> {

    private Long id;
    private Long parentId;
    private String name;
    private LocalDateTime createTime;

    @TableField(exist = false)
    private List<T> children = new ArrayList<>();
}
