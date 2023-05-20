package com.imooc.controller;

import com.imooc.base.BaseInfoProperties;
import com.imooc.base.RabbitMQConfig;
import com.imooc.bo.CommentBO;
import com.imooc.enums.MessageEnum;
import com.imooc.exceptions.GraceException;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.mapper.CommentMapper;
import com.imooc.mo.MessageMO;
import com.imooc.pojo.Comment;
import com.imooc.pojo.Users;
import com.imooc.pojo.Vlog;
import com.imooc.service.CommentService;
import com.imooc.service.MsgService;
import com.imooc.service.UserService;
import com.imooc.service.VlogService;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import com.imooc.vo.CommentVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;




@Slf4j
@Api(tags = "Comment 接口")
@RestController
@RequestMapping("comment")
public class CommentController extends BaseInfoProperties {
    @Autowired
    private CommentService commentService;
    @Autowired
    private UserService userService;

    @PostMapping("create")
    public GraceJSONResult create(@RequestBody @Valid CommentBO commentBO){
        String fatherCommentId = commentBO.getFatherCommentId();
        if(!fatherCommentId.equalsIgnoreCase("0")){
            // 校验fatherComment是否存在
            return GraceJSONResult.errorCustom(ResponseStatusEnum.ARTICLE_CREATE_ERROR);
        }
        // 校验vloger是否存在,校验commentUser是否存在
        String vlogerId = commentBO.getVlogerId();
        String commentUserId = commentBO.getCommentUserId();
        if(!checkUserExist(vlogerId) || !checkUserExist(commentUserId)){
            return GraceJSONResult.errorCustom(ResponseStatusEnum.USER_NOT_EXIST_ERROR);
        }

        CommentVO commentVO = commentService.createComment(commentBO);
        return GraceJSONResult.ok(commentVO);
    }

    private boolean checkUserExist(String userId){
        Users user = userService.getUser(userId);
        return user != null;
    }



    @ApiOperation(value = "短视频的评论总数")
    @GetMapping("counts")
    public GraceJSONResult counts(@RequestParam String vlogId) {

        String countsStr = redis.get(REDIS_VLOG_COMMENT_COUNTS + ":" + vlogId);
        if (StringUtils.isBlank(countsStr)) {
            countsStr = "0";
        }

        return GraceJSONResult.ok(Integer.valueOf(countsStr));
    }

    @GetMapping("list")
    public GraceJSONResult list(@RequestParam String vlogId,
                                @RequestParam(defaultValue = "") String userId,
                                @RequestParam Integer page,
                                @RequestParam Integer pageSize) {

        PagedGridResult gridResult = commentService.queryVlogComments(vlogId, userId, page, pageSize);
        return GraceJSONResult.ok(gridResult);
    }


    @DeleteMapping("delete")
    public GraceJSONResult delete(@RequestParam String commentUserId,
                                  @RequestParam String commentId,
                                  @RequestParam String vlogId) {
        commentService.deleteComment(commentUserId, commentId, vlogId);
        return GraceJSONResult.ok();
    }


    @Autowired
    private MsgService msgService;
    @Autowired
    private VlogService vlogService;

    @Autowired
    public RabbitTemplate rabbitTemplate;

    @ApiOperation(value = "点赞评论")
    @PostMapping("like")
    public GraceJSONResult like( @RequestParam String commentId, @RequestParam String userId) {

//        故意犯错 bigkey
        redis.increment(REDIS_VLOG_COMMENT_LIKED_COUNTS+":"+commentId, 1);
        //redis.set(REDIS_USER_LIKE_COMMENT+":"+userId + ":" + commentId, "1");
        redis.hset(REDIS_USER_LIKE_COMMENT, userId + ":" + commentId,"1");


        Comment comment = commentService.getComment(commentId);
        Vlog vlog = vlogService.getVlog(comment.getVlogId());
        Map msgContent = new HashMap();
        msgContent.put("vlogId", vlog.getId());
        msgContent.put("vlogCover", vlog.getCover());
        msgContent.put("commentId", commentId);
//        msgService.createMsg(userId,
//                comment.getCommentUserId(),
//                MessageEnum.LIKE_COMMENT.type,
//                msgContent);
        // MQ异步解耦
        MessageMO messageMO = new MessageMO();
        messageMO.setFromUserId(userId);
        messageMO.setToUserId(comment.getCommentUserId());
        messageMO.setMsgContent(msgContent);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_MSG,
                "sys.msg." + MessageEnum.LIKE_COMMENT.enValue,
                JsonUtils.objectToJson(messageMO));


        return GraceJSONResult.ok();
    }

    @ApiOperation(value = "取消点赞评论")
    @PostMapping("unlike")
    public GraceJSONResult unlike(@RequestParam String userId, @RequestParam String commentId) {

        redis.decrement(REDIS_VLOG_COMMENT_LIKED_COUNTS+":"+commentId, 1);
        redis.hdel(REDIS_USER_LIKE_COMMENT, userId + ":" + commentId);

        return GraceJSONResult.ok();
    }

}

