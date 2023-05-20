package com.imooc.service;

import com.imooc.bo.UpdatedUsersBO;
import com.imooc.pojo.Users;


public interface UserService {

    /**
     * 判断用户是否存在，如果存在返回user信息
     */
    public Users queryMobileIsExist(String mobile);


    /**
     * 创建用户，新增用户记录到数据库
     */
    public Users createUser(String mobile);


    /**
     * 根据用户主键id查询用户信息
     */
    public Users getUser(String userId);


    /**
     * 用户修改信息
     */
    public Users updateUserInfo(UpdatedUsersBO updatedUsersBO);

    /**
     * 用户修改信息
     */
    public Users updateUserInfo(UpdatedUsersBO updatedUsersBO, Integer type);



}
