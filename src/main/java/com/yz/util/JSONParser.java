package com.yz.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

/**
 * JSON与Java对象序列化反序列化解析类
 * */
public class JSONParser{
	private static final SerializerFeature[] FEATURES=new SerializerFeature[]{SerializerFeature.WriteClassName};
	
	public static byte[] getBytes(Object object) {
		return getString(object).getBytes();
	}

	
	public static Object getObject(byte[] buf) {
		String json=new String(buf);
		return JSON.parse(json);
	}

	
	public static String getString(Object object) {
		return JSON.toJSONString(object, FEATURES);
	}

	
	public static Object getObject(String text) {
		try {
			return JSON.parse(text);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
}
