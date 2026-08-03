package com.fresh.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.ContextUtil;
import com.fresh.message.entity.MsgTemplate;
import com.fresh.message.entity.UserInnerMsg;
import com.fresh.message.mapper.MsgTemplateMapper;
import com.fresh.message.mapper.UserInnerMsgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final UserInnerMsgMapper innerMsgMapper;
    private final MsgTemplateMapper templateMapper;

    public List<UserInnerMsg> listInnerMsg() {
        Long userId = ContextUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        return innerMsgMapper.selectList(new LambdaQueryWrapper<UserInnerMsg>()
                .eq(UserInnerMsg::getUserId, userId)
                .orderByDesc(UserInnerMsg::getCreateTime));
    }

    public void readInnerMsg(Long msgId) {
        Long userId = ContextUtil.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCodeEnum.UNAUTHORIZED);
        }
        int rows = innerMsgMapper.update(null, new LambdaUpdateWrapper<UserInnerMsg>()
                .eq(UserInnerMsg::getId, msgId)
                .eq(UserInnerMsg::getUserId, userId)
                .set(UserInnerMsg::getReadFlag, 1));
        if (rows == 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST);
        }
    }

    public List<MsgTemplate> listTemplate() {
        return templateMapper.selectList(new LambdaQueryWrapper<MsgTemplate>()
                .eq(MsgTemplate::getStatus, 1)
                .orderByDesc(MsgTemplate::getId));
    }
}
