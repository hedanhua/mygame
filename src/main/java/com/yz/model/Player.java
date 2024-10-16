package com.yz.model;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;

@Data
public class Player implements Serializable {
	private String playerId;
	private long uid;
	private int serverId;
	private String nickname;
    
    /**
     * 修改名字次数
     */
    private int nameNum;
	private String head;
	private String province;
	private String openId;
	private String sessionId;
    /**
     * 当前外观id
     */
    private Integer skinId;

    /**
     * 连胜次数
     */
    private Integer continueWinNum;
    /**
     * 胜利总次数
     */
    private Integer winNum;
    /**
     *本周积分
     */
    private Long weekScore;
    /**
     *总积分
     */
    private Long score;
	//当前抖音房间ID
    private String dyRoomId;
	
	private long newDayTime;
	private long newWeekTime;
	private long newMonthTime;
	private long createTime;
	private long loginTime;
	private long updateTime;
	private long activityTime;
	// 战斗属性
	ConcurrentHashMap<String, Double> attrs = new ConcurrentHashMap<>();
	//玩家扩展信息
    ConcurrentHashMap<String, Long> props = new ConcurrentHashMap<>();

    


    public void onNewDay(){
	    //TODO
    }
    
    public void onNewWeek(){
    	weekScore =(long) 0;
    }
	
}
