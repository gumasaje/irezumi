# HTTP Timeout과 수신 측 처리

HTTP 요청에서 timeout이 발생하면 요청 자체가 실패한 것이라고 생각했다. 그렇다면 송신 측에서 timeout이 발생했을 때 수신 측도 요청을 처리하지 않는 걸까?

이를 직접 확인하기 위해 간단한 Sender와 Receiver를 구현했다. Sender가 응답을 기다리다 먼저 timeout되는 상황을 만들고, 그 이후에도 Receiver가 요청을 처리하는지 확인했다.

## 실험 구성

Sender는 `/callback`으로 `POST` 요청을 한 번 보내고 최대 2초 동안 응답을 기다린다. HTTP body는 사용하지 않았다.

Receiver는 세 가지 모드로 실행했다.

| 모드 | 동작 |
| --- | --- |
| `normal` | 바로 처리한 뒤 `204` 응답 |
| `fail-before` | 처리하지 않고 `500` 응답 |
| `delay-before` | 5초 기다린 뒤 처리하고 `204` 응답 |

여기서 처리는 메모리의 카운터를 1 증가시키는 것으로 단순화했다.

`normal`과 `fail-before`는 각각 정상 처리와 처리 전 실패를 비교하기 위한 기준선이고, 실제로 확인하려는 상황은 `delay-before`다.

## 실험 결과

세 가지 모드의 실행 결과는 다음과 같았다.

| 모드 | Sender | Receiver |
| --- | --- | --- |
| `normal` | `204` 수신 | 처리 건수 1 |
| `fail-before` | `500` 수신 | 처리 건수 0 |
| `delay-before` | 약 2초 후 `HttpTimeoutException` | 약 5초 후 처리 건수 1 |

`delay-before`의 실제 로그는 다음과 같았다.

```text
Sender:   exception = HttpTimeoutException
Sender:   elapsedMillis = 2009

Receiver: callback 도착: 2026-09-28T08:29:33.617323Z
Receiver: 처리 시작: 2026-09-28T08:29:38.618620Z
Receiver: 요청 건수: 1
Receiver: 처리 완료: 2026-09-28T08:29:38.619668Z
Receiver: returningStatus = 204
```

시간 순서로 정리하면 다음과 같다.

```text
0초   Receiver가 요청 수신
 ↓
2초   Sender에서 timeout 발생
 ↓
5초   Receiver가 요청 처리
```

## 결과 해석

`delay-before`에서 Sender는 약 2초 뒤 `HttpTimeoutException`이 발생했지만, Receiver는 실행을 계속해 약 5초 뒤 카운터를 증가시켰다.

따라서 Sender에서 발생한 timeout은 **정해진 시간 안에 응답을 확인하지 못했다는 뜻**이며, 이것만으로 Receiver의 처리 여부를 판단할 수 없다.

```text
timeout
→ 제한 시간 안에 응답을 확인하지 못함
→ Receiver의 처리 여부는 알 수 없음
```

Receiver 로그의 `returningStatus = 204` 역시 Receiver가 `204` 응답을 보내려 시도했다는 기록일 뿐, Sender가 그 응답을 실제로 받았다는 의미는 아니다.

## 실험 범위

이번 실험은 같은 컴퓨터에서 `127.0.0.1`을 이용한 loopback 통신으로 진행했다. Receiver의 처리도 실제 비즈니스 로직이나 데이터 저장 대신 메모리 카운터 증가로 단순화했다.

따라서 이번 결과가 모든 timeout에서 Receiver가 요청을 처리한다는 의미는 아니며, 실제 네트워크나 외부 서버에서도 항상 같은 결과가 나온다고 볼 수는 없다.

이번에 확인한 것은 **Sender에서 timeout이 발생했다는 사실만으로는 Receiver의 처리 여부를 판단할 수 없다는 점**이다.

## 다음 실험

이번 실험에서 Sender는 timeout 이후 Receiver가 요청을 처리했는지 알 수 없었다.

이런 상황에서 같은 요청을 다시 보내면 어떻게 될까?

다음에는 Sender가 응답을 확인하지 못한 요청을 다시 전송했을 때 Receiver가 이를 다시 처리하는지 확인한다.