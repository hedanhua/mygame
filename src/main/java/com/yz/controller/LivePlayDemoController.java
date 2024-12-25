package com.yz.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yz.constant.RedisConstants;
import com.yz.model.JsonResponse;
import com.yz.model.LiveDataModel;
import com.yz.model.LivePlayAPIResponse;
import com.yz.util.RedisUtils;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import redis.clients.jedis.Tuple;

/**
 * 抖音云x弹幕玩法的服务端demo展示
 */
@RestController
@Slf4j
public class LivePlayDemoController {
	
	@Resource
	private RedisUtils redisUtils;
	
	private Map<String ,JSONObject> roundMap = new HashMap();
	


    /**
     * 开始玩法对局，玩法开始前调用
     */
    @PostMapping(path = "/start_game")
    public JsonResponse start_game(HttpServletRequest httpRequest) {
        // 开发者可以直接通过请求头获取直播间信息,无需自行通过token置换
        // 应用id
        String appID = httpRequest.getHeader("X-TT-AppID");
        // 直播间id
        String roomID = httpRequest.getHeader("X-Room-ID");
        // 主播id
        String anchorOpenID = httpRequest.getHeader("X-Anchor-OpenID");
        // 主播头像url
        String avatarUrl = httpRequest.getHeader("X-Avatar-Url");
        // 主播昵称
        String nickName = httpRequest.getHeader("X-Nick-Name");

        log.info("appID: {}, roomID: {}, anchorOpenID: {}, avatarUrl: {}, nickName: {}", appID,
                roomID, anchorOpenID, avatarUrl, nickName);

        JSONObject userObj = new JSONObject();
        userObj.put("openId", anchorOpenID);
        userObj.put("head", avatarUrl);
        userObj.put("name", nickName);
        redisUtils.set(RedisConstants.user_info+anchorOpenID, userObj);
        // 调用弹幕玩法服务端API，开启直播间推送任务，开启后，开发者服务器会通过/live_data_callback接口 收到直播间玩法指令
        List<String> msgTypeList = new ArrayList<>();
        msgTypeList.add("live_like");
        msgTypeList.add("live_comment");
        msgTypeList.add("live_gift");
      //  msgTypeList.add("live_fansclub");

        for (String msgType : msgTypeList) {
            boolean result = startLiveDataTask(appID, roomID, msgType);
            if (result) {
                log.info("roomID={},msgType={} 推送开启成功", roomID, msgType);
            } else {
                log.error("roomID={},msgType={} 推送开启失败",roomID, msgType);
            }
        }
 	Set<Tuple> setAll = redisUtils.getRankByPage(RedisConstants.user_score_rank, 0, 99);
		JSONArray rankArr = new JSONArray();
		int index = 0;
		String extra_data = "";
		if (setAll != null && !setAll.isEmpty()) {
			for (Tuple tmp : setAll) {
				    String openId = tmp.getElement();
				    JSONObject rankData = new JSONObject();
				    JSONObject userObj2 =  (JSONObject) redisUtils.get(RedisConstants.user_info+openId, JSONObject.class);

					rankData.put("name", userObj2!=null ? userObj2.getString("name"): openId);
					rankData.put("openId", openId);
					rankData.put("rank", ++index);
					rankData.put("score", tmp.getScore());
					rankData.put("head", userObj2!=null ?userObj2.getString("head"):openId);
					rankArr.add(rankData);
			}
			Map<String, Object> bodyMap = new HashMap<>();
			bodyMap.put("cmd", "rankList100");
			bodyMap.put("extra_data", rankArr);
			extra_data=JSON.toJSONString(rankArr);
			pushDataToClient(anchorOpenID,  JSON.toJSONString(bodyMap));
		}
		
	syncStartStatus(appID,roomID,anchorOpenID);
        JsonResponse response = new JsonResponse();
        response.success("开始玩法对局成功",extra_data);
        return response;
    }
    
