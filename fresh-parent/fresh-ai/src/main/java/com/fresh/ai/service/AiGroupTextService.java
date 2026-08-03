package com.fresh.ai.service;

import com.alibaba.fastjson2.JSON;
import com.fresh.ai.client.DashScopeClient;
import com.fresh.ai.dto.GoodsInfoDTO;
import com.fresh.ai.dto.GroupTextGenerateDTO;
import com.fresh.ai.entity.AiGroupText;
import com.fresh.ai.mapper.AiGroupTextMapper;
import com.fresh.ai.vo.GroupTextVO;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.ContextUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiGroupTextService {

    private static final String GROUP_TEXT_PROMPT = """
            你是生鲜团购文案策划，根据商品信息生成3套不同风格的宣传文案。
            每套文案80字以内，突出新鲜、产地、性价比。
            输出格式：用 --- 分隔3套文案，不要编号。
            """;

    private final DashScopeClient dashScopeClient;
    private final AiGroupTextMapper groupTextMapper;

    public GroupTextVO generate(GroupTextGenerateDTO dto) {
        validateInput(dto);
        String inputInfo = buildInputInfo(dto);
        String output;
        try {
            output = dashScopeClient.chatWithSystem(GROUP_TEXT_PROMPT + "\n商品信息：\n" + inputInfo, List.of());
        } catch (BusinessException e) {
            output = "新鲜直达，产地直供的" + dto.getGoodsName() + "，限时团购超值享！\n---\n"
                    + dto.getGoodsName() + "，" + dto.getSpec() + "，社区邻居都在买！\n---\n"
                    + "今日团购：" + dto.getGoodsName() + "，新鲜看得见。";
        }
        List<String> texts = Arrays.stream(output.split("---"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        AiGroupText log = new AiGroupText();
        log.setAdminId(ContextUtil.getAdminId() != null ? ContextUtil.getAdminId() : 0L);
        log.setGoodsId(dto.getGoodsId() == null ? 0L : dto.getGoodsId());
        log.setInputInfo(inputInfo);
        log.setOutputText(output);
        log.setCreateTime(LocalDateTime.now());
        groupTextMapper.insert(log);

        GroupTextVO vo = new GroupTextVO();
        vo.setTexts(texts);
        vo.setLogId(log.getId());
        return vo;
    }

    public String generateForFeign(GoodsInfoDTO dto) {
        GroupTextGenerateDTO gen = new GroupTextGenerateDTO();
        gen.setGoodsId(dto.getGoodsId());
        gen.setGoodsName(dto.getGoodsName());
        gen.setSpec(dto.getSpec());
        gen.setOrigin(dto.getOrigin());
        gen.setExtraInfo(dto.getExtraInfo());
        GroupTextVO vo = generate(gen);
        return String.join("\n---\n", vo.getTexts());
    }

    private void validateInput(GroupTextGenerateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getGoodsName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品信息缺失，无法生成文案");
        }
    }

    private String buildInputInfo(GroupTextGenerateDTO dto) {
        return JSON.toJSONString(dto);
    }
}
