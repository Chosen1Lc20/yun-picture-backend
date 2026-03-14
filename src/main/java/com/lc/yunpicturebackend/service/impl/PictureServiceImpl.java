package com.lc.yunpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lc.yunpicturebackend.model.entity.Picture;
import com.lc.yunpicturebackend.service.PictureService;
import com.lc.yunpicturebackend.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
* @author lianchao0921
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-03-14 21:17:46
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

}




