package com.imooc.service.impl;

import com.imooc.bo.UpdatedUsersBO;
import com.imooc.enums.Sex;
import com.imooc.enums.UserInfoModifyType;
import com.imooc.enums.YesOrNo;
import com.imooc.exceptions.GraceException;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.mapper.UsersMapper;
import com.imooc.pojo.Users;
import com.imooc.service.UserService;
import com.imooc.utils.DateUtil;
import com.imooc.utils.DesensitizationUtil;
import org.n3r.idworker.Sid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    public UsersMapper usersMapper;

    @Override
    public Users queryMobileIsExist(String mobile) {
//        查询一条实例
        Example userExample = new Example(Users.class);
        Example.Criteria userCriteria = userExample.createCriteria();
        userCriteria.andEqualTo("mobile", mobile);
        Users user = usersMapper.selectOneByExample(userExample);
        return user;
    }


    @Autowired
    public Sid sid;
    private static final String USER_FACE1 = "https://ichef.bbci.co.uk/news/976/cpsprodpb/15951/production/_117310488_16.jpg.webp";
    @Transactional
    @Override
    public Users createUser(String mobile) {
        /**
         * 互联网项目都要考虑可扩展性
         * 如果未来的业务激增，那么就需要分库分表
         * 那么数据库表主键id必须保证全局（全库）唯一，不得重复
         */
        String userId = sid.nextShort();

        Users user = new Users();

        user.setId(userId);
        user.setMobile(mobile);
        user.setNickname("用户：" + DesensitizationUtil.commonDisplay(mobile));
        user.setImoocNum("用户：" + DesensitizationUtil.commonDisplay(mobile));
        user.setFace(USER_FACE1);

        user.setBirthday(DateUtil.stringToDate("1900-01-01"));
        user.setSex(Sex.secret.type);

        user.setCountry("中国");
        user.setProvince("");
        user.setCity("");
        user.setDistrict("");
        user.setDescription("这家伙很懒，什么都没留下~");
        user.setCanImoocNumBeUpdated(YesOrNo.YES.type);

        user.setCreatedTime(new Date());
        user.setUpdatedTime(new Date());

        usersMapper.insert(user);

        return user;
    }

    //根据主键查询，比较简单
    @Override
    public Users getUser(String userId) {
        return usersMapper.selectByPrimaryKey(userId);
    }



    @Transactional
    @Override
    public Users updateUserInfo(UpdatedUsersBO updatedUsersBO) {
        Users pendingUser = new Users();
        BeanUtils.copyProperties(updatedUsersBO, pendingUser);

        int result = usersMapper.updateByPrimaryKeySelective(pendingUser);
        if (result != 1) {
            GraceException.display(ResponseStatusEnum.USER_UPDATE_ERROR);
        }

        return getUser(updatedUsersBO.getId());
    }


    @Transactional
    @Override
    public Users updateUserInfo(UpdatedUsersBO updatedUsersBO, Integer type) {

        Example example = new Example(Users.class);
        Example.Criteria criteria = example.createCriteria();

        // 如果是昵称，需要保证唯一
        if (type == UserInfoModifyType.NICKNAME.type) {
            criteria.andEqualTo("nickname", updatedUsersBO.getNickname());
            Users user = usersMapper.selectOneByExample(example);
            if (user != null) {
                GraceException.display(ResponseStatusEnum.USER_INFO_UPDATED_NICKNAME_EXIST_ERROR);
            }
        }

        // 如果是慕课号，需要保证唯一，并且只能修改一次
        if (type == UserInfoModifyType.IMOOCNUM.type) {
            criteria.andEqualTo("imoocNum", updatedUsersBO.getImoocNum());
            Users user = usersMapper.selectOneByExample(example);
            if (user != null) {
                GraceException.display(ResponseStatusEnum.USER_INFO_UPDATED_IMOOCNUM_EXIST_ERROR);
            }

            Users tempUser = getUser(updatedUsersBO.getId());
            if (tempUser.getCanImoocNumBeUpdated() == YesOrNo.NO.type) {
                GraceException.display(ResponseStatusEnum.USER_INFO_CANT_UPDATED_IMOOCNUM_ERROR);
            }

            updatedUsersBO.setCanImoocNumBeUpdated(YesOrNo.NO.type);
        }

        return updateUserInfo(updatedUsersBO);
    }




}
