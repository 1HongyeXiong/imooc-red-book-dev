package com.imooc.controller;

import com.imooc.base.BaseInfoProperties;
import com.imooc.bo.VlogBO;
import com.imooc.enums.YesOrNo;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.service.VlogService;
import com.imooc.utils.PagedGridResult;
import com.imooc.vo.IndexVlogVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


@Api(tags = "VlogController Vlog短视频功能模块")
@Slf4j
@RestController
@RequestMapping("vlog")
@RefreshScope
public class VlogController extends BaseInfoProperties {

    @Autowired
    private VlogService vlogService;

    @ApiOperation(value = "发布视频")
    @PostMapping("publish")
    public GraceJSONResult publish(@Valid @RequestBody VlogBO vlogBO) {

        // TODO BO校验作为作业
        vlogService.createVlog(vlogBO);
        return GraceJSONResult.ok();
    }


    @GetMapping("indexList")
    public GraceJSONResult indexList(@RequestParam(defaultValue = "") String search,
                                     @RequestParam(defaultValue = "") String userId,
                                     @RequestParam Integer page,
                                     @RequestParam Integer pageSize) {

        // 关于page和pageSize的默认值思考，可以看一下下一节图文
        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = vlogService.queryIndexVlogList(search,userId, page, pageSize);
        return GraceJSONResult.ok(gridResult);
    }

    @GetMapping("detail")
    public GraceJSONResult detail(@RequestParam(defaultValue = "") String userId, @RequestParam String vlogId) {
        IndexVlogVO result = vlogService.getVlogDetail(userId, vlogId);
        return GraceJSONResult.ok(result);
    }


    @PostMapping("changeToPrivate")
    public GraceJSONResult changeToPrivate(@RequestParam String userId,
                                           @RequestParam String vlogId) {

        vlogService.changeToPrivateOrPublic(userId, vlogId, YesOrNo.YES.type);
        return GraceJSONResult.ok();
    }

    @PostMapping("changeToPublic")
    public GraceJSONResult changeToPublic(@RequestParam String userId,
                                          @RequestParam String vlogId) {

        vlogService.changeToPrivateOrPublic(userId, vlogId, YesOrNo.NO.type);
        return GraceJSONResult.ok();
    }


    @ApiOperation(value = "我的公开视频")
    @GetMapping("myPublicList")
    public GraceJSONResult myPublicList(@RequestParam String userId,
                                        @RequestParam Integer page,
                                        @RequestParam Integer pageSize) {
        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = vlogService.queryMyVlogList(userId, page, pageSize, YesOrNo.NO.type);
        return GraceJSONResult.ok(gridResult);
    }


    @ApiOperation(value = "我的私密视频")
    @GetMapping("myPrivateList")
    public GraceJSONResult myPrivateList(@RequestParam String userId,
                                         @RequestParam Integer page,
                                         @RequestParam Integer pageSize) {
        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = vlogService.queryMyVlogList(userId, page, pageSize, YesOrNo.YES.type);
        return GraceJSONResult.ok(gridResult);
    }

    @Value("${nacos.counts}")
    private Integer nacosCounts;

    @PostMapping("like")
    public GraceJSONResult like(@RequestParam String userId,
                                @RequestParam String vlogerId,
                                @RequestParam String vlogId) {


        // 被点赞后，这个视频和视频发布者的获赞都会+1
        redis.increment(REDIS_VLOGER_BE_LIKED_COUNTS + ":" + vlogerId,1);
        redis.increment(REDIS_VLOG_BE_LIKED_COUNTS + ":" + vlogId,1);
        // 我点赞的视频，在redis中保存关联关系
        redis.set(REDIS_USER_LIKE_VLOG + ":" + userId + ":" + vlogId, "1");

        // 我点赞的视频，关联关系保存到数据库
        vlogService.userLikeVolg(vlogId, userId);

        String countsStr = redis.get(REDIS_VLOG_BE_LIKED_COUNTS + ":" + vlogId);
        Integer counts = 0;
        if (StringUtils.isNotBlank(countsStr)) {
            counts = Integer.valueOf(countsStr);
            if (counts >= nacosCounts) {
                vlogService.flushCounts(vlogId, counts);
            }
        }
// TODO: 用MQ解耦


        return GraceJSONResult.ok();
    }


    @PostMapping("unlike")
    public GraceJSONResult unlike(@RequestParam String userId,
                                  @RequestParam String vlogerId,
                                  @RequestParam String vlogId) {

        redis.decrement(REDIS_VLOGER_BE_LIKED_COUNTS + ":" + vlogerId,1);
        redis.decrement(REDIS_VLOG_BE_LIKED_COUNTS + ":" + vlogId,1);
        redis.del(REDIS_USER_LIKE_VLOG + ":" + userId + ":" + vlogId);

        // 我取消喜欢的视频，关联关系在数据库中删除
        vlogService.userUnLikeVolg(vlogId, userId);

        return GraceJSONResult.ok();
    }


    @ApiOperation(value = "查询短视频被喜欢/点赞的总数")
    @PostMapping("totalLikedCounts")
    public GraceJSONResult totalLikedCounts(@RequestParam String vlogId) {

        String countsStr = redis.get(REDIS_VLOG_BE_LIKED_COUNTS + ":" + vlogId);
        if (StringUtils.isBlank(countsStr)) {
            countsStr = "0";
        }

        return GraceJSONResult.ok(Integer.valueOf(countsStr));
    }


    @GetMapping("myLikedList")
    public GraceJSONResult myLikedList(@RequestParam String userId,
                                       @RequestParam Integer page,
                                       @RequestParam Integer pageSize) {
        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = vlogService.queryMyLikedList(userId, page, pageSize);
        return GraceJSONResult.ok(gridResult);
    }

    @GetMapping("followList")
    public GraceJSONResult followList(@RequestParam String myId,
                                      @RequestParam Integer page,
                                      @RequestParam Integer pageSize) {
        PagedGridResult gridResult = vlogService.getMyFollowVlogList(myId, page, pageSize);
        return GraceJSONResult.ok(gridResult);
    }


    @GetMapping("friendList")
    public GraceJSONResult friendList(@RequestParam String myId,
                                      @RequestParam Integer page,
                                      @RequestParam Integer pageSize) {

        if (page == null) {
            page = COMMON_START_PAGE;
        }
        if (pageSize == null) {
            pageSize = COMMON_PAGE_SIZE;
        }

        PagedGridResult gridResult = vlogService.getMyFriendVlogList(myId, page, pageSize);
        return GraceJSONResult.ok(gridResult);
    }






}

