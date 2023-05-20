package com.imooc.service.impl;

import com.github.pagehelper.PageHelper;
import com.imooc.base.BaseInfoProperties;
import com.imooc.base.RabbitMQConfig;
import com.imooc.enums.MessageEnum;
import com.imooc.enums.YesOrNo;
import com.imooc.mapper.FansMapper;
import com.imooc.mapper.FansMapperCustom;
import com.imooc.mo.MessageMO;
import com.imooc.pojo.Fans;
import com.imooc.service.FansService;
import com.imooc.service.MsgService;
import com.imooc.utils.JsonUtils;
import com.imooc.utils.PagedGridResult;
import com.imooc.vo.FansVO;
import com.imooc.vo.VlogerVO;
import org.apache.commons.lang3.StringUtils;
import org.n3r.idworker.Sid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FansServiceImpl extends BaseInfoProperties implements FansService {


    @Autowired
    private MsgService msgService;
    @Autowired
    private FansMapper fansMapper;
    @Autowired
    private Sid sid;

    @Autowired
    public RabbitTemplate rabbitTemplate;

    // 判断对方是否关注我
    public Fans queryFansRelationship(String fanId, String vlogerId) {
        Example example = new Example(Fans.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("fanId", fanId);
        criteria.andEqualTo("vlogerId", vlogerId);
        List list = fansMapper.selectByExample(example);
        Fans fan = null;
        if (list != null && !list.isEmpty() && list.size() > 0) {
            fan = (Fans)list.get(0);
        }
        return fan;
    }

    @Transactional
    @Override
    public void doFollow(String myId, String vlogerId) {

        String fid = sid.nextShort();

        Fans fans = new Fans();
        fans.setId(fid);
        fans.setFanId(myId);
        fans.setVlogerId(vlogerId);

        // 判断对方是否关注我，如果关注我，那么双方都要互为朋友关系
        Fans vloger = queryFansRelationship(vlogerId, myId);

        if (vloger != null) {
            fans.setIsFanFriendOfMine(YesOrNo.YES.type);

            // 把对方的粉丝关系也改成朋友关系
            vloger.setIsFanFriendOfMine(YesOrNo.YES.type);
            fansMapper.updateByPrimaryKeySelective(vloger);
        } else {
            fans.setIsFanFriendOfMine(YesOrNo.NO.type);
        }

        fansMapper.insert(fans);
        //msgService.createMsg(myId, vlogerId, MessageEnum.FOLLOW_YOU.type, null);
        MessageMO messageMO = new MessageMO();
        messageMO.setFromUserId(myId);
        messageMO.setToUserId(vlogerId);
// 优化：使用mq异步解耦
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_MSG,
                "sys.msg." + MessageEnum.FOLLOW_YOU.enValue,
                JsonUtils.objectToJson(messageMO));

    }

    @Transactional
    @Override
    public void doCancel(String myId, String vlogerId) {
        // 判断是否朋友关系，如果是，则需要取消双方的关系
        Fans fan = queryFansRelationship(myId, vlogerId);
        if (fan != null && fan.getIsFanFriendOfMine() == 1) {
            // 抹除对方关联表的朋友关系，自己的关系删了就行
            Fans pendingFan =  queryFansRelationship(vlogerId, myId);
            pendingFan.setIsFanFriendOfMine(YesOrNo.NO.type);
            fansMapper.updateByPrimaryKeySelective(pendingFan);
        }

        // 删除自己的关注关联关系
        fansMapper.delete(fan);

    }

    @Override
    public boolean queryDoIFollowVloger(String myId, String vlogerId) {
        Fans fan = queryFansRelationship(myId, vlogerId);
        return fan != null;
    }

    @Autowired
    private FansMapperCustom fansMapperCustom;
    @Override
    public PagedGridResult queryMyFollows(String myId, Integer page, Integer pageSize) {

        Map<String, Object> map = new HashMap<>();
        map.put("myId", myId);

        PageHelper.startPage(page, pageSize);
        List<VlogerVO> list = fansMapperCustom.queryMyFollows(map);

        return setterPagedGrid(list, page);
    }


    @Override
    public PagedGridResult queryMyFans(String myId, Integer page, Integer pageSize) {



        Map<String, Object> map = new HashMap<>();
        map.put("myId", myId);

        PageHelper.startPage(page, pageSize);
        List<FansVO> list = fansMapperCustom.queryMyFans(map);


        /**
         * <判断用户是否是我朋友（互关互粉）>
         * 普通做法：
         * 多表关联+嵌套关联查询，这样违反多表关联的规范，不可取，高并发会出现性能问题
         *
         * 常规做法：
         * 1. 避免过多的表关联查询，先查询我的粉丝，获得fansList
         * 2. 判断粉丝关注我，并且我也关注粉丝 -> 循环fansList，获得每一个粉丝，再循环数据库查询我是否关注他
         * 3. 如果我也关注粉丝，说明我俩互为朋友关系（互关/互粉），则标记flag
         *
         * 高级做法：
         * 1. 关注/取关的时候，关联关系保存在redis中，不要依赖数据库
         * 2. 数据库查询后，直接循环查询redis即可。避免第二次循环数据库查询造成的性能瓶颈。
         */

        for (FansVO f : list) {
            String relationShip = redis.get(REDIS_FANS_AND_VLOGGER_RELATIONSHIP + ":" + myId + ":" + f.getFanId());
            if (StringUtils.isNotBlank(relationShip) && relationShip.equalsIgnoreCase("1")) {
                f.setFriend(true);
            }
        }


        return setterPagedGrid(list, page);
    }






}