    /**
     *同步对局开始状态​ 
     *
     */ 
    private boolean syncStartStatus(String appID, String roomID, String anchorOpenId​) {
	 JSONObject obj =  roundMap.get(roomID);
	 Long roundId = (long) 1;
	 if(obj!=null) {
	     roundId = obj.getLongValue("roundId");
	 }else {
	     obj = new JSONObject();
	 }
	 if(roundId == null) {
	    roundId=(long) 1;
	 }else {
	    roundId+=1;
	 }
	 long time = System.currentTimeMillis()/1000;
	 obj.put("roundId", roundId);
	 obj.put("appID", appID);
	 obj.put("roomID", roomID);
	 obj.put("startTime", time);
	 obj.put("anchorOpenId​", anchorOpenId​);
	 obj.put("roundStatus​", 1);
	 roundMap.put(roomID,obj);
	 OkHttpClient client = new OkHttpClient();
	        String body = new JSONObject()
	                .fluentPut("​room_id​​", roomID)
	                .fluentPut("​app_id​", appID)
	                .fluentPut("​anchor_open_id​", anchorOpenId​)
	                .fluentPut("​​round_id​​", roundId)
	                .fluentPut("​start_time​​", time)
	                .fluentPut("​​status​​", 1)
	                .toString();
	        String url = "https://webcast.bytedance.com/api/gaming_con/round/sync_status";
	        log.info("==============syncStartStatus,url={},body={}",body,url);
	        Request request = new Request.Builder()
	                .url(url) // 内网专线访问小玩法openAPI,无需https协议
	                .addHeader("Content-Type", "application/json") // 无需维护access_token
	                .post(
	                        okhttp3.RequestBody.create(
	                                MediaType.get("application/json; charset=utf-8"),
	                                body
	                        )
	                )
	                .build();
	        
	        try {
	            Response httpResponse = client.newCall(request).execute();
	            if (httpResponse.code() != 200) {
	                log.error("开启​同步对局状态​失败,http访问非200");
	                return false;
	            }
	            JSONObject result =JSON.parseObject(httpResponse.body().string());
	    	    if(result.getIntValue("​errcode")!=0){
	                log.error("开启​同步对局状态​失败，错误信息: {}", result.getString("​​errmsg​"));
	                return false;
	            }
	        } catch (IOException e) {
	            log.error("开启​同步对局状态​异常,e: {}", e.getMessage(), e);
	            return false;
	        }
	        return true;
    }
    
    
    /**
     *同步对局结束状态​ 
     *
     */ 
    private boolean syncEndStatus(String appID, String roomID, String anchorOpenId​, int result) {
	JSONObject obj =  roundMap.get(roomID);
	Long roundId = obj.getLongValue("roundId");
	if(roundId == null) {
	    roundId=(long) 1;
	}
	 long startTime =  obj.getLongValue("startTime");
	 long time =System.currentTimeMillis()/1000;
	 JSONArray overDataArray =new JSONArray();
	 JSONObject overData=new JSONObject();
	 //0=平局1=胜利、2=失败、
	 if(result==0) {
		 JSONObject overDataObj1 =new JSONObject();
		 overDataObj1.put("group_id", "1");
		 overDataObj1.put("result", "3");
		 overDataArray.add(overDataObj1);
		 JSONObject overDataObj2 =new JSONObject();
		 overDataObj2.put("group_id", "2");
		 overDataObj2.put("result", "3");
		 overDataArray.add(overDataObj2);
	 }else if(result==1) {
	         JSONObject overDataObj1 =new JSONObject();
		 overDataObj1.put("group_id", "1");
		 overDataObj1.put("result", "1");
		 overDataArray.add(overDataObj1);
		 JSONObject overDataObj2 =new JSONObject();
		 overDataObj2.put("group_id", "2");
		 overDataObj2.put("result", "2");
		 overDataArray.add(overDataObj2);
	 }else if(result==2) {
	                 JSONObject overDataObj1 =new JSONObject();
	  		 overDataObj1.put("group_id", "1");
	  		 overDataObj1.put("result", "2");
	  		 overDataArray.add(overDataObj1);
	  		 JSONObject overDataObj2 =new JSONObject();
	  		 overDataObj2.put("group_id", "2");
	  		 overDataObj2.put("result", "1");
	  		 overDataArray.add(overDataObj2);
	 }
	 overData.put("group_result_list", overDataArray);
	 OkHttpClient client = new OkHttpClient();
	        String body = new JSONObject()
	                .fluentPut("​room_id​​", roomID)
	                .fluentPut("​app_id​", appID)
	                .fluentPut("​anchor_open_id​", anchorOpenId​)
	                .fluentPut("​​round_id​​", roundId)
	                .fluentPut("​​​start_time​​​​", startTime)
	                .fluentPut("​​end_time​​​", time)
	                .fluentPut("​​status​​", 2)
	                .fluentPut("​group_result_list​​", overData)
	                .toString();
	        String url = "https://webcast.bytedance.com/api/gaming_con/round/sync_status";
	        log.info("==============syncEndStatus,url={},body={}",body,url);
	        Request request = new Request.Builder()
	                .url(url) // 内网专线访问小玩法openAPI,无需https协议
	                .addHeader("Content-Type", "application/json") // 无需维护access_token
	                .post(
	                        okhttp3.RequestBody.create(
	                                MediaType.get("application/json; charset=utf-8"),
	                                body
	                        )
	                )
	                .build();
	        
	        try {
	            Response httpResponse = client.newCall(request).execute();
	            if (httpResponse.code() != 200) {
	                log.error("结束​同步对局状态​失败,http访问非200");
	                return false;
	            }
	            JSONObject result2 =JSON.parseObject(httpResponse.body().string());
	    	    if(result2.getIntValue("​errcode")!=0){
	                log.error("结束同步对局状态​失败，错误信息: {}", result2.getString("​​errmsg​"));
	                return false;
	            }
	        } catch (IOException e) {
	            log.error("结束同步对局状态​异常,e: {}", e.getMessage(), e);
	            return false;
	        }
	        return true;
    }
    
