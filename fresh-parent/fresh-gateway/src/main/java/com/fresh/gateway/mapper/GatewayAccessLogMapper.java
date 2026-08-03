package com.fresh.gateway.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fresh.gateway.entity.GatewayAccessLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GatewayAccessLogMapper extends BaseMapper<GatewayAccessLog> {
}
