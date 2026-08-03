package com.fresh.goods.controller;

import com.fresh.common.base.PageVO;
import com.fresh.common.base.Result;
import com.fresh.goods.entity.Goods;
import com.fresh.goods.entity.GroupActivity;
import com.fresh.goods.entity.SeckillActivity;
import com.fresh.goods.service.GoodsService;
import com.fresh.goods.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @GetMapping("/category/tree")
    public Result<List<CategoryTreeVO>> categoryTree() {
        return Result.success(goodsService.categoryTree());
    }

    @GetMapping("/goods/hot")
    public Result<List<Goods>> hot() {
        return Result.success(goodsService.hotList());
    }

    /** 分类选购：按分类/关键词/销量或价格排序分页 */
    @GetMapping("/goods/list")
    public Result<PageVO<Goods>> goodsList(
            @RequestParam(value = "catId", required = false) Long catId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sortType", defaultValue = "sale") String sortType,
            @RequestParam(value = "sortOrder", defaultValue = "desc") String sortOrder,
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return Result.success(goodsService.pageGoods(catId, keyword, sortType, sortOrder, pageNum, pageSize));
    }

    /** 仅匹配数字 id，避免 /goods/list 被当成商品详情 */
    @GetMapping("/goods/{goodsId:\\d+}")
    public Result<GoodsDetailVO> detail(@PathVariable("goodsId") Long goodsId) {
        return Result.success(goodsService.detail(goodsId));
    }

    @GetMapping("/group/list")
    public Result<List<GroupActivity>> groupList() {
        return Result.success(goodsService.groupList());
    }

    @GetMapping("/seckill/list")
    public Result<List<SeckillActivity>> seckillList() {
        return Result.success(goodsService.seckillList());
    }
}
