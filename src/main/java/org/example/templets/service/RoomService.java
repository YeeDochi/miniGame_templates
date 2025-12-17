package org.example.templets.service;

import org.example.templets.dto.BaseGameRoom;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {
    private final Map<String, BaseGameRoom> rooms = new ConcurrentHashMap<>();

    public BaseGameRoom createRoom(String name) {
        // ★ 새 게임 만들 때 여기만 구현 클래스로 변경 (예: new OmokRoom(name))
        // BaseGameRoom room = new MyNewGameRoom(name);
        // rooms.put(room.getRoomId(), room);
        // return room;
        return null; // 템플릿이라 null 처리, 실제 구현 시 수정 필요
    }

    public BaseGameRoom findRoom(String roomId) {
        return rooms.get(roomId);
    }

    public List<BaseGameRoom> findAll() {
        return new ArrayList<>(rooms.values());
    }

    public void deleteRoom(String roomId) {
        rooms.remove(roomId);
    }
}