package com.lc.yunpicturebackend.api.imagesearch;

import cn.hutool.core.util.ObjectUtil;
import com.lc.yunpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.lc.yunpicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.lc.yunpicturebackend.api.imagesearch.sub.GetImageListApi;
import com.lc.yunpicturebackend.api.imagesearch.sub.GetImagePageUrlApi;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;

import java.util.List;

public class ImageSearchApiFacade {
    public static List<ImageSearchResult> searchImages(String pictureUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(pictureUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(imageList), ErrorCode.OPERATION_ERROR,"以图搜图失败");
        return imageList;
    }

    public static void main(String[] args) {
        String url = "https://lcbucket-1411346346.cos.ap-beijing.myqcloud.com/public/2029839156843589633/2026-03-15-20-50-18_i3i1WOk6cMrFyVNd.jpg";
        List<ImageSearchResult> imageSearchResults = searchImages(url);
        System.out.println(imageSearchResults);
    }
}
