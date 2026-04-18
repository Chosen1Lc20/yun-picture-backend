package com.lc.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import com.lc.yunpicturebackend.exception.ThrowUtils;
import org.apache.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Component
public class UrlPictureUpload extends PictureUploadTemplate<String> {
    @Override
    void validPicture(String fileUrl) {
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
            if (response.contentLength() > 5 * ONE_MB) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "上传图片体积不能超过5MB");
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

    @Override
    void processPicture(String fileUrl, File tempFile) {
        HttpUtil.downloadFile(fileUrl,tempFile);
    }

    @Override
    String getOriginalFileName(String fileUrl) {
        //mainName是 图片名字 extName是扩展名
        /**
         * 获取文件类型
         * "https://picsum.photos/200" 从这个网站的url没有后缀名,通过文件本身的魔数可以得到后缀名
         */
        String fileType = getFileType(fileUrl);
        if(fileType.equals("UNKNOWN")){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不支持当前传递的图片格式");
        }
        return FileUtil.mainName(fileUrl)+"."+fileType;
    }
    /**
     * 根据URL获取图片真实类型
     */
    public String getFileType(String fileUrl) {
        try (InputStream inputStream = new URL(fileUrl).openStream()) {
            return getFileTypeByMagicNumber(inputStream);
        } catch (IOException e) {
            return "UNKNOWN";
        }
    }
}