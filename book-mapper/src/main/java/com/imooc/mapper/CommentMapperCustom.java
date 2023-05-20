package com.imooc.mapper;

import com.imooc.vo.CommentVO;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface CommentMapperCustom {

    public List<CommentVO> getCommentList(@Param("paramMap") Map<String, Object> map);

}

