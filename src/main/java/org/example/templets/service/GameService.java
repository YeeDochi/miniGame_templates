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

        for (Player player : players) {
            // 1. 비회원(dbUsername 없음)은 점수 저장 건너뜀
            if (player.getDbUsername() == null) {
                continue;
            }

            // 2. 해당 게임의 점수 계산 (게임별 로직에 맞게 호출)
            // 예: int score = room.calculateScore(player.getSenderId());
            // Yacht_Dice 예시:
            int totalScore = room.getTotalScore(player.getId());

            // 3. 점수 전송 (게임 이름은 프로젝트별로 변경: 예 "Yacht_Dice", "Omok" 등)
            scoreSender.sendScore(
                    player.getDbUsername(),
                    "GAME_NAME_HERE", // 🔥 게임 종류 식별자 (DB에 저장될 이름)
                    totalScore,       // 점수
                    true              // isScore (true: 점수형, false: 승패형 등 정책에 따름)
            );
        }

        // 방 삭제 또는 초기화 로직이 있다면 여기서 처리
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