package com.yz.entity;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import lombok.Data;

@Data
@TableName(value = "player")
public class PlayerDTO extends Model<PlayerDTO>{
	@TableId
    /**
     * 玩家唯一id
     */
    private Long uid;

    /**
     * 玩家唯一id字符
     */
    private String playerId;

    /**
     * 区服ID
     */
    private Integer serverId;

    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 修改名字次数
     */
    private int nameNum;

    /**
     * 头像
     */
    private String head;

    /**
     * 省份
     */
    private String province;

    /**
     * openid
     */
    private String openId;

    /**
     * session_id
     */
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
 
    /**
     * 新天上次刷新时间
     */
    private Date newDayTime;

    /**
     * 新周上次刷新时间
     */
    private Date newWeekTime;

    /**
     * 新月上次刷新时间
     */
    private Date newMonthTime;
    
    /**
     * 上次系统邮件领取时间
     */
    private Date mailAcceptTime;

    /**
     * 登录时间
     */
    private Date loginTime;

    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 活跃时间(离线时间)
     */
    private Date activityTime;

    /**
     * 玩家扩展信息
     */
    private String props;

}
