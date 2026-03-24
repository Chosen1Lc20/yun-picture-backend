package com.lc.yunpicturebackend.controller;

import com.lc.yunpicturebackend.annotation.AuthCheck;
import com.lc.yunpicturebackend.common.BaseResponse;
import com.lc.yunpicturebackend.common.ResultUtils;
import com.lc.yunpicturebackend.constant.UserConstant;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import com.lc.yunpicturebackend.manager.CosManager;
import com.lc.yunpicturebackend.model.entity.User;
import com.lc.yunpicturebackend.service.UserService;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Response;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@RestController("/file")
@Slf4j
@RequestMapping("file")
public class FileController {
    @Resource
    private CosManager cosManager;
    /**
     * 关键：
     * 1. consumes 指定 multipart/form-data（强制文件上传格式）
     * 2. @RequestPart 指定参数名（前后端必须一致）
     */
    @AuthCheck( mustRole = UserConstant.ADMIN_ROLE )
    @PostMapping(value = "/test/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<String> testUpload(@RequestPart MultipartFile multipartFile) {
        String originalFilename = multipartFile.getOriginalFilename();
        String filePath = String.format("/test4yuntuku/%s", originalFilename);
        File tempFile = null;
        //上传文件
        try {
            tempFile = File.createTempFile("/test4yuntuku", null);
            multipartFile.transferTo(tempFile);
            cosManager.putObject(filePath, tempFile);
            return ResultUtils.success(filePath);
        } catch (IOException e) {
            log.error("file upload error filePath:{} ,{}", filePath, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"文件上传失败");
        } finally {
            //删除临时文件
            if(tempFile != null) {
                boolean result = tempFile.delete();
                if(!result) {
                    log.error("file delete error filePath:{}", filePath);
                }
            }
        }
    }

    @GetMapping("/test/download")
    @AuthCheck( mustRole = UserConstant.ADMIN_ROLE )
    public void testDownload(String filepath ,HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectIps = null;
        try {
            COSObject object = cosManager.getObject(filepath);
            cosObjectIps = object.getObjectContent();
            byte[] byteArray = IOUtils.toByteArray(cosObjectIps);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            //写入响应
            response.getOutputStream().write(byteArray);
            response.getOutputStream().flush();

        } catch (IOException e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectIps != null) {
                cosObjectIps.close();
            }
        }
    }
}

