package com.yz.asp;

import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSONObject;


@Aspect
@Component
public class AopLog {

	private static final Logger logger = LoggerFactory.getLogger(AopLog.class);

	@Pointcut("execution(public * com.yz.controller.*.*(..))")
	public void log() {

	}

	@Around("log()")
	public Object aroundLog(ProceedingJoinPoint point) throws Throwable {
		long startTime = System.currentTimeMillis();
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attributes == null) {
			return null;
		}
		Object result = point.proceed();
		long endTime = System.currentTimeMillis();
		Object[] args = point.getArgs();
		String requestStr = null;
		if(args.length >1){
		    requestStr = JSONObject.toJSONString(args[1]);
		}
		HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();
		String url = request.getRequestURI();
		String responseStr = JSONObject.toJSONString(result);
		logger.info("url={},耗时=={}ms,请求参数={},返回参数={}", url, endTime - startTime, requestStr, responseStr);
		return result;
	}

}