    //上报阵营数据​
    public boolean uploadUserGroupInfo( String roomID, String openId,String groupId) {
	JSONObject obj =  roundMap.get(roomID);
	Long roundId = obj.getLongValue("roundId");
	String appID = obj.getString("appID");
	 OkHttpClient client = new OkHttpClient();
	        String body = new JSONObject()
	                .fluentPut("room_id​", roomID)
	                .fluentPut("​app_id​", appID)
	                .fluentPut("​open_id​", openId)
	                .fluentPut("​​round_id​​", roundId)
	                .fluentPut("​​group_id​​", groupId)
	                .toString();
	        Request request = new Request.Builder()
	                .url("​https://webcast.bytedance.com/api/gaming_con/round/upload_user_group_info") // 内网专线访问小玩法openAPI,无需https协议
	                .addHeader("Content-Type", "application/json") // 无需维护access_token
	                .post(
	                        okhttp3.RequestBody.create(
	                                MediaType.get("application/json; charset=utf-8"),
	                                body
	                        )
	                )
	                .build();
	        try {
	            Response httpResponse = client.newCall(request).execute();
	            if (httpResponse.code() != 200) {
	                log.error("上报阵营数据失败,http访问非200");
	                return false;
	            }
	            JSONObject result =JSON.parseObject(httpResponse.body().string());
	    	    if(result.getIntValue("​errcode")!=0){
	                log.error("上报阵营数据失败，错误信息: {}", result.getString("​​errmsg​"));
	                return false;
	            }
	        } catch (IOException e) {
	            log.error("上报阵营数据异常,e: {}", e.getMessage(), e);
	            return false;
	        }
	        return true;
    }
    
    
    
    /**
     * startLiveDataTask: 开启推送任务：<a href="https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/server/live/danmu#%E5%90%AF%E5%8A%A8%E4%BB%BB%E5%8A%A1">...</a>
     *
     * @param appID   小玩法appID
     * @param roomID  直播间ID
     * @param msgType 评论/点赞/礼物/粉丝团
     */
    private boolean startLiveDataTask(String appID, String roomID, String msgType) {
        // example: 通过java OkHttp库发起http请求,开发者可使用其余http访问形式
        OkHttpClient client = new OkHttpClient();
        String body = new JSONObject()
                .fluentPut("roomid", roomID)
                .fluentPut("appid", appID)
                .fluentPut("msg_type", msgType)
                .toString();
        Request request = new Request.Builder()
                .url("http://webcast.bytedance.com/api/live_data/task/start") // 内网专线访问小玩法openAPI,无需https协议
                .addHeader("Content-Type", "application/json") // 无需维护access_token
                .post(
                        okhttp3.RequestBody.create(
                                MediaType.get("application/json; charset=utf-8"),
                                body
                        )
                )
                .build();

        try {
            Response httpResponse = client.newCall(request).execute();
            if (httpResponse.code() != 200) {
                log.error("开启推送任务失败,http访问非200");
                return false;
            }
            LivePlayAPIResponse livePlayAPIResponse
                    = JSON.parseObject(httpResponse.body().string(), LivePlayAPIResponse.class);
            if (livePlayAPIResponse.getErrNo() != 0) {
                log.error("开启推送任务失败，错误信息: {}", livePlayAPIResponse.getErrorMsg());
                return false;
            }
        } catch (IOException e) {
            log.error("开启推送任务异常,e: {}", e.getMessage(), e);
            return false;
        }
        return true;
    }
    
