package com.yz;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@ServletComponentScan
public class GameMain implements ApplicationListener<ContextClosedEvent> {
	static ApplicationContext context;
	
	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
		context = SpringApplication.run(GameMain.class, args);
	}

	public static <T> T getBean(Class<T> cls) {
		return context.getBean(cls);
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent arg0) {

	}
	

}
