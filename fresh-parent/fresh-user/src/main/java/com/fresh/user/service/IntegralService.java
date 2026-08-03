package com.fresh.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.user.dto.IntegralDTO;
import com.fresh.user.entity.AppUser;
import com.fresh.user.entity.UserIntegralLog;
import com.fresh.user.mapper.AppUserMapper;
import com.fresh.user.mapper.UserIntegralLogMapper;
import com.fresh.user.vo.AppUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IntegralService {

    private final AppUserMapper appUserMapper;
    private final UserIntegralLogMapper integralLogMapper;

    public List<UserIntegralLog> logs(Long userId) {
        return integralLogMapper.selectList(new LambdaQueryWrapper<UserIntegralLog>()
                .eq(UserIntegralLog::getUserId, userId)
                .orderByDesc(UserIntegralLog::getCreateTime));
    }

    public AppUserVO getUserById(Long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "用户不存在");
        }
        AppUserVO vo = new AppUserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void freeze(IntegralDTO dto) {
        AppUser user = requireUser(dto.getUserId());
        if (user.getIntegral() < dto.getIntegral()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "积分不足");
        }
        user.setIntegral(user.getIntegral() - dto.getIntegral());
        user.setFrozenIntegral(user.getFrozenIntegral() + dto.getIntegral());
        appUserMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unfreeze(IntegralDTO dto) {
        AppUser user = requireUser(dto.getUserId());
        user.setIntegral(user.getIntegral() + dto.getIntegral());
        user.setFrozenIntegral(Math.max(0, user.getFrozenIntegral() - dto.getIntegral()));
        appUserMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void change(IntegralDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getIntegral() == null || dto.getIntegral() == 0) {
            return;
        }
        // 同一订单的正向奖励只发一次（支付 MQ / 核销完成 双路径幂等）
        if (dto.getIntegral() > 0 && dto.getOrderId() != null && hasPositiveRewardForOrder(dto.getOrderId())) {
            return;
        }
        AppUser user = requireUser(dto.getUserId());
        if (dto.getIntegral() < 0) {
            user.setFrozenIntegral(Math.max(0, user.getFrozenIntegral() + dto.getIntegral()));
        } else {
            user.setIntegral((user.getIntegral() == null ? 0 : user.getIntegral()) + dto.getIntegral());
        }
        appUserMapper.updateById(user);

        UserIntegralLog log = new UserIntegralLog();
        log.setUserId(dto.getUserId());
        log.setChangeNum(dto.getIntegral());
        log.setType(dto.getIntegral() > 0 ? 1 : 2);
        log.setOrderId(dto.getOrderId());
        log.setRemark(dto.getRemark());
        integralLogMapper.insert(log);
    }

    public boolean hasPositiveRewardForOrder(Long orderId) {
        if (orderId == null) {
            return false;
        }
        Long count = integralLogMapper.selectCount(new LambdaQueryWrapper<UserIntegralLog>()
                .eq(UserIntegralLog::getOrderId, orderId)
                .gt(UserIntegralLog::getChangeNum, 0));
        return count != null && count > 0;
    }

    /** 扣减可用积分（兑券/抽奖），直接扣余额并记流水 */
    @Transactional(rollbackFor = Exception.class)
    public void deduct(IntegralDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getIntegral() == null || dto.getIntegral() <= 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "积分扣减参数无效");
        }
        AppUser user = requireUser(dto.getUserId());
        int cost = dto.getIntegral();
        if (user.getIntegral() == null || user.getIntegral() < cost) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "积分不足");
        }
        user.setIntegral(user.getIntegral() - cost);
        appUserMapper.updateById(user);

        UserIntegralLog log = new UserIntegralLog();
        log.setUserId(dto.getUserId());
        log.setChangeNum(-cost);
        log.setType(3);
        log.setOrderId(dto.getOrderId());
        log.setRemark(dto.getRemark() != null ? dto.getRemark() : "积分消费");
        integralLogMapper.insert(log);
    }

    private AppUser requireUser(Long userId) {
        AppUser user = appUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "用户不存在");
        }
        return user;
    }
}
