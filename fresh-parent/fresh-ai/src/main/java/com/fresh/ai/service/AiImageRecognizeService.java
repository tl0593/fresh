package com.fresh.ai.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fresh.ai.client.DashScopeClient;
import com.fresh.ai.dto.AfterSaleAiResultDTO;
import com.fresh.ai.dto.AfterSaleImageMqDTO;
import com.fresh.ai.dto.ImageDamageResultDTO;
import com.fresh.ai.entity.AiImageRecognize;
import com.fresh.ai.feign.OrderFeignClient;
import com.fresh.ai.mapper.AiImageRecognizeMapper;
import com.fresh.common.base.Result;
import com.fresh.common.constant.AfterSaleConstant;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiImageRecognizeService {

    private static final String VISION_PROMPT = """
            你是生鲜售后定损AI，请分析图片中商品的损坏情况。
            严格只返回JSON，不要其他文字，格式如下：
            {"damageLevel":1,"damageRatio":10.5,"refundAmount":5.00,"description":"轻微磕碰"}
            字段说明：damageLevel 1轻微 2中度 3重度；damageRatio为损坏百分比；refundAmount为建议理赔金额(元)。
            """;

    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*}", Pattern.DOTALL);

    private final DashScopeClient dashScopeClient;
    private final AiImageRecognizeMapper imageRecognizeMapper;
    private final OrderFeignClient orderFeignClient;
    private final AiMqProducer mqProducer;

    public ImageDamageResultDTO handleAfterSaleImage(AfterSaleImageMqDTO dto) {
        if (dto == null || dto.getAfterSaleId() == null || !StringUtils.hasText(dto.getImgUrl())) {
            log.warn("Invalid after sale image message: {}", dto);
            return null;
        }
        ImageDamageResultDTO result = recognize(dto.getImgUrl());
        saveRecord(dto, result);
        writeBackOrder(dto.getAfterSaleId(), result);
        mqProducer.sendRecognizeFinish(dto.getAfterSaleId(), dto.getUserId(), result);
        return result;
    }

    public ImageDamageResultDTO recognize(String imgUrl) {
        if (!StringUtils.hasText(imgUrl)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "图片格式非法");
        }
        String raw;
        try {
            raw = dashScopeClient.visionAnalyze(imgUrl, VISION_PROMPT);
        } catch (BusinessException e) {
            log.error("Vision analyze failed for {}", imgUrl, e);
            throw new BusinessException(ErrorCodeEnum.AI_SERVICE_ERROR.getCode(), "图像识别服务异常");
        }
        return parseResult(raw);
    }

    private ImageDamageResultDTO parseResult(String raw) {
        ImageDamageResultDTO dto = new ImageDamageResultDTO();
        dto.setDescription(raw);
        try {
            Matcher matcher = JSON_PATTERN.matcher(raw);
            if (matcher.find()) {
                JSONObject json = JSON.parseObject(matcher.group());
                dto.setDamageLevel(json.getInteger("damageLevel"));
                dto.setDamageRatio(json.getBigDecimal("damageRatio"));
                dto.setRefundAmount(json.getBigDecimal("refundAmount"));
                dto.setDescription(json.getString("description"));
            }
        } catch (Exception e) {
            log.warn("Parse vision result fallback: {}", raw);
        }
        if (dto.getDamageLevel() == null) {
            dto.setDamageLevel(AfterSaleConstant.DAMAGE_LIGHT);
        }
        if (dto.getDamageRatio() == null) {
            dto.setDamageRatio(BigDecimal.valueOf(10));
        }
        if (dto.getRefundAmount() == null) {
            dto.setRefundAmount(BigDecimal.valueOf(5));
        }
        return dto;
    }

    private void saveRecord(AfterSaleImageMqDTO mq, ImageDamageResultDTO result) {
        AiImageRecognize record = new AiImageRecognize();
        record.setAfterSaleId(mq.getAfterSaleId());
        record.setImgUrl(mq.getImgUrl());
        record.setRawResult(JSON.toJSONString(result));
        record.setDamageLevel(result.getDamageLevel());
        record.setDamageRatio(result.getDamageRatio());
        record.setRefundAmount(result.getRefundAmount());
        record.setCreateTime(LocalDateTime.now());
        imageRecognizeMapper.insert(record);
    }

    private void writeBackOrder(Long afterSaleId, ImageDamageResultDTO result) {
        AfterSaleAiResultDTO dto = new AfterSaleAiResultDTO();
        dto.setAfterSaleId(afterSaleId);
        dto.setAiDamageLevel(result.getDamageLevel());
        dto.setAiRate(result.getDamageRatio());
        dto.setAiRefundMoney(result.getRefundAmount());
        try {
            Result<Void> resp = orderFeignClient.updateAfterSaleAiResult(dto);
            if (resp == null || resp.getCode() != 200) {
                log.warn("Write back after sale failed: {}", resp);
            }
        } catch (Exception e) {
            log.error("Feign write back after sale error, afterSaleId={}", afterSaleId, e);
        }
    }
}
