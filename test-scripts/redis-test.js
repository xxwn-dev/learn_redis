import http from 'k6/http';
import { check, sleep } from 'k6';

// 테스트 설정
export const options = {
    stages: [
        { duration: '1s', target: 500 },  // 1초 만에 500명으로 폭증 (순간 부하)
        { duration: '10s', target: 500 }, // 10초 동안 500명 유지
        { duration: '1s', target: 0 },
    ],
};

export default function () {

    const userId = __VU; // 가상 유저 ID를 userId로 사용

    const url = `http://localhost:8080/purchase/v2/redis/1?userId=${userId}`;
    // 테스트할 API 주소

    const params = {
        headers: { 'Content-Type': 'application/json' },
    };

    const res = http.post(url, JSON.stringify({}), params);

    if(res.status !== 200 && res.status !== 400) {
        console.error(`Unexpected status code: ${res.status}, Body: ${res.body}`);
    }

    // 응답 코드가 200(성공) 또는 400(재고부족/중복 등 의도된 실패)인지 확인
    check(res, {
        '성공 (첫 구매)': (r) => r.status === 200,
        '차단 (중복 구매)': (r) => r.status === 400, // 서버에서 중복 시 400을 준다고 가정
    });
}