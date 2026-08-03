package com.fresh.data.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserTrendVO {

    private List<Item> points = new ArrayList<>();

    @Data
    public static class Item {
        private LocalDate statDate;
        private Integer newUser;
        private Integer activeUser;
    }
}
