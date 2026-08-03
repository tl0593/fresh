package com.fresh.data.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserBehaviorTypeConstantTest {

    @Test
    void newBehaviorTypes_defined() {
        assertThat(UserBehaviorTypeConstant.COMMENT).isEqualTo(6);
        assertThat(UserBehaviorTypeConstant.COUPON).isEqualTo(7);
        assertThat(UserBehaviorTypeConstant.REGISTER).isEqualTo(8);
    }
}
