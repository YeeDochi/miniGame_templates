/* [card-core.js] 카드 게임용 유틸리티 */
const CardModule = {
    // 문양 문자 변환 (S -> ♠)
    getSuitChar: (suit) => {
        const map = { 'S': '♠', 'D': '♦', 'H': '♥', 'C': '♣' };
        return map[suit] || suit;
    },

    // 색상 판별
    getColor: (suit) => {
        return (suit === 'H' || suit === 'D') ? 'red' : 'black';
    },

    // 랭크 변환 (1 -> A, 11 -> J ...)
    getRankStr: (rank) => {
        const map = { 1: 'A', 11: 'J', 12: 'Q', 13: 'K' };
        return map[rank] || rank;
    },

    /**
     * 카드 HTML 요소 생성
     * @param {string} suit - 'S', 'D', 'H', 'C'
     * @param {number|string} rank - 1~13
     * @param {boolean} isFlipped - 처음에 뒷면으로 보여줄지 여부
     */
    createCardElement: (suit, rank, isFlipped = false) => {
        const suitChar = CardModule.getSuitChar(suit);
        const rankStr = CardModule.getRankStr(rank);
        const color = CardModule.getColor(suit);

        const area = document.createElement('div');
        area.className = 'card-area';

        // 카드 내부 구조 (앞면/뒷면)
        area.innerHTML = `
            <div class="card ${color} ${isFlipped ? 'flipped' : ''}">
                <div class="card-face">
                    <div style="font-size:16px; font-weight:bold; text-align:left;">${rankStr}${suitChar}</div>
                    <div style="font-size:36px; position:absolute; top:50%; left:50%; transform:translate(-50%,-50%);">${suitChar}</div>
                    <div style="font-size:16px; font-weight:bold; text-align:right; transform:rotate(180deg);">${rankStr}${suitChar}</div>
                </div>
                <div class="card-back"></div>
            </div>
        `;

        return { area, cardInner: area.querySelector('.card') };
    },

    /**
     * 손패(Hand) 그리기
     * @param {string} containerId - 카드를 넣을 div ID
     * @param {Array} cards - [{suit:'S', rank:1}, ...]
     * @param {boolean} isOverlap - 카드 겹치기 여부
     * @param {Function} onClick - 클릭 시 실행할 콜백 (cardObj => {})
     */
    renderHand: (containerId, cards, isOverlap, onClick) => {
        const container = document.getElementById(containerId);
        if(!container) return;

        container.innerHTML = '';
        container.className = `hand-container ${isOverlap ? 'overlap' : ''}`;

        cards.forEach((c, idx) => {
            // 정보가 없으면(-1 등) 뒷면으로 생성
            const isUnknown = (c.suit === '?' || c.rank === -1);
            const { area } = CardModule.createCardElement(
                isUnknown ? 'S' : c.suit,
                isUnknown ? 1 : c.rank,
                isUnknown // 모르는 카드는 뒤집힌 상태로 시작
            );

            // 딜링 애니메이션 (순차적)
            area.style.animationDelay = `${idx * 0.1}s`;
            area.classList.add('card-anim-deal');

            if (onClick && !isUnknown) {
                area.onclick = () => {
                    // 선택 효과 토글
                    const isSelected = area.classList.toggle('selected');
                    onClick(c, isSelected);
                };
            }
            container.appendChild(area);
        });
    }
};