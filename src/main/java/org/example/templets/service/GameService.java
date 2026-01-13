package org.example.templets.service;

import org.example.templets.dto.BaseGameRoom;
import org.example.templets.dto.GameMessage;
import org.example.templets.dto.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {
    private final RoomService roomService;
    private final SimpMessagingTemplate messagingTemplate;
    private final org.example.common.service.ScoreSender scoreSender;
    // 입장 처리
    public void join(String roomId, GameMessage message) {
        BaseGameRoom room = roomService.findRoom(roomId);
        if (room == null) return;

        Player newPlayer = new Player(message.getSender(), message.getSenderId());

        // [추가] 로그인 유저 체크 및 ID 저장 로직
        if (message.getData() != null && message.getData().containsKey("dbUsername")) {
            String realId = (String) message.getData().get("dbUsername");
            if (realId != null && !realId.equals("null") && !realId.isEmpty()) {
                newPlayer.setDbUsername(realId);
                System.out.println("✅ 로그인 유저 입장: " + newPlayer.getSender() + " (" + realId + ")");
            }
        }

        room.enterUser(newPlayer);

        message.setType("JOIN");
        message.setContent(message.getSender() + "님이 입장하셨습니다.");
        broadcast(roomId, message);

        // ... (기존 broadcast 코드) ...

        // [Tip] 실제 구현 시 주석 해제: 기존 유저 정보를 신규 유저에게 동기화
//        for (Player p : room.getUsers().values()) {
//            if (p.getId().equals(message.getSenderId())) continue; // 나 자신 제외
//
//            GameMessage syncMsg = GameMessage.builder()
//                    .type("JOIN")
//                    .sender(p.getNickname())
//                    .senderId(p.getId())
//                    // Player의 attributes나 skinUrl을 data에 담아서 전송
//                    .data(Map.of("semple", "semple"))
//                    .build();
//
//            messagingTemplate.convertAndSend("/topic/" + roomId, syncMsg);
//        }
        GameMessage syncMsg = new GameMessage();
        syncMsg.setType("SYNC");
        syncMsg.setRoomId(roomId);
        syncMsg.setSender("SYSTEM");
        syncMsg.setData(room.getGameSnapshot()); // BaseGameRoom에 추가한 메서드 호출

        // 특정 유저에게만 보내는 게 정석이지만, 템플릿 구조상 전체 broadcast 후 클라이언트가 필터링해도 됨
        broadcast(roomId, syncMsg);
    }

    // 게임 행동 처리 (핵심)
    public void handleGameAction(String roomId, GameMessage message) {
        BaseGameRoom room = roomService.findRoom(roomId);
        if (room == null) return;

        GameMessage result = room.handleAction(message);

        if (result != null) {
            // [추가] 게임 종료 신호가 오면 점수 저장 로직 실행
            if ("GAME_OVER".equals(result.getType())) {
                // 방에 있는 모든 유저 정보를 넘겨줌
                endGame(roomId, new ArrayList<>(room.getUsers().values()));
            }

            broadcast(roomId, result);
        }
    }

    public void chat(String roomId, GameMessage message) {
        // 정답 체크 로직이 필요하면 여기서 room.checkAnswer() 등을 호출 가능
        broadcast(roomId, message);
    }
    public void endGame(String roomId, List<Player> players) {
        BaseGameRoom room = roomService.findRoom(roomId);
        if (room == null) return; // 방어 로직 추가

        for (Player player : players) {
            // 1. 비회원 건너뛰기
            if (player.getDbUsername() == null) {
                continue;
            }

            // 2. 점수 가져오기 (형변환 필요)
            // [주의] 실제 만드시는 게임 Room 클래스 이름으로 변경하세요 (예: OmokRoom)
            int totalScore = 0;
            if (room instanceof org.example.templets.dto.MyGameRoom) {
                org.example.templets.dto.MyGameRoom myRoom = (org.example.templets.dto.MyGameRoom) room;
                totalScore = myRoom.getTotalScore(player.getSenderId()); // getSenderId() 사용
                //방을 가져와서 변경할 점수를 기입. 만약 승수로 판단하는게임이라면 그냥 없어된다. Score 는  0  이나 null로
            }

            // 3. 점수 전송
            scoreSender.sendScore(
                    player.getDbUsername(),
                    "My_Game_Title", // 🔥 실제 게임 이름으로 변경
                    totalScore,
                    true
            );
        }
    }
    public void exit(String roomId, GameMessage message) {
        BaseGameRoom room = roomService.findRoom(roomId);
        if (room != null) {
            room.exitUser(message.getSenderId());
            if (room.getUsers().isEmpty()) {
                roomService.deleteRoom(roomId);
            } else {
                broadcast(roomId, message);
            }
        }
    }

    private void broadcast(String roomId, GameMessage message) {
        messagingTemplate.convertAndSend("/topic/" + roomId, message);
    }
}