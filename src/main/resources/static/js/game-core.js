// [game-core.js]
const Core = (function() {
    let stompClient = null;
    let myId = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
        const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
    let myNickname = "";
    let currentRoomId = "";
    let GameImpl = null; // 게임 구현체
    let CONFIG = { apiPath: "", wsPath: "" };

    // --- 초기화 및 공통 기능 ---
    function init(implementation, config) {
        GameImpl = implementation;
        CONFIG = config;
        document.getElementById('game-title-header').innerText = config.gameName;
        // 테마 로드
        if (localStorage.getItem('theme') === 'dark') document.body.classList.add('dark-mode');
    }

    function login() {
        const input = document.getElementById('nicknameInput').value.trim();
        if (!input) return showAlert("닉네임을 입력하세요.");
        myNickname = input;
        document.getElementById('welcome-msg').innerText = ` ${myNickname}님`;
        document.getElementById('login-screen').classList.add('hidden');
        document.getElementById('lobby-screen').classList.remove('hidden');
        loadRooms();
    }

    function loadRooms() {
        fetch(`${CONFIG.apiPath}/api/rooms`)
            .then(res => res.json())
            .then(rooms => {
                const list = document.getElementById('room-list');
                list.innerHTML = '';
                if (!rooms.length) list.innerHTML = '<li style="padding:15px; text-align:center; color:#888;">생성된 방이 없습니다.</li>';
                rooms.forEach(r => {
                    const li = document.createElement('li');
                    li.className = 'room-item';
                    li.innerHTML = `<span style="font-weight:bold;">${r.roomName}</span> <button class="btn-default" onclick="Core.joinRoom('${r.roomId}', '${r.roomName}')">참가</button>`;
                    list.appendChild(li);
                });
            });
    }

    function createRoom() {
        const name = document.getElementById('roomNameInput').value;
        if (!name) return showAlert("방 제목을 입력하세요.");
        // 게임별 추가 파라미터가 있다면 GameImpl에서 가져올 수 있음
        fetch(`${CONFIG.apiPath}/api/rooms?name=${encodeURIComponent(name)}`, { method: 'POST' })
            .then(res => res.json())
            .then(room => joinRoom(room.roomId, room.roomName));
    }

    function joinRoom(roomId, roomName) {
        fetch(`${CONFIG.apiPath}/api/rooms/${roomId}`)
            .then(res => res.json())
            .then(room => {
                currentRoomId = roomId;
                document.getElementById('room-title-text').innerText = roomName;
                document.getElementById('lobby-screen').classList.add('hidden');
                document.getElementById('game-screen').classList.remove('hidden');
                document.getElementById('messages').innerHTML = ''; // 채팅 초기화

                // ★ 게임별 무대 설치
                const stage = document.getElementById('game-stage');
                const tools = document.getElementById('game-tools-area');
                const header = document.getElementById('custom-header-area');
                if (GameImpl.onEnterRoom) GameImpl.onEnterRoom(stage, tools, header);

                connectStomp(roomId);
            })
            .catch(err => showAlert("입장 실패: " + err));
    }

    function connectStomp(roomId) {
        const socket = new SockJS(CONFIG.wsPath);
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // 디버그 로그 끄기
        stompClient.connect({}, function () {
            // 입장 메시지
            stompClient.send(`/app/${roomId}/join`, {}, JSON.stringify({ type: 'JOIN', sender: myNickname, senderId: myId }));

            // 구독
            stompClient.subscribe(`/topic/${roomId}`, function (msg) {
                const body = JSON.parse(msg.body);
                handleCommonMessage(body);
            });
        });
    }

    function handleCommonMessage(msg) {
        // 공통 처리: 채팅, 퇴장, 게임오버
        if (msg.type === 'CHAT') showChat(msg.sender, msg.content);
        else if (msg.type === 'EXIT') showChat('SYSTEM', msg.content);
        else if (msg.type === 'GAME_OVER') {
            document.getElementById('ranking-modal').classList.remove('hidden');
            document.getElementById('winnerName').innerText = (msg.winnerName || msg.sender) + " 승리!";
            document.getElementById('winnerImage').src = msg.winnerSkin || "https://via.placeholder.com/100";
            confetti({ particleCount: 150, spread: 70, origin: { y: 0.6 } });
        }
        // ★ 그 외(STONE, DRAW 등)는 구현체에게 넘김
        else {
            if (GameImpl.handleMessage) GameImpl.handleMessage(msg, myId);
        }
    }

    function sendChat() {
        const input = document.getElementById('chatInput');
        if (!input.value.trim()) return;
        stompClient.send(`/app/${currentRoomId}/chat`, {}, JSON.stringify({ type: 'CHAT', sender: myNickname, senderId: myId, content: input.value }));
        input.value = '';
    }

    function showChat(sender, msg) {
        const div = document.createElement('div');
        div.className = sender === 'SYSTEM' ? 'msg-system' : 'msg-item';
        div.innerHTML = sender === 'SYSTEM' ? msg : `<span style="font-weight:bold;">${sender}</span>: ${msg}`;
        const box = document.getElementById('messages');
        box.appendChild(div);
        box.scrollTop = box.scrollHeight;
    }

    // --- Helper Functions ---
    function showAlert(msg) {
        document.getElementById('alert-msg-text').innerText = msg;
        document.getElementById('alert-modal').classList.remove('hidden');
    }
    function closeAlert() { document.getElementById('alert-modal').classList.add('hidden'); }
    function closeRanking() {
        document.getElementById('ranking-modal').classList.add('hidden');
        exitRoom();
    }
    function exitRoom() {
        if(stompClient) stompClient.disconnect();
        location.reload();
    }
    function toggleTheme() {
        document.body.classList.toggle('dark-mode');
        localStorage.setItem('theme', document.body.classList.contains('dark-mode') ? 'dark' : 'light');
    }

    return {
        init, login, createRoom, joinRoom, loadRooms, sendChat, showAlert, closeAlert, closeRanking, exitRoom, toggleTheme,
        startGame: () => stompClient.send(`/app/${currentRoomId}/action`, {}, JSON.stringify({ type: 'START', senderId: myId })),
        // ★ 게임 로직에서 서버로 보낼 때 쓰는 함수
        sendAction: (data) => {
            stompClient.send(`/app/${currentRoomId}/action`, {}, JSON.stringify({
                type: 'ACTION', senderId: myId, sender: myNickname, ...data
            }));
        }
    };
})();