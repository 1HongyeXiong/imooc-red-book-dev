package com.imooc.service.impl;

import com.github.pagehelper.PageHelper;
import com.imooc.base.BaseInfoProperties;
import com.imooc.base.RabbitMQConfig;
import com.imooc.bo.VlogBO;
import com.imooc.enums.MessageEnum;
import com.imooc.enums.YesOrNo;
import com.imooc.mapper.MyLikedVlogMapper;
import com.imooc.mapper.VlogMapper;
import com.imooc.mapper.VlogMapperCustom;
import com.imooc.mo.MessageMO;
import com.imooc.pojo.MyLikedVlog;
import com.imooc.pojo.Vlog;
import com.imooc.service.FansService;
import com.imooc.service.MsgService;
import com.imooc.service.VlogService;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import com.imooc.vo.IndexVlogVO;
import org.apache.commons.lang3.StringUtils;
import org.n3r.idworker.Sid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VlogServiceImpl extends BaseInfoProperties implements VlogService {

    @Autowired
    private VlogMapper vlogMapper;

    @Autowired
    public Sid sid;

    @Autowired
    public RabbitTemplate rabbitTemplate;

    @Transactional
    @Override
    public void createVlog(VlogBO vlogBO) {

        Vlog vlog = new Vlog();
        BeanUtils.copyProperties(vlogBO, vlog);

        String vid = sid.nextShort();
        vlog.setId(vid);
        vlog.setLikeCounts(0);
        vlog.setCommentsCounts(0);
        vlog.setIsPrivate(YesOrNo.NO.type);

        vlog.setCreatedTime(new Date());
        vlog.setUpdatedTime(new Date());

        vlogMapper.insert(vlog);
    }

    @Autowired
    private VlogMapperCustom vlogMapperCustom;

    @Autowired
    private FansService fansService;


    @Override
    public PagedGridResult queryIndexVlogList(String search, String userId, Integer page, Integer pageSize) {

/**
 * page: 第几页
 * pageSize: 每页显示条数
 */
        PageHelper.startPage(page, pageSize);

        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isNotBlank(search)) {
            map.put("search", search);
        }
        List<IndexVlogVO> list = vlogMapperCustom.getIndexVlogList(map);

        for (IndexVlogVO vo : list) {
            String vlogerId = vo.getVlogerId();
            String vlogId = vo.getVlogId();


            if (StringUtils.isNotBlank(userId)) {
                // 用户是否关注博主
                boolean doIFollowVloger = fansService.queryDoIFollowVloger(userId, vlogerId);
                vo.setDoIFollowVloger(doIFollowVloger);
                //判断当前用户是否点赞过视频
                vo.setDoILikeThisVlog(doILikeVlog(userId, vlogId));
            }
            //获得当前视频被点赞过的总数
            vo.setLikeCounts(Integer.valueOf(getVlogBeLikedCounts(vlogId)));
        }
        return setterPagedGrid(list, page);
//    return list;
    }
    // 用户是否点赞/喜欢当前视频，跟上一个方法有关系
    private boolean doILikeVlog(String myId, String vlogId) {
        String doILike = redis.get(REDIS_USER_LIKE_VLOG + ":" + myId + ":" + vlogId);
        boolean isLike = false;
        if (StringUtils.isNotBlank(doILike) && doILike.equalsIgnoreCase("1")) {
            isLike = true;
        }
        return isLike;
    }

    // 从redis中查询喜欢/点赞视频的总数，跟上上个方法有关系
    private Integer getVlogBeLikedCounts(String vlogId) {
        String countsStr = redis.get(REDIS_VLOG_BE_LIKED_COUNTS + ":" + vlogId);
        if (StringUtils.isBlank(countsStr)) {
            countsStr = "0";
        }
        return Integer.valueOf(countsStr);
    }




    @Override
    public IndexVlogVO getVlogDetail(String userId, String vlogId) {
        Map map = new HashMap();
        map.put("vlogId", vlogId);
        List<IndexVlogVO> list = vlogMapperCustom.getVlogDetailById(map);

        if (list != null && list.size() > 0 && !list.isEmpty()) {
            IndexVlogVO vlogDetail = list.get(0);
            return setterVO(vlogDetail, userId);
        }

        return null;
    }


    @Transactional
    @Override
    public void changeToPrivateOrPublic(String userId, String vlogId, Integer yesOrNo) {
        Example vlogExample = new Example(Vlog.class);
        Example.Criteria vlogCriteria = vlogExample.createCriteria();
        vlogCriteria.andEqualTo("id", vlogId);
        vlogCriteria.andEqualTo("vlogerId", userId);

        Vlog pendingPrivate = new Vlog();
        pendingPrivate.setIsPrivate(yesOrNo);

        vlogMapper.updateByExampleSelective(pendingPrivate, vlogExample);
    }


    @Override
    public PagedGridResult queryMyVlogList(String userId, Integer page, Integer pageSize, Integer yesOrNo) {

        Example vlogExample = new Example(Vlog.class);
        Example.Criteria vlogCriteria = vlogExample.createCriteria();
        vlogCriteria.andEqualTo("vlogerId", userId);
        vlogCriteria.andEqualTo("isPrivate", yesOrNo);

        PageHelper.startPage(page, pageSize);
        List<Vlog> list = vlogMapper.selectByExample(vlogExample);

        return setterPagedGrid(list, page);
    }



    @Autowired
    private MyLikedVlogMapper myLikedVlogMapper;
    @Autowired
    private MsgService msgService;


    @Transactional
    @Override
    public void flushCounts(String vlogId, Integer counts) {
        Vlog vlog = new Vlog();
        vlog.setId(vlogId);
        vlog.setLikeCounts(counts);
        vlogMapper.updateByPrimaryKeySelective(vlog);
    }


    @Transactional
    @Override
    public void userLikeVolg(String vlogId, String userId) {
        String rid = sid.nextShort();

        MyLikedVlog myLikedVlog = new MyLikedVlog();
        myLikedVlog.setId(rid);
        myLikedVlog.setUserId(userId);
        myLikedVlog.setVlogId(vlogId);

        myLikedVlogMapper.insert(myLikedVlog);

        Vlog vlog = this.getVlog(vlogId);
        Map msgContent = new HashMap();
        msgContent.put("vlogId", vlog.getId());
        msgContent.put("vlogCover", vlog.getCover());
        //msgService.createMsg(userId, vlog.getVlogerId(), MessageEnum.LIKE_VLOG.type ,msgContent);
        MessageMO messageMO = new MessageMO();
        messageMO.setFromUserId(userId);
        messageMO.setToUserId(vlog.getVlogerId());
        messageMO.setMsgContent(msgContent);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_MSG,
                "sys.msg." + MessageEnum.LIKE_VLOG.enValue,
                JsonUtils.objectToJson(messageMO));


    }

    @Override
    public Vlog getVlog(String id) {
        return vlogMapper.selectByPrimaryKey(id);
    }

    @Transactional
    @Override
    public void userUnLikeVolg(String vlogId, String userId) {
        MyLikedVlog myLikedVlog = new MyLikedVlog();
        myLikedVlog.setUserId(userId);
        myLikedVlog.setVlogId(vlogId);

        myLikedVlogMapper.delete(myLikedVlog);
    }



    @Override
    public PagedGridResult queryMyLikedList(String userId, Integer page, Integer pageSize) {

        PageHelper.startPage(page, pageSize);

        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        List<IndexVlogVO> list = vlogMapperCustom.getMyLikedVlogList(map);

        return setterPagedGrid(list, page);
    }


    @Override
    public PagedGridResult getMyFollowVlogList(String myId, Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);

        Map<String, Object> map = new HashMap<>();
        map.put("myId", myId);
        List<IndexVlogVO> list = vlogMapperCustom.getMyFollowVlogList(map);

        for (IndexVlogVO vo : list) {
            String vlogerId = vo.getVlogerId();
            String vlogId = vo.getVlogId();

            if (StringUtils.isNotBlank(myId)) {
                // 用户必定关注博主
                vo.setDoIFollowVloger(true);

                // 用户是否点赞/喜欢当前视频
                vo.setDoILikeThisVlog(doILikeVlog(myId, vlogId));
            }

            // 从redis中查询喜欢/点赞视频的总数
            vo.setLikeCounts(Integer.valueOf(getVlogBeLikedCounts(vlogId)));
        }

        return setterPagedGrid(list, page);
    }

    @Override
    public PagedGridResult getMyFriendVlogList(String myId, Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);

        Map<String, Object> map = new HashMap<>();
        map.put("myId", myId);
        List<IndexVlogVO> list = vlogMapperCustom.getMyFriendVlogList(map);

        for (IndexVlogVO vo : list) {
            String vlogerId = vo.getVlogerId();
            String vlogId = vo.getVlogId();

            if (StringUtils.isNotBlank(myId)) {
                // 用户必定关注博主
                vo.setDoIFollowVloger(true);

                // 用户是否点赞/喜欢当前视频
                vo.setDoILikeThisVlog(doILikeVlog(myId, vlogId));
            }

            // 从redis中查询喜欢/点赞视频的总数
            vo.setLikeCounts(Integer.valueOf(getVlogBeLikedCounts(vlogId)));
        }

        return setterPagedGrid(list, page);
    }

    private IndexVlogVO setterVO(IndexVlogVO v, String userId) {
        String vlogerId = v.getVlogerId();
        String vlogId = v.getVlogId();


        if (StringUtils.isNotBlank(userId)) {
            // 用户是否关注博主
            boolean doIFollowVloger = fansService.queryDoIFollowVloger(userId, vlogerId);
            v.setDoIFollowVloger(doIFollowVloger);
            //判断当前用户是否点赞过视频
            v.setDoILikeThisVlog(doILikeVlog(userId, vlogId));
        }
        //获得当前视频被点赞过的总数
        v.setLikeCounts(Integer.valueOf(getVlogBeLikedCounts(vlogId)));
        return v;

    }


}

