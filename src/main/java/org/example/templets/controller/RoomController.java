package org.example.templets.controller;

import org.example.common.service.ScoreSender;
import org.example.templets.dto.BaseGameRoom;
import org.example.templets.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final ScoreSender scoreSender;

    @RequestMapping(method = RequestMethod.HEAD)
    public void healthCheck() {
    }
    // 1. 방 목록 조회 (GET /api/rooms)
    @GetMapping
    public List<BaseGameRoom> findAllRooms() {
        return roomService.findAll();
    }

    // 2. 방 생성 (POST /api/rooms?name=...)
    @PostMapping
    public BaseGameRoom createRoom(@RequestParam String name) {
        return roomService.createRoom(name);
    }

    // 3. 특정 방 조회 (GET /api/rooms/{roomId})
    @GetMapping("/{roomId}")
    public BaseGameRoom getRoom(@PathVariable String roomId) {
        return roomService.findRoom(roomId);
    }

    @GetMapping("/rankings")
    public ResponseEntity<Object> getRanking(@RequestParam(required = false) String gameType) {
        return scoreSender.ranking(gameType);
    }
}