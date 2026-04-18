package com.lc.yunpicturebackend.api.aliyunai;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.lc.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.lc.yunpicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.lc.yunpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.lc.yunpicturebackend.exception.BusinessException;
import com.lc.yunpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 开发Api调用类,利用hutool工具包的http请求类来调用阿里云百炼的api
 */

//创建任务获取任务Id
//POST https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting

//查询任务结果
//GET https://dashscope.aliyuncs.com/api/v1/tasks/%s
@Slf4j
@Component
public class AliYunAiApi {
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    //创建任务地址
    private String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    //查询任务状态
    private String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建AI扩图任务
     *
     * @param request
     * @return
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        HttpRequest httpRequest = new HttpRequest(CREATE_OUT_PAINTING_TASK_URL);
        httpRequest.header("Content-Type", "application/json").
                header("Authorization", "Bearer " + apiKey).
                //必须开启异步处理,设置为enable
                        header("X-DashScope-Async", "enable").
                body(JSONUtil.toJsonStr(request));
        CreateOutPaintingTaskResponse response = null;
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "调用AI扩图请求失败");
            }
            response = JSONUtil.toBean(httpResponse.body(),
                    CreateOutPaintingTaskResponse.class);
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                String message = response.getMessage();
                log.info("响应异常:{}", message);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI接口响应异常");
            }
        } catch (Exception e) {
            log.error("发生异常", e);
        }
        return response;
    }

    /**
     * 查询创建的任务
     * @param getOutPaintingTaskResponse1
     * @return
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(GetOutPaintingTaskResponse getOutPaintingTaskResponse1) {
        //AI扩图的任务Id
        String taskId = getOutPaintingTaskResponse1.getOutput().getTaskId();
        HttpRequest httpRequest = HttpRequest.get(String.format(GET_OUT_PAINTING_TASK_URL, taskId))
                .header("Authorization", "Bearer " + apiKey);
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求失败");
            }
            return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
        }
    }
}
