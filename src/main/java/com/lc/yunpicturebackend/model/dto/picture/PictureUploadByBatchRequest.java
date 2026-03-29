package com.lc.yunpicturebackend.model.dto.picture;

import lombok.Data;

/**
 * 示例请求接口地址 https://cn.bing.com/images/async?q=%E4%B8%96%E7%95%8C%E6%97%85%E6%B8%B8%E8%83%9C%E5%9C%B0&mmasync=1
 */
@Data
public class PictureUploadByBatchRequest {
    /**
     * 搜索词
     */
    private String searchText;
    /**
     * 抓取数量
     */
    private int searchCount;
    /**
     * 名称前缀
     */
    private String namePrefix;
}
