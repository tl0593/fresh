package com.fresh.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.ContextUtil;
import com.fresh.user.entity.UserAddress;
import com.fresh.user.mapper.UserAddressMapper;
import com.fresh.user.vo.UserAddressVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final UserAddressMapper userAddressMapper;

    public List<UserAddressVO> list() {
        Long userId = requireUserId();
        return userAddressMapper.selectList(new LambdaQueryWrapper<UserAddress>()
                        .eq(UserAddress::getUserId, userId)
                        .eq(UserAddress::getDelFlag, 0)
                        .orderByDesc(UserAddress::getIsDefault))
                .stream().map(this::toVO).toList();
    }

    public void save(UserAddress address) {
        address.setUserId(requireUserId());
        if (address.getId() == null) {
            userAddressMapper.insert(address);
        } else {
            userAddressMapper.updateById(address);
        }
    }

    public UserAddressVO getById(Long addressId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null || address.getDelFlag() == 1) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "地址不存在");
        }
        return toVO(address);
    }

    private UserAddressVO toVO(UserAddress address) {
        UserAddressVO vo = new UserAddressVO();
        BeanUtils.copyProperties(address, vo);
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
