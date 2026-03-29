package com.lc.yunpicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.math.MathUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.lc.yunpicturebackend.config.CosClientConfig;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.model.enums.PictureFormatEnum;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
@Deprecated
@Service
@Slf4j
public class FileManager {  
  
    @Resource
    private CosClientConfig cosClientConfig;
  
    @Resource  
    private CosManager cosManager;

    /**
     * 上传图片 (生成本地文件) 已废弃,改为upload包下的模板方法优化
     * @param multipartFile
     * @param uploadPathPrefix
     * @return
     */
    @Deprecated
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        //文件校验,无效则会抛出异常
        validPicture(multipartFile);
        //格式化时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String formatDatetime = LocalDateTime.now().format(formatter);
        //拼接图片上传地址
        String UUId = RandomUtil.randomString(16);
        //multipartFile.getOriginalFilename(): 带后缀的文件名
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        String uploadFileName = String.format("%s_%s.%s", formatDatetime, UUId, fileSuffix);
        String finalUploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File tempFile = null;
        try {
            //上传文件
            //InputStream inputStream = multipartFile.getInputStream();
            tempFile = File.createTempFile("temp123",".tmp");
            multipartFile.transferTo(tempFile);
            //腾讯云 COS 对象存储上传文件成功后，返回的上传结果回执对象
            PutObjectResult putObjectResult = cosManager.putPictureObject(finalUploadPath, tempFile);
            //图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //返回的format格式是大写的!!!
            String format = imageInfo.getFormat();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setPicFormat(format);
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setUrl(cosClientConfig.getHost()+finalUploadPath);
            uploadPictureResult.setPicSize(FileUtil.size(tempFile));
            uploadPictureResult.setPicName(FileUtil.mainName(multipartFile.getOriginalFilename()));

            return uploadPictureResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            //删除临时文件
            deleteTempFile(tempFile);
        }
    }
    /**
     * 上传图片 通过url进行上传 已废弃,改为upload包下的模板方法优化
     * @param fileUrl
     * @param uploadPathPrefix
     * @return
     */
    @Deprecated
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        //文件校验,无效则会抛出异常
        //todo
        validPictureByUrl(fileUrl);
        //格式化时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String formatDatetime = LocalDateTime.now().format(formatter);
        //拼接图片上传地址
        String UUId = RandomUtil.randomString(16);
        String originalFileName = FileUtil.mainName(fileUrl)+"."+FileUtil.extName(fileUrl);
        //图片的后缀名
        String uploadFileName = String.format("%s_%s.%s", formatDatetime, UUId, FileUtil.getSuffix(originalFileName));
        String finalUploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File tempFile = null;
        try {
            //todo
            //示例url:https://lcbucket-1411346346.cos.ap-beijing.myqcloud.com/public/2029839156843589633/2026-03-22-14-45-43_OXu7OevucB0WENq3.jpg
            tempFile = File.createTempFile("temp123ByUrl", ".tmp");

            HttpUtil.downloadFile(fileUrl,tempFile);
            //腾讯云 COS 对象存储上传文件成功后，返回的上传结果回执对象
            PutObjectResult putObjectResult = cosManager.putPictureObject(finalUploadPath, tempFile);
            //图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //返回的format格式是大写的!!!
            String format = imageInfo.getFormat();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setPicFormat(format);
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setUrl(cosClientConfig.getHost()+finalUploadPath);
            uploadPictureResult.setPicSize(FileUtil.size(tempFile));
            //todo
            uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));

            return uploadPictureResult;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            //删除临时文件
            deleteTempFile(tempFile);
        }
    }

    /**
     * 上传文件 (通过流传输)
     * @param multipartFile
     * @param uploadPathPrefix
     * @return
     */
    public String uploadFileToCOS(MultipartFile multipartFile,String uploadPathPrefix) {
        //文件校验,无效则会抛出异常
        validPicture(multipartFile);
        //格式化时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String formatDatetime = LocalDateTime.now().format(formatter);
        //拼接图片上传地址
        String UUId = RandomUtil.randomString(16);
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        String uploadFileName = String.format("%s_%s.%s", formatDatetime, UUId, fileSuffix);
        String finalUploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        String pictureUrl = null;
        try {
            pictureUrl = cosManager.uploadFileToCOS(multipartFile, finalUploadPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return pictureUrl;
    }

    public void validPicture(MultipartFile file) {
        //1.校验大小 字节为单位
        long fileSize = file.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(fileSize>2*ONE_MB,new BusinessException(ErrorCode.PARAMS_ERROR,"图片大小不能超过2MB"));
        //2.校验文件后缀名
        //todo 使用枚举类
        PictureFormatEnum[] values = PictureFormatEnum.values();
        List<String> ALLOW_FORMAT_LIST = Arrays.stream(values).map(PictureFormatEnum::getFormat).toList();
        String fileSuffix = FileUtil.getSuffix(file.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),new BusinessException(ErrorCode.PARAMS_ERROR,"文件类型错误"));
    }
    public void validPictureByUrl(String fileUrl) {
        ThrowUtils.throwIf(StringUtils.isBlank(fileUrl),new BusinessException(ErrorCode.PARAMS_ERROR,"传递的url为空"));
        //校验url格式是否合法
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"传递的url格式不正确");
        }
        //校验协议,目前仅支持http和https
        if(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://")){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不支持http和https以外的协议进行图片上传");
        }
        //校验response是否正常返回
        HttpRequest requestUsingHead= null ;
        HttpResponse response = null;
        requestUsingHead = HttpUtil.createRequest(Method.HEAD, fileUrl);
        String fileFormat;
        try {
            response = requestUsingHead.execute();
            if (response.getStatus() != HttpStatus.SC_OK) {
                return;
            }

            if (!response.isOk()) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法获取到指定url的图片");
            }
            //校验大小
            final long ONE_MB = 1024 * 1024;
            if (response.contentLength() > 2 * ONE_MB) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片体积不能超过2MB");
            }
            //校验文件格式
            fileFormat = response.header("Content-Type");
            final List<String> ALLOW_FORMAT_LIST = Arrays.asList("image/png", "image/jpeg", "image/webp", "image/jpg");
            if(StringUtils.isBlank(fileFormat)){
                if(!ALLOW_FORMAT_LIST.contains(fileFormat)){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR,"仅支持png,jpeg,webp,jpg格式的上传");
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,e.getMessage());
        } finally {
            if(response != null){
                response.close();
            }
        }
    }

    public void deleteTempFile(File file) {
        if(file==null){
            return;
        }
        boolean deleteResult = file.delete();
        ThrowUtils.throwIf(!deleteResult,new BusinessException(ErrorCode.OPERATION_ERROR,"删除临时文件失败"));
    }
}
