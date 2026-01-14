package org.example.templets.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.HashMap;
import java.util.Map;

@Getter @Setter
public class Player {
    private String sender;   // 닉네임
    private String id;       // [변경] 웹소켓 세션 ID (senderId -> id)
    private String dbUsername;
    private Map<String, Object> attributes = new HashMap<>();

    // 생성자 파라미터는 그대로 받아서 내부 id에 할당
    public Player(String sender, String senderId) {
        this.sender = sender;
        this.id = senderId;  // [변경] this.senderId -> this.id
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public int getInt(String key) {
        Object val = attributes.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }

    public String getString(String key) {
        Object val = attributes.get(key);
        return val != null ? val.toString() : "";
    }

    public boolean getBoolean(String key) {
        Object val = attributes.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return val != null && Boolean.parseBoolean(val.toString());
    }

    public void set(String key, Object value) {
        attributes.put(key, value);
    }
}