package com.imooc.controller;

import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;

import com.imooc.utils.SMSUtils;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Api(tags = "HelloController 测试的接口")
@RestController
@RefreshScope
public class HelloController {
    @Value("${nacos.counts}")
    private Integer nacosCounts;


    @GetMapping("nacosCounts")
    public Object nacosCounts() {
        return GraceJSONResult.ok("nacosCounts的数值"+nacosCounts);
    }


    @GetMapping("hello")
    public Object hello() {
        return GraceJSONResult.ok("Hello SpringBoot~");
    }

    @Autowired
    private SMSUtils smsUtils;
    @GetMapping("sms")
    public Object sms() throws Exception {

        String code = "123456";
        smsUtils.sendSMS("+12064090298",code);
        return GraceJSONResult.ok();
    }
}


