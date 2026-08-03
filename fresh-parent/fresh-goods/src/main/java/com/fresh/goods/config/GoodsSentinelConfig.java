package com.fresh.goods.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class GoodsSentinelConfig {

    @PostConstruct
    public void initRules() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule commentRule = new FlowRule("submitComment");
        commentRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        commentRule.setCount(20);
        rules.add(commentRule);
        FlowRule couponRule = new FlowRule("receiveCoupon");
        couponRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        couponRule.setCount(50);
        rules.add(couponRule);
        FlowRuleManager.loadRules(rules);
    }
}
