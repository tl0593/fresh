package com.fresh.goods.controller;

import com.fresh.common.base.Result;
import com.fresh.goods.dto.CommentSubmitDTO;
import com.fresh.goods.service.CommentService;
import com.fresh.goods.vo.CommentListVO;
import com.fresh.goods.vo.CommentRateVO;
import com.fresh.goods.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/comment/submit")
    public Result<Void> submit(@RequestBody CommentSubmitDTO dto) {
        commentService.submit(dto);
        return Result.success();
    }

    @GetMapping("/comment/list/{goodsId}")
    public Result<CommentListVO> list(@PathVariable("goodsId") Long goodsId,
                                    @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                    @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {
        return Result.success(commentService.listByGoods(goodsId, pageNum, pageSize));
    }

    /** 商品评价统计（总数/好评率），详情页专用 */
    @GetMapping("/comment/stats/{goodsId}")
    public Result<CommentRateVO> stats(@PathVariable("goodsId") Long goodsId) {
        return Result.success(commentService.getCommentRate(goodsId));
    }

    @GetMapping("/comment/user/list")
    public Result<List<CommentVO>> userList() {
        return Result.success(commentService.listByUser());
    }

    /** 订单内已评价的订单项 ID 列表 */
    @GetMapping("/comment/order/{orderNo}/done")
    public Result<List<Long>> commentedItemIds(@PathVariable("orderNo") String orderNo) {
        return Result.success(commentService.commentedOrderItemIds(orderNo));
    }
}
