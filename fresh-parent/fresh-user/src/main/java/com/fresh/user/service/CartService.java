package com.fresh.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fresh.common.constant.RedisKeyConstant;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.ContextUtil;
import com.alibaba.fastjson2.TypeReference;
import com.fresh.common.util.JsonUtils;
import com.fresh.common.util.RedisUtils;
import com.fresh.user.dto.CartUpdateDTO;
import com.fresh.user.entity.UserCart;
import com.fresh.user.mapper.UserCartMapper;
import com.fresh.user.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisUtils redisUtils;
    private final UserCartMapper userCartMapper;

    public List<CartItemVO> list() {
        Long userId = requireUserId();
        String cache = redisUtils.get(RedisKeyConstant.USER_CART + userId);
        if (cache != null) {
            return com.alibaba.fastjson2.JSON.parseObject(cache, new TypeReference<List<CartItemVO>>() {});
        }
        List<UserCart> dbList = userCartMapper.selectList(new LambdaQueryWrapper<UserCart>()
                .eq(UserCart::getUserId, userId)
                .eq(UserCart::getDelFlag, 0));
        List<CartItemVO> items = dbList.stream().map(this::toVO).toList();
        redisUtils.set(RedisKeyConstant.USER_CART + userId, JsonUtils.toJson(items));
        return items;
    }

    public void update(CartUpdateDTO dto) {
        Long userId = requireUserId();
        if (dto == null || dto.getGoodsId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品信息无效");
        }
        // 默认规格 id 可能为 0，允许
        if (dto.getSpecId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "请选择规格");
        }
        List<CartItemVO> items = new ArrayList<>(list());
        boolean found = false;
        boolean increment = Boolean.TRUE.equals(dto.getIncrement());
        for (int i = 0; i < items.size(); i++) {
            CartItemVO item = items.get(i);
            if (Objects.equals(item.getGoodsId(), dto.getGoodsId())
                    && Objects.equals(item.getSpecId(), dto.getSpecId())) {
                if (dto.getNum() != null && dto.getNum() <= 0 && !increment) {
                    items.remove(i);
                } else {
                    if (dto.getNum() != null) {
                        int base = item.getNum() == null ? 0 : item.getNum();
                        int next = increment ? base + dto.getNum() : dto.getNum();
                        if (next <= 0) {
                            items.remove(i);
                            found = true;
                            break;
                        }
                        item.setNum(next);
                    }
                    if (dto.getSelected() != null) {
                        item.setSelected(dto.getSelected());
                    }
                }
                found = true;
                break;
            }
        }
        if (!found && dto.getNum() != null && dto.getNum() > 0) {
            CartItemVO item = new CartItemVO();
            item.setGoodsId(dto.getGoodsId());
            item.setSpecId(dto.getSpecId());
            item.setNum(dto.getNum());
            item.setSelected(dto.getSelected() == null ? 1 : dto.getSelected());
            items.add(item);
        }
        redisUtils.set(RedisKeyConstant.USER_CART + userId, JsonUtils.toJson(items));
    }

    public void syncToDb() {
        // 定时任务可批量同步，此处简化
    }

    private CartItemVO toVO(UserCart cart) {
        CartItemVO vo = new CartItemVO();
        vo.setGoodsId(cart.getGoodsId());
        vo.setSpecId(cart.getSpecId());
        vo.setNum(cart.getNum());
        vo.setSelected(cart.getSelected());
        return vo;
    }

    private Long requireUserId() {
        Long userId = ContextUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }
}