    public static void main(String[] args) {
	         JSONObject obj =new JSONObject();
        	JSONArray rankArr = new JSONArray();
        	JSONObject rankData1 = new JSONObject();
        	rankData1.put("openId", "1");
        	rankData1.put("score", 1000);
        	JSONObject rankData2 = new JSONObject();
        	rankData2.put("openId", "2");
        	rankData2.put("score", 2000);
		rankArr.add(rankData1); 
		rankArr.add(rankData2); 
		
		obj.put("result", 1);
		obj.put("scores", rankArr);
		System.out.println(JSON.toJSONString(obj));
	}


    /**
     * 获取阵营数据
     */
    @PostMapping(path = "/getGroupData")
    public JsonResponse getGroupData(HttpServletRequest httpRequest, @RequestBody String body) {
	    log.info("==========getGroupData,body={}",body);
	    JSONObject data = JSONObject.parseObject(body);
	    String app_id = data.getString("​app_id​");
	    String room_id​ = data.getString("​room_id​");
	    String open_id = data.getString("open_id");
	    JsonResponse response = new JsonResponse();
	    
	    JSONObject obj =  roundMap.get(room_id​);
	    Long roundId = (long) 0;
	    String group_id = "";
	    int round_status=2;
	    int user_group_status​=0;
	    if(obj!=null) {
		    roundId = obj.getLongValue("roundId");
		    round_status=1;
		    JSONArray array = obj.getJSONArray("openIds");
		    boolean flag= false;
		    for(int i=0;i<array.size();i++) {
			JSONObject openObj = array.getJSONObject(i);
			String openId = openObj.getString("openId");
			if(openId.equals(open_id)) {
			    group_id = openObj.getString("groupId");
			    flag =true;
			    break;
			}
		    }
		    if(flag) {
			user_group_status​=1;
		    }
	    }
	    JSONObject jsonbject =new JSONObject();
	    jsonbject.put("round_id", roundId);
	    jsonbject.put("round_status", round_status);
	    jsonbject.put("group_id​", group_id);
	    jsonbject.put("user_group_status​​", user_group_status​);
	    response.success(jsonbject,null);
	    return response;
    }

    /**
     * 结束玩法
     */
    @PostMapping(path = "/finish_game")
    public JsonResponse finishGame(HttpServletRequest httpRequest, @RequestBody String body) {
	String roomID = httpRequest.getHeader("X-Room-ID");
	log.info("==========finish_game,roomID={},body={}",roomID,body);
    	List<String> users = new ArrayList<>();
    	if(!StringUtils.isEmpty(body)){
    	       JSONObject data = JSONObject.parseObject(body);
    	       int result = data.getIntValue("result");
    	       JSONObject roundObj =  roundMap.get(roomID);
    	       syncEndStatus(roundObj.getString("appID"),roundObj.getString("roomID"),
    		       roundObj.getString("anchorOpenId​"),result);
    	       roundMap.remove(roomID);
    		//JSONArray array = JSONArray.parseArray(body);
    	       JSONArray array = data.getJSONArray("scores");
    	       if(array!=null) {
       		    for(int i=0;i<array.size();i++){
			JSONObject obj = array.getJSONObject(i);
    		         String openId = obj.getString("openId");
    		          long score = obj.getLongValue("score");
    		         Double myScore = redisUtils.zscore(RedisConstants.user_score_rank, openId);
        		if(myScore == null){
        			myScore = (double) 0;
        		}
        		myScore = myScore+score;
        		redisUtils.addRankNew(RedisConstants.user_score_rank, openId, myScore.longValue());
		   }
    	       }
    	}
    	Set<Tuple> setAll = redisUtils.getRankByPage(RedisConstants.user_score_rank, 0, -1);
		JSONArray rankArr = new JSONArray();
		int index = 0;
		String extra_data = "";
		if (setAll != null && !setAll.isEmpty()) {
			for (Tuple tmp : setAll) {
				    String openId = tmp.getElement();
				    JSONObject rankData = new JSONObject();
				    JSONObject userObj =  (JSONObject) redisUtils.get(RedisConstants.user_info+openId, JSONObject.class);
					rankData.put("name",userObj!=null? userObj.getString("name"):openId);
					rankData.put("openId", openId);
					rankData.put("rank", ++index);
					rankData.put("score", tmp.getScore());
					rankData.put("head", userObj!=null?userObj.getString("head"):openId);
					rankArr.add(rankData);
			}
			Map<String, Object> bodyMap = new HashMap<>();
			bodyMap.put("cmd", "rankList");
			bodyMap.put("extra_data", JSON.toJSONString(rankArr));
			extra_data =  JSON.toJSONString(rankArr);
			for(String anchorOpenId:users){
	        	  pushDataToClient(anchorOpenId,  JSON.toJSONString(bodyMap));
			}
		}
        JsonResponse response = new JsonResponse();
        response.success("结束玩法成功",extra_data);
        return response;
    }
    
   

