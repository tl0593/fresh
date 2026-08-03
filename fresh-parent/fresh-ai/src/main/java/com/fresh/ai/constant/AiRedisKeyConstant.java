package com.fresh.ai.constant;

public final class AiRedisKeyConstant {

    private AiRedisKeyConstant() {
    }

    public static final String SESSION_PREFIX = "ai:session:";
    public static final String KNOWLEDGE_LIST = "ai:knowledge:list";
    public static final String COOK_PREFIX = "ai:cook:";

    public static String sessionKey(Long userId, String sessionKey) {
        return SESSION_PREFIX + userId + ":" + sessionKey;
    }

    public static String cookKey(Long userId) {
        return COOK_PREFIX + userId;
    }
}
