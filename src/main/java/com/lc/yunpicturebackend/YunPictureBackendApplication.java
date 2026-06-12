package com.lc.yunpicturebackend;

import com.lc.yunpicturebackend.model.enums.UserRoleEnum;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;

@SpringBootApplication
@MapperScan("com.lc.yunpicturebackend.mapper")
@EnableAspectJAutoProxy( exposeProxy = true )
@EnableAsync
public class YunPictureBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(YunPictureBackendApplication.class, args);
	}
}
