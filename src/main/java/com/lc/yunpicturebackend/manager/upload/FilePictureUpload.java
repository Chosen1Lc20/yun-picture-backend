package com.lc.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.model.enums.PictureFormatEnum;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Component
public class FilePictureUpload extends PictureUploadTemplate<MultipartFile>{
    @Override
    void validPicture(MultipartFile file) {
        //1.校验大小 字节为单位
        long fileSize = file.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(fileSize>5*ONE_MB,new BusinessException(ErrorCode.PARAMS_ERROR,"图片大小不能超过5MB"));
        //2.校验文件后缀名
        //todo 使用枚举类
        PictureFormatEnum[] values = PictureFormatEnum.values();
        List<String> ALLOW_FORMAT_LIST = Arrays.stream(values).map(PictureFormatEnum::getFormat).toList();
        String fileSuffix = FileUtil.getSuffix(file.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix),new BusinessException(ErrorCode.PARAMS_ERROR,"文件类型错误"));
    }

    @Override
    void processPicture(MultipartFile multipartFile, File file) {
        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,e.getMessage());
        }
    }

    @Override
    String getOriginalFileName(MultipartFile multipartFile) {
        return multipartFile.getOriginalFilename();
    }

    /**
     * 根据上传文件获取真实类型
     */
    @Override
    public String getFileType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return getFileTypeByMagicNumber(inputStream);
        } catch (IOException e) {
            return "UNKNOWN";
        }
    }
}
