package com.fresh.common.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

public final class IdUtils {

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    private IdUtils() {
    }

    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    public static String nextIdStr() {
        return SNOWFLAKE.nextIdStr();
    }
}
