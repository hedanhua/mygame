package com.yz.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Iterator;
import java.util.List;

/**
 * JSON公共帮助类
 * @author bill.li
 *
 */
public class JSONUtil {

	/**
	 * Object转成String
	 */
	public static String objToStr(Object obj) {
		return JSON.toJSONString(obj);
	}

	/**
	 * String转成Object
	 */
	public static Object strToObj(String str, Class<?> clazz) {
		return JSON.parseObject(str, clazz);
	}

	/**
	 * 将JSON列表转换为字符串
	 * @param jsonList
	 * @return
	 */
	public static String JSONListToString(List<JSONObject> jsonList) {
		if (jsonList == null) {
			return "[]";
		}
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("[");
		for (JSONObject json : jsonList) {
			stringBuffer.append(json.toString());
			stringBuffer.append(",");
		}
		if (stringBuffer.length() != 1) {
			stringBuffer.deleteCharAt(stringBuffer.length() - 1);
		}
		stringBuffer.append("]");
		return stringBuffer.toString();
	}

	/**
	 * json格式转换成前端需要的jsa
	 * @param str
	 * @return
	 */
	public static JSONArray JsonToJsarray(String str){
		JSONArray jsa = new JSONArray();
		if(str!=null&&str.length()>2){
			JSONObject jsonObject = JSONObject.parseObject(str);
			Iterator<String> keys = jsonObject.keySet().iterator();
			 while(keys.hasNext()){
				jsa.add((String)keys.next());
			}
		}
		return jsa;
	}
	/**
	 * 将字符串转化为JSON列表
	 * @param jsonString 格式：[{"id":1,num:3},{"id":2,num:4}]
	 * @return
	 */
//	public static List<JSONObject> stringToJSONList(String jsonListString) {
//		List<JSONObject> jsonList = new ArrayList<JSONObject>();
//		try {
//			if (jsonListString.length() > 2) {
//				String jsonListStringTmp = jsonListString.substring(0, jsonListString.length() - 1);
//				String[] jsonStrings = jsonListStringTmp.split("}");
//				for (String jsonString : jsonStrings) {
//					JSONObject jsonObject = new JSONObject();
//					jsonObject = jsonObject.parse(jsonString.substring(1) + "}");
//					jsonList.add(jsonObject);
//				}
//			}
//		} catch (JSONException e) {
//			return new ArrayList<JSONObject>();
//		}
//		return jsonList;
//	}

}
