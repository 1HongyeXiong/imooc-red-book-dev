package com.imooc.controller;

import com.imooc.base.BaseInfoProperties;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.mo.MessageMO;
import com.imooc.service.MsgService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "MessageController 消息功能模块")
@Slf4j
@RestController
@RequestMapping("msg")
public class MsgController extends BaseInfoProperties {

    @Autowired
    private MsgService msgService;

    @ApiOperation(value = "查询用户的消息列表")
    @GetMapping("list")
    public GraceJSONResult list(@RequestParam String userId,
                                @RequestParam Integer page,
                                @RequestParam Integer pageSize) {

        // MongoDB 从0分页，区别于数据库
        if (page == null) {
            page = COMMON_START_PAGE_ZERO;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        List<MessageMO> result = msgService.queryList(userId, page, pageSize);
        return GraceJSONResult.ok(result);
    }

}

