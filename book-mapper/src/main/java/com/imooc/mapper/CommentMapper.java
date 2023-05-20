package com.imooc.mapper;

import com.imooc.pojo.Comment;
import com.imooc.my.mapper.MyMapper;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentMapper extends MyMapper<Comment> {
}