    /**
     * 通过抖音云服务接受直播间数据，内网专线加速+免域名备案
     * 通过内网专线会自动携带X-Anchor-OpenID字段
     * ref: <a href="https://developer.open-douyin.com/docs/resource/zh-CN/developer/tools/cloud/develop-guide/danmu-callback">...</a>
     */
    @PostMapping(path = "/live_data_callback")
    public JsonResponse liveDataCallbackExample(
            @RequestHeader("X-Anchor-OpenID") String anchorOpenID,
            @RequestHeader("x-msg-type") String msgType,
            @RequestHeader("x-roomid") String roomID,
            @RequestBody String body) {
	if(!msgType.equals("user_group_push")) {
	        List<LiveDataModel> liveDataModelList = JSON.parseArray(body, LiveDataModel.class);
	        liveDataModelList.forEach(liveDataModel ->
	                pushDataToClientByDouyinCloudWebsocket(anchorOpenID, liveDataModel.getMsgID(), msgType, body)
	        );
	        JSONArray array = JSONArray.parseArray(body);
	        JSONObject obj = array.getJSONObject(0);
	        JSONObject userObj = new JSONObject();
	        userObj.put("openId", anchorOpenID);
	        userObj.put("head", obj.getString("avatar_url"));
	        userObj.put("name", obj.getString("nickname"));
	        redisUtils.set(RedisConstants.user_info+anchorOpenID, userObj);
	        
	        if(msgType.equals("live_comment")) {
	            JSONObject round =  roundMap.get(roomID);
		    JSONArray roundarray =new JSONArray();
		    boolean flag =false;
		    if(round!=null) {
			roundarray = round.getJSONArray("openIds");
			if(roundarray!=null) {
			    for(int i=0;i<roundarray.size();i++) {
				JSONObject openObj = roundarray.getJSONObject(i);
				String openId = openObj.getString("openId");
				if(openId.equals(anchorOpenID)) {
				    flag =true;
				    break;
				}
			    }
			}else {
			    roundarray =new JSONArray();
			}
		    }
		    if(!flag) {
		                JSONArray array2 = JSONArray.parseArray(body);
				for (int i = 0; i < array2.size(); i++) {
				        JSONObject obj2= array2.getJSONObject(i);
					String content = obj2.getString("content");
					if(content.trim().equals("1")) {
					      JSONObject openObj =new JSONObject();
					      openObj.put("openId", anchorOpenID);
					      openObj.put("groupId", "1");
					      roundarray.add(openObj);
					      round.put("openIds", roundarray);
					      roundMap.put(roomID, round);
					      uploadUserGroupInfo(roomID, anchorOpenID, "1");
					      break;
					}else if(content.trim().equals("2")) {
					      JSONObject openObj =new JSONObject();
					      openObj.put("openId", anchorOpenID);
					      openObj.put("groupId", "21");
					      roundarray.add(openObj);
					      round.put("openIds", roundarray);
					      round.put("groupId", "2");
					      roundMap.put(roomID, round);
					      uploadUserGroupInfo(roomID, anchorOpenID, "2");
					      break;
					}
				}
		    }
	        }
	    
	        JsonResponse response = new JsonResponse();
	        response.success("success");
	        return response;
	}else {
	    JSONObject data = JSONObject.parseObject(body);
	    //String app_id = data.getString("​app_id​");
	    String room_id​ = data.getString("​room_id​");
	    String open_id = data.getString("open_id");
	    String group_id = data.getString("group_id");
	    log.info("用户ID=={}，加入阵营={}，room_id​={}",open_id,group_id,room_id​);
	    JsonResponse response = new JsonResponse();
	    JSONObject obj =  roundMap.get(room_id​);
	    Long roundId = (long) 0;
	    int round_status=2;
	    JSONArray array = new JSONArray();
	    if(obj!=null) {
		    roundId = obj.getLongValue("roundId");
		    round_status=1;
		    array = obj.getJSONArray("openIds");
		    if(array==null) {
			array =new JSONArray();
		    }
		     JSONObject openObj =new JSONObject();
		      openObj.put("openId", open_id);
		      openObj.put("groupId", group_id);
		      array.add(openObj);
		      obj.put("openIds", array);
	    }
	    roundMap.put(room_id​, obj);
	    JSONObject jsonbject =new JSONObject();
	    jsonbject.put("round_id", roundId);
	    jsonbject.put("round_status", round_status);
	    jsonbject.put("group_id", group_id);
	    response.success(jsonbject,null);
	    return response;
	}
    }


