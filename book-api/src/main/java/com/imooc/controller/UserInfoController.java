package com.imooc.controller;

import com.imooc.MinIOConfig;
import com.imooc.base.BaseInfoProperties;
import com.imooc.bo.UpdatedUsersBO;
import com.imooc.enums.FileTypeEnum;
import com.imooc.enums.UserInfoModifyType;
import com.imooc.grace.result.GraceJSONResult;
import com.imooc.grace.result.ResponseStatusEnum;

import com.imooc.pojo.Users;
import com.imooc.service.UserService;
import com.imooc.utils.MinIOUtils;
import com.imooc.utils.SMSUtils;
import com.imooc.vo.UsersVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "UserInfoController 用户信息接口模块")
@RestController
@RequestMapping("userInfo")
@Slf4j
public class UserInfoController extends BaseInfoProperties {

    @Autowired
    private UserService userService;

    @ApiOperation(value = "查询用户信息")
    @GetMapping("query")
    public GraceJSONResult query(@RequestParam String userId) throws Exception {
        Users userInfo = userService.getUser(userId);
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(userInfo, usersVO);

        // 我关注的博主总数量
        String myFollowsCountsStr = redis.get(REDIS_MY_FOLLOWS_COUNTS + ":" + userId);
        // 我的粉丝总数量
        String myFansCountsStr = redis.get(REDIS_MY_FANS_COUNTS + ":" + userId);
        // 用户获赞总数，视频博主（点赞/喜欢）总和
        //String likedVlogCountsStr = redis.get(REDIS_VLOG_BE_LIKED_COUNTS + ":" + userId);
        String likedVlogerCountsStr = redis.get(REDIS_VLOGER_BE_LIKED_COUNTS + ":" + userId);


        Integer myFollowsCounts = 0;
        Integer myFansCounts = 0;
        Integer likedVlogCounts = 0;
        Integer likedVlogerCounts = 0;
        Integer totalLikeMeCounts = 0;


        if (StringUtils.isNotBlank(myFollowsCountsStr)) {
            myFollowsCounts = Integer.valueOf(myFollowsCountsStr);
        }
        if (StringUtils.isNotBlank(myFansCountsStr)) {
            myFansCounts = Integer.valueOf(myFansCountsStr);
        }

//        if (StringUtils.isNotBlank(likedVlogCountsStr)) {
//            likedVlogCounts = Integer.valueOf(likedVlogCountsStr);
//        }

        if (StringUtils.isNotBlank(likedVlogerCountsStr)) {
            likedVlogerCounts = Integer.valueOf(likedVlogerCountsStr);
        }

        totalLikeMeCounts = likedVlogCounts + likedVlogerCounts;

        usersVO.setMyFollowsCounts(myFollowsCounts);
        usersVO.setMyFansCounts(myFansCounts);
        usersVO.setTotalLikeMeCounts(totalLikeMeCounts);

        return GraceJSONResult.ok(usersVO);
    }


    @ApiOperation(value = "修改用户信息")
    @PostMapping("modifyUserInfo")
    public GraceJSONResult modifyUserInfo(@RequestBody UpdatedUsersBO updatedUsersBO,
                                          @RequestParam Integer type) {

        UserInfoModifyType.checkUserInfoTypeIsRight(type);

        Users newUserInfo = userService.updateUserInfo(updatedUsersBO, type);

        // 返回最新用户信息传到前端，修改客户端缓存
        return GraceJSONResult.ok(newUserInfo);
    }

    @Autowired
    private MinIOConfig minIOConfig;
    @PostMapping("modifyImage")
    public GraceJSONResult modifyImage(@RequestParam String userId,
                                       @RequestParam Integer type,
                                       MultipartFile file) throws Exception {

        if (type != FileTypeEnum.BGIMG.type && type != FileTypeEnum.FACE.type) {
            return GraceJSONResult.errorCustom(ResponseStatusEnum.FILE_UPLOAD_FAILD);
        }

        String fileName = file.getOriginalFilename();

        MinIOUtils.uploadFile(minIOConfig.getBucketName(),
                fileName,
                file.getInputStream());

        String imgUrl = minIOConfig.getFileHost()
                + "/"
                + minIOConfig.getBucketName()
                + "/"
                + fileName;


        // 修改图片地址到数据库
        UpdatedUsersBO updatedUserBO = new UpdatedUsersBO();
        updatedUserBO.setId(userId);

        if (type == FileTypeEnum.BGIMG.type) {
            updatedUserBO.setBgImg(imgUrl);
        } else {
            updatedUserBO.setFace(imgUrl);
        }
        Users users = userService.updateUserInfo(updatedUserBO);

        return GraceJSONResult.ok(users);
    }
}

