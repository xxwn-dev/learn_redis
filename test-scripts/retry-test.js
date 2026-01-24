import http from 'k6/http';
import { check, sleep } from 'k6';

// 테스트 설정: 100명의 가상 유저(VU)가 동시에 1번씩 실행
export const options = {
    vus: 100,
    iterations: 100,
};

const BASE_URL = 'http://localhost:8080'; // 본인의 서버 주소로 수정
const TICKET_ID = 1;

export default function () {
    // 가상 유저 번호를 userId로 활용 (1~100)
    const userId = __VU;
    const url = `${BASE_URL}/purchase/v2/redis/${TICKET_ID}?userId=${userId}`;

    const res = http.post(url);

    // 응답 상태 코드 확인 (200 OK 또는 400 Bad Request)
    check(res, {
        'is status 200 or 400': (r) => r.status === 200 || r.status === 400,
    });
}