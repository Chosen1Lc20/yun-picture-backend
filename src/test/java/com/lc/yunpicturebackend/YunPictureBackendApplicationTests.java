package com.lc.yunpicturebackend;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
class YunPictureBackendApplicationTests {
	@Resource
	private RedisTemplate<String, Object> redisTemplate;
	@Test
	public void doTest(){
		ValueOperations<String, Object> ops = redisTemplate.opsForValue();
		ops.set("111","aaa");
		assertEquals("aaa",ops.get("111"),"找不到对应的值");
		log.info((String) ops.get("111"));
		ops.set("111","bbb");
		assertEquals("bbb",ops.get("111"),"找不到对应的值");
		log.info((String) ops.get("111"));
		redisTemplate.delete("111");
		assertEquals(null,ops.get("111"),"删除失败");
	}
}
