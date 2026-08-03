package com.fresh.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fresh.ai.entity.AiChatRecord;
import com.fresh.ai.entity.AiGroupText;
import com.fresh.ai.entity.AiImageRecognize;
import com.fresh.ai.mapper.AiChatRecordMapper;
import com.fresh.ai.mapper.AiGroupTextMapper;
import com.fresh.ai.mapper.AiImageRecognizeMapper;
import com.fresh.common.base.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiLogService {

    private final AiChatRecordMapper chatRecordMapper;
    private final AiImageRecognizeMapper imageRecognizeMapper;
    private final AiGroupTextMapper groupTextMapper;

    public PageVO<AiChatRecord> chatLogPage(Integer pageNum, Integer pageSize, Long userId) {
        Page<AiChatRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiChatRecord> wrapper = new LambdaQueryWrapper<AiChatRecord>()
                .eq(userId != null, AiChatRecord::getUserId, userId)
                .orderByDesc(AiChatRecord::getCreateTime);
        Page<AiChatRecord> result = chatRecordMapper.selectPage(page, wrapper);
        return PageVO.of(result.getTotal(), result.getRecords());
    }

    public PageVO<AiImageRecognize> imageRecLogPage(Integer pageNum, Integer pageSize, Long afterSaleId) {
        Page<AiImageRecognize> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiImageRecognize> wrapper = new LambdaQueryWrapper<AiImageRecognize>()
                .eq(afterSaleId != null, AiImageRecognize::getAfterSaleId, afterSaleId)
                .orderByDesc(AiImageRecognize::getCreateTime);
        Page<AiImageRecognize> result = imageRecognizeMapper.selectPage(page, wrapper);
        return PageVO.of(result.getTotal(), result.getRecords());
    }

    public PageVO<AiGroupText> groupTextLogPage(Integer pageNum, Integer pageSize, Long goodsId) {
        Page<AiGroupText> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiGroupText> wrapper = new LambdaQueryWrapper<AiGroupText>()
                .eq(goodsId != null, AiGroupText::getGoodsId, goodsId)
                .orderByDesc(AiGroupText::getCreateTime);
        Page<AiGroupText> result = groupTextMapper.selectPage(page, wrapper);
        return PageVO.of(result.getTotal(), result.getRecords());
    }
}
