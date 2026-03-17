package com.lc.yunpicturebackend.manager;

import com.lc.yunpicturebackend.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 提供通用的文件上传和文件下载操作
 */
@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 将本地文件上传到 COS
     * @param key 图片唯一key
     * @param file 文件对象
     * @return PutObjectResult
     * @throws CosClientException
     * @throws CosServiceException
     */
    public PutObjectResult putObject(String key, File file)
            throws CosClientException, CosServiceException{
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 从COS中获取文件
     * @param key 图片唯一key
     * @return 图片COSObject
     * @throws CosClientException
     * @throws CosServiceException
     */
    public COSObject getObject(String key)
            throws CosClientException, CosServiceException{
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        // 构造处理参数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传对象 (根据流方式上传,不生成本地临时文件)
     * @param multipartFile 图片
     * @param key 唯一键
     * @return
     */
    public String uploadFileToCOS(MultipartFile multipartFile,String key) throws IOException {
        InputStream multipartFileIns = null;
        try {
            multipartFileIns = multipartFile.getInputStream();
            ObjectMetadata objectMetadata = new ObjectMetadata();
            //设置元数据
            objectMetadata.setContentType(multipartFile.getContentType());
            objectMetadata.setContentLength(multipartFile.getSize());
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(),key,multipartFileIns,objectMetadata);

            PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
            //示例链接
            //https://lcbucket-1411346346.cos.ap-beijing.myqcloud.com/public/
            //                                 2029839156843589633/2026-03-15-20-49-55_8NoNzRGN4ISLN5pA.jpg
            //生成访问链接
            return "https://"+cosClientConfig.getBucket()+".cos."+cosClientConfig.getRegion()+".myqclound.com/"+
                            key;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            //用了流最后一定要关闭
            if(multipartFileIns != null){
                multipartFileIns.close();
            }
        }
    }
}
