package com.lc.yunpicturebackend.config;

import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 请求包装过滤器
 *
 * @author pine
 */
@Order(1)
@Component
public class HttpRequestWrapperFilter implements Filter {

    //重写 doFilter（请求拦截 / 放行的逻辑）
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        // 判断：是否是 HTTP 请求（排除 WebSocket/异步非HTTP请求）
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            // 获取请求头：Content-Type（标识请求体格式：JSON/表单/文件等）
            String contentType = httpServletRequest.getHeader(Header.CONTENT_TYPE.getValue());
            // JSON 请求：用 RequestWrapper 包装，解决流只能读一次问题
            if (ContentType.JSON.getValue().equals(contentType)) {
                // 可以再细粒度一些，只有需要进行空间权限校验的接口才需要包一层
                chain.doFilter(new RequestWrapper(httpServletRequest), response);
            } else {
                chain.doFilter(request, response);
            }
        }
    }

}
