package com.lc.yunpicturebackend.api.imagesearch.model;

import lombok.Data;

@Data
public class ImageSearchResult {

    /**
     * 搜索结果缩略图地址
     */
    private String thumbUrl;

    /**
     * 相似图片来源网页
     */
    private String fromUrl;

    /**
     * 百度识图的相似图详情页接口地址，点击这张图会跳转到这个链接，继续搜索该图的更多相似内容
     */
    private String objUrl;
}
