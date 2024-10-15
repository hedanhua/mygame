package com.yz.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class LiveDataModel {
    @JSONField(name = "msg_id")
    private String MsgID;


}