package com.fresh.goods.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fresh.common.base.PageVO;
import com.fresh.common.base.Result;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.ContextUtil;
import com.fresh.common.util.JsonUtils;
import com.fresh.common.util.RedisUtils;
import com.fresh.goods.config.GoodsProperties;
import com.fresh.goods.constant.GoodsRedisKeyConstant;
import com.fresh.goods.dto.CommentReplyDTO;
import com.fresh.goods.dto.CommentSubmitDTO;
import com.fresh.goods.dto.OrderItemCheckVO;
import com.fresh.goods.entity.comment.CommentImage;
import com.fresh.goods.entity.comment.CommentReply;
import com.fresh.goods.entity.comment.GoodsComment;
import com.fresh.goods.feign.OrderFeignClient;
import com.fresh.goods.mapper.comment.CommentImageMapper;
import com.fresh.goods.mapper.comment.CommentReplyMapper;
import com.fresh.goods.mapper.comment.GoodsCommentMapper;
import com.fresh.goods.vo.CommentListVO;
import com.fresh.goods.vo.CommentRateVO;
import com.fresh.goods.vo.CommentReplyVO;
import com.fresh.goods.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final GoodsCommentMapper commentMapper;
    private final CommentImageMapper imageMapper;
    private final CommentReplyMapper replyMapper;
    private final OrderFeignClient orderFeignClient;
    private final RedisUtils redisUtils;
    private final GoodsProperties properties;
    private final GoodsMqProducer mqProducer;

    @SentinelResource("submitComment")
    @Transactional(transactionManager = "commentTransactionManager", rollbackFor = Exception.class)
    public void submit(CommentSubmitDTO dto) {
        Long userId = requireUserId();
        if (dto.getOrderItemId() == null || dto.getOrderItemId() <= 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "请从订单页进入评价");
        }
        if (dto.getScore() == null || dto.getScore() < 1 || dto.getScore() > 5) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "评分需在1-5星之间");
        }
        Result<OrderItemCheckVO> checkResult;
        try {
            checkResult = orderFeignClient.checkCanComment(dto.getOrderItemId());
        } catch (Exception e) {
            log.warn("校验评价资格 Feign 调用失败, orderItemId={}", dto.getOrderItemId(), e);
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "校验评价资格失败，请稍后重试");
        }
        if (checkResult == null || checkResult.getCode() == null || checkResult.getCode() != 200
                || checkResult.getData() == null) {
            String msg = checkResult != null && checkResult.getMsg() != null ? checkResult.getMsg() : "订单不可评价";
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), msg);
        }
        if (!Boolean.TRUE.equals(checkResult.getData().getCanComment())) {
            OrderItemCheckVO check = checkResult.getData();
            if (check.getIsCommented() != null && check.getIsCommented() == 1) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "该商品已评价");
            }
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "仅已完成订单可评价");
        }
        OrderItemCheckVO check = checkResult.getData();
        if (!userId.equals(check.getUserId())) {
            throw new BusinessException(ErrorCodeEnum.FORBIDDEN);
        }
        Long exists = commentMapper.selectCount(new LambdaQueryWrapper<GoodsComment>()
                .eq(GoodsComment::getOrderItemId, dto.getOrderItemId())
                .eq(GoodsComment::getDelFlag, 0));
        if (exists != null && exists > 0) {
            // 修复历史：评价已存在但订单项未标记时，补标已评价
            try {
                orderFeignClient.markCommented(dto.getOrderItemId());
            } catch (Exception e) {
                log.warn("补标订单项已评价失败 orderItemId={}", dto.getOrderItemId());
            }
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "该商品已评价");
        }

        GoodsComment comment = new GoodsComment();
        comment.setUserId(userId);
        comment.setOrderItemId(dto.getOrderItemId());
        comment.setOrderNo(check.getOrderNo());
        comment.setGoodsId(check.getGoodsId());
        comment.setSpecId(check.getSpecId());
        comment.setScore(dto.getScore());
        comment.setContent(dto.getContent());
        comment.setStatus(1);
        comment.setDelFlag(0);
        commentMapper.insert(comment);

        if (dto.getImages() != null) {
            int sort = 0;
            Set<String> uniq = new LinkedHashSet<>();
            for (String img : dto.getImages()) {
                if (img == null || img.isBlank()) {
                    continue;
                }
                String url = img.trim();
                // 微信临时路径不能入库
                if (url.startsWith("http://tmp") || url.startsWith("https://tmp")
                        || url.startsWith("wxfile://") || url.startsWith("file://")) {
                    continue;
                }
                if (!uniq.add(url)) {
                    continue;
                }
                CommentImage image = new CommentImage();
                image.setCommentId(comment.getId());
                image.setImgUrl(url);
                image.setSort(sort++);
                imageMapper.insert(image);
            }
        }

        Map<String, Object> mq = new HashMap<>();
        mq.put("commentId", comment.getId());
        mq.put("orderItemId", dto.getOrderItemId());
        mq.put("goodsId", check.getGoodsId());
        mq.put("userId", userId);
        try {
            orderFeignClient.markCommented(dto.getOrderItemId());
        } catch (Exception e) {
            log.warn("同步标记订单项已评价失败, orderItemId={}, 将依赖 MQ 兜底", dto.getOrderItemId(), e);
        }
        mqProducer.sendCommentAdd(mq);

        clearCommentCache(check.getGoodsId());
    }

    public CommentListVO listByGoods(Long goodsId, Integer pageNum, Integer pageSize) {
        // 评价列表直接查库：避免空列表被缓存后新评价不展示（管理端/详情页）
        Page<GoodsComment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<GoodsComment>()
                        .eq(GoodsComment::getGoodsId, goodsId)
                        .eq(GoodsComment::getStatus, 1)
                        .eq(GoodsComment::getDelFlag, 0)
                        .orderByDesc(GoodsComment::getCreateTime));

        CommentListVO vo = new CommentListVO();
        vo.setTotal(page.getTotal());
        vo.setRecords(buildCommentVOList(page.getRecords()));

        CommentRateVO rate = getCommentRate(goodsId);
        vo.setAvgScore(rate.getAvgScore());
        vo.setGoodRate(rate.getGoodRate());
        return vo;
    }

    public List<CommentVO> listByUser() {
        Long userId = requireUserId();
        List<GoodsComment> comments = commentMapper.selectList(new LambdaQueryWrapper<GoodsComment>()
                .eq(GoodsComment::getUserId, userId)
                .eq(GoodsComment::getDelFlag, 0)
                .orderByDesc(GoodsComment::getCreateTime));
        return buildCommentVOList(comments);
    }

    /** 某订单下已评价的订单项 ID（用于按商品评价、隐藏按钮） */
    public List<Long> commentedOrderItemIds(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return List.of();
        }
        Long userId = requireUserId();
        return commentMapper.selectList(new LambdaQueryWrapper<GoodsComment>()
                        .eq(GoodsComment::getOrderNo, orderNo)
                        .eq(GoodsComment::getUserId, userId)
                        .eq(GoodsComment::getDelFlag, 0)
                        .select(GoodsComment::getOrderItemId))
                .stream()
                .map(GoodsComment::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    public PageVO<CommentVO> adminPage(Integer pageNum, Integer pageSize) {
        Page<GoodsComment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<GoodsComment>()
                        .eq(GoodsComment::getDelFlag, 0)
                        .orderByDesc(GoodsComment::getCreateTime));
        return PageVO.of(page.getTotal(), buildCommentVOList(page.getRecords()));
    }

    @Transactional(transactionManager = "commentTransactionManager", rollbackFor = Exception.class)
    public void hide(Long commentId) {
        GoodsComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "评价不存在");
        }
        comment.setStatus(0);
        commentMapper.updateById(comment);
        clearCommentCache(comment.getGoodsId());
    }

    @Transactional(transactionManager = "commentTransactionManager", rollbackFor = Exception.class)
    public void reply(CommentReplyDTO dto) {
        GoodsComment comment = commentMapper.selectById(dto.getCommentId());
        if (comment == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "评价不存在");
        }
        CommentReply reply = new CommentReply();
        reply.setCommentId(dto.getCommentId());
        reply.setAdminId(dto.getAdminId());
        reply.setReplyContent(dto.getReplyContent());
        reply.setDelFlag(0);
        replyMapper.insert(reply);
        clearCommentCache(comment.getGoodsId());
    }

    public CommentRateVO getCommentRate(Long goodsId) {
        String cacheKey = GoodsRedisKeyConstant.GOODS_SCORE + goodsId;
        String cache = redisUtils.get(cacheKey);
        if (cache != null) {
            return JsonUtils.fromJson(cache, CommentRateVO.class);
        }

        List<GoodsComment> all = commentMapper.selectList(new LambdaQueryWrapper<GoodsComment>()
                .eq(GoodsComment::getGoodsId, goodsId)
                .eq(GoodsComment::getStatus, 1)
                .eq(GoodsComment::getDelFlag, 0));
        CommentRateVO rate = new CommentRateVO();
        rate.setGoodsId(goodsId);
        rate.setTotalCount((long) all.size());
        if (all.isEmpty()) {
            rate.setAvgScore(5.0);
            rate.setGoodRate(100.0);
        } else {
            double avg = all.stream().mapToInt(GoodsComment::getScore).average().orElse(5.0);
            long good = all.stream().filter(c -> c.getScore() >= 4).count();
            rate.setAvgScore(Math.round(avg * 10) / 10.0);
            rate.setGoodRate(Math.round(good * 1000.0 / all.size()) / 10.0);
        }
        redisUtils.set(cacheKey, JsonUtils.toJson(rate), 900, TimeUnit.SECONDS);
        return rate;
    }

    private List<CommentVO> buildCommentVOList(List<GoodsComment> comments) {
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> commentIds = comments.stream().map(GoodsComment::getId).collect(Collectors.toList());
        Map<Long, List<String>> imageMap = imageMapper.selectList(new LambdaQueryWrapper<CommentImage>()
                        .in(CommentImage::getCommentId, commentIds))
                .stream()
                .collect(Collectors.groupingBy(CommentImage::getCommentId,
                        Collectors.mapping(CommentImage::getImgUrl, Collectors.toList())));
        Map<Long, CommentReply> replyMap = replyMapper.selectList(new LambdaQueryWrapper<CommentReply>()
                        .in(CommentReply::getCommentId, commentIds)
                        .eq(CommentReply::getDelFlag, 0))
                .stream()
                .collect(Collectors.toMap(CommentReply::getCommentId, r -> r, (a, b) -> b));

        List<CommentVO> list = new ArrayList<>();
        for (GoodsComment c : comments) {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(c, vo);
            vo.setImages(imageMap.getOrDefault(c.getId(), List.of()));
            CommentReply reply = replyMap.get(c.getId());
            if (reply != null) {
                CommentReplyVO replyVO = new CommentReplyVO();
                BeanUtils.copyProperties(reply, replyVO);
                vo.setReply(replyVO);
            }
            list.add(vo);
        }
        return list;
    }

    private void clearCommentCache(Long goodsId) {
        if (goodsId == null) {
            return;
        }
        redisUtils.delete(GoodsRedisKeyConstant.GOODS_SCORE + goodsId);
        // 商品详情缓存内嵌 commentRate，评价变更后需失效
        redisUtils.delete(GoodsRedisKeyConstant.GOODS_DETAIL + goodsId);
        // listByGoods 按 pageNum/pageSize 缓存，提交后清理常用页，避免详情/列表仍显示旧空数据
        int[] sizes = {1, 10, 30, 50};
        for (int size : sizes) {
            for (int page = 1; page <= 5; page++) {
                redisUtils.delete(GoodsRedisKeyConstant.GOODS_COMMENT + goodsId + ":" + page + ":" + size);
            }
        }
    }

    private Long requireUserId() {
        Long userId = ContextUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }
}
