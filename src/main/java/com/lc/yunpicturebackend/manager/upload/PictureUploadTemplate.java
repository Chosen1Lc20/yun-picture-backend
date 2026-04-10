package com.lc.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.lc.yunpicturebackend.config.CosClientConfig;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.manager.CosManager;
import com.lc.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.lc.yunpicturebackend.model.entity.Picture;
import com.lc.yunpicturebackend.utils.picture.ColorTransformUtils;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 使用了模板设计模式
 */
@Slf4j
public abstract class PictureUploadTemplate<T> {
    @Resource
    private CosManager cosManager;
    @Resource
    private CosClientConfig cosClientConfig;

    public UploadPictureResult uploadPicture(T t, String uploadPathPrefix) {
        //1.校验文件
        validPicture(t);
        //格式化时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        String formatDatetime = LocalDateTime.now().format(formatter);
        //拼接图片上传地址
        String originalFileName = getOriginalFileName(t);
        String UUId = RandomUtil.randomString(16);
        //文件后缀名
        String fileSuffix = FileUtil.getSuffix(originalFileName);
        //文件类型
        String fileType = getFileType(t);

        String uploadFileName = String.format("%s_%s.%s", formatDatetime, UUId, fileSuffix);
        //uploadPathPrefix示例: public/2029839156843589633
        String finalUploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
        File tempFile = null;
        try {
            //2.创建临时文件
            tempFile = File.createTempFile("temp123",".tmp");
            //3.处理临时文件
            this.processPicture(t, tempFile);
            //4.腾讯云 COS 对象存储上传文件成功后，返回的上传结果回执对象
            PutObjectResult putObjectResult = cosManager.putPictureObject(finalUploadPath, tempFile);
            //原图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //压缩图片信息
            List<CIObject> ciObjects = putObjectResult.getCiUploadResult().getProcessResults().getObjectList();
            if(!ciObjects.isEmpty()){
                CIObject compressedPic = ciObjects.get(0);
                //缩略图默认为压缩图
                CIObject thumbnailPic = compressedPic;
                if(ciObjects.size()>1){
                    //说明有缩略图
                    thumbnailPic = ciObjects.get(1);
                }
                return buildResult(originalFileName, compressedPic, thumbnailPic, imageInfo);
            }
            //5.返回封装结果
            return this.buildResult(originalFileName,finalUploadPath,imageInfo,tempFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            //6.删除临时文件
            deleteTempFile(tempFile);
        }
    }

    /**
     * 校验图片是否有效
     * @param t
     * @return
     */
    abstract void validPicture(T t);

    /**
     * 将 t(multipartFile或 url) 指示的文件输出到临时文件file中
     * @param t
     * @param file
     */
    abstract void processPicture(T t, File file);

    /**
     * 获取t(multipartFile或 url) 指示的文件的原始名字,带扩展名
     * @param t
     * @return
     */
    abstract String getOriginalFileName(T t);

    abstract String getFileType(T t);

    public UploadPictureResult buildResult(String originalFileName, String finalUploadPath,
                                           ImageInfo imageInfo , File tempFile) {
        String format = imageInfo.getFormat();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        //图片主色调
        String picAve = imageInfo.getAve();
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setPicFormat(format);
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setUrl(cosClientConfig.getHost()+finalUploadPath);
        uploadPictureResult.setPicSize(FileUtil.size(tempFile));
        uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));
        //attention 由于cos存储的ave格式不是标准的十六进制格式(会去掉前导0),所以在这进行标准化。方便前端展示
        String standardColor = ColorTransformUtils.getStandardColor(picAve);
        uploadPictureResult.setPicColor(standardColor);

        return uploadPictureResult;
    }
    public UploadPictureResult buildResult(String originalFileName, CIObject compressedCiObj, CIObject thumbnailCiObj,
                                           ImageInfo imageInfo) {
        String key = compressedCiObj.getKey();
        String format = compressedCiObj.getFormat();
        Integer picHeight = compressedCiObj.getHeight();
        Integer picWidth = compressedCiObj.getWidth();
        Integer picSize = compressedCiObj.getSize();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        //图片主色调
        String picAve = imageInfo.getAve();
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setPicFormat(format);
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicSize(picSize.longValue());
        uploadPictureResult.setUrl(cosClientConfig.getHost()+"/"+key);
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost()+"/"+thumbnailCiObj.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));
        String standardColor = ColorTransformUtils.getStandardColor(picAve);
        uploadPictureResult.setPicColor(standardColor);
        return uploadPictureResult;
    }


    public void deleteTempFile(File file) {
        if(file==null){
            return;
        }
        boolean deleteResult = file.delete();
        ThrowUtils.throwIf(!deleteResult,new BusinessException(ErrorCode.OPERATION_ERROR,"删除临时文件失败"));
    }

    /**
     * 字节数组转十六进制字符串
     */
    protected String bytesToHex(byte[] bytes) {
        // 1. 创建字符串拼接器（高效拼接字符串，首选StringBuilder）
        StringBuilder sb = new StringBuilder();

        // 2. 遍历字节数组的每一个字节（我们传的是文件前4个字节）
        for (byte b : bytes) {
            // 3. 【核心！】把有符号byte → 转成无符号整数，再转十六进制字符串
            String hex = Integer.toHexString(b & 0xFF);

            // 4. 补0：保证每个字节对应【两位十六进制】
            if (hex.length() == 1) {
                sb.append("0");
            }

            // 5. 把转换好的十六进制字符拼接到结果里
            sb.append(hex);
        }

        // 6. 转大写返回（魔数标准格式是大写）
        return sb.toString().toUpperCase();
    }
    /**
     * 根据魔数 获取文件真实类型 魔数匹配优先使用startsWith()而非全量匹配，兼容不同长度魔数
     * @param inputStream 文件输入流（URL流/上传文件流）
     * @return 文件类型（JPG/PNG/GIF/BMP/WEBP），未知返回 UNKNOWN
     */
    public String getFileTypeByMagicNumber(InputStream inputStream) {
        try {
            byte[] bytes = new byte[4];
            // 读取文件前4个字节（魔数核心区域）
            inputStream.read(bytes);
            // 复用你已有的方法：转十六进制
            String magicNumber = bytesToHex(bytes);

            // 根据魔数匹配文件类型（精准匹配）
            if (magicNumber.startsWith("FFD8FF")) {
                return "JPG";
            } else if (magicNumber.startsWith("89504E47")) {
                return "PNG";
            } else if (magicNumber.startsWith("47494638")) {
                return "GIF";
            } else if (magicNumber.startsWith("424D")) {
                return "BMP";
            } else if (magicNumber.startsWith("52494646")) {
                return "WEBP";
            } else {
                return "UNKNOWN";
            }
        } catch (Exception e) {
            // 流异常/文件损坏 → 未知类型
            return "UNKNOWN";
        }
    }
}