    //---------------- 抖音云websocket相关demo ---------------------

    /**
     * 抖音云websocket监听的回调函数,客户端建连/上行发消息都会走到该HTTP回调函数中
     * ref: <a href="https://developer.open-douyin.com/docs/resource/zh-CN/developer/tools/cloud/develop-guide/websocket-guide/websocket#%E5%BB%BA%E8%BF%9E%E8%AF%B7%E6%B1%82">...</a>
     */
    @RequestMapping(path = "/websocket_callback", method = {RequestMethod.POST, RequestMethod.GET})
    public JsonResponse websocketCallback(HttpServletRequest request) {
        String eventType = request.getHeader("x-tt-event-type");
        switch (eventType) {
            case "connect":
                // 客户端建连
            case "disconnect": {
                // 客户端断连
            }
            case "uplink": {
                // 客户端上行发消息
            }
            default:
                break;
        }
        JsonResponse response = new JsonResponse();
        response.success("success");
        return response;
    }

    /**
     * 使用抖音云websocket网关,将数据推送到主播端
     * ref: <a href="https://developer.open-douyin.com/docs/resource/zh-CN/developer/tools/cloud/develop-guide/websocket-guide/websocket#%E4%B8%8B%E8%A1%8C%E6%B6%88%E6%81%AF%E6%8E%A8%E9%80%81">...</a>
     */
    private void pushDataToClientByDouyinCloudWebsocket(String anchorOpenId, String msgID, String msgType, String data) {
        // 这里通过HTTP POST请求将数据推送给抖音云网关,进而抖音云网关推送给主播端
        OkHttpClient client = new OkHttpClient();

        Map<String, String> bodyMap = new HashMap<>();
        bodyMap.put("msg_id", msgID);
        bodyMap.put("msg_type", msgType);
        bodyMap.put("data", data);
//        JSONObject obj = new JSONObject();
//        obj.put("rank", 1);
//        obj.put("score", 200);
//        bodyMap.put("extra_data", obj.toJSONString());
        String bodyStr = JSON.toJSONString(bodyMap);

        Request request = new Request.Builder()
                .url("http://ws-push.dycloud-api.service/ws/live_interaction/push_data")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-TT-WS-OPENIDS", JSON.toJSONString(Arrays.asList(anchorOpenId)))
                .post(
                        okhttp3.RequestBody.create(
                                MediaType.parse("application/json; charset=utf-8"),
                                bodyStr
                        )
                )
                .build();

        try {
            Response httpResponse = client.newCall(request).execute();
            log.info("websocket http call done, response: {}", JSON.toJSONString(httpResponse));
        } catch (IOException e) {
            log.error("websocket http call exception, e: ", e);
        }
    }
    
    
    private void pushDataToClient(String anchorOpenId, String bodyStr) {
        // 这里通过HTTP POST请求将数据推送给抖音云网关,进而抖音云网关推送给主播端
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://ws-push.dycloud-api.service/ws/live_interaction/push_data")
                .addHeader("Content-Type", "application/json")
                .addHeader("X-TT-WS-OPENIDS", JSON.toJSONString(Arrays.asList(anchorOpenId)))
                .post(
                        okhttp3.RequestBody.create(
                                MediaType.parse("application/json; charset=utf-8"),
                                bodyStr
                        )
                )
                .build();

        try {
            Response httpResponse = client.newCall(request).execute();
            log.info("websocket http call done, response: {}", JSON.toJSONString(httpResponse));
        } catch (IOException e) {
            log.error("websocket http call exception, e: ", e);
        }
    }
}