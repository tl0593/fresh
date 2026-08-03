package com.fresh.order.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OrderSentinelConfig {

    @PostConstruct
    public void initRules() {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule createRule = new FlowRule("createOrder");
        createRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        createRule.setCount(5);
        rules.add(createRule);
        FlowRule payRule = new FlowRule("payCallback");
        payRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        payRule.setCount(100);
        rules.add(payRule);
        FlowRuleManager.loadRules(rules);
    }
}
