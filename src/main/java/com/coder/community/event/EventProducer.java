package com.coder.community.event;

import com.alibaba.fastjson.JSONObject;
import com.coder.community.entity.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventProducer {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    //处理事件
    public void fireEvent(Event event){
        //将事件发布到指定的主题(内容转化为JSON的字符串)
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
