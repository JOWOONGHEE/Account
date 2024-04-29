<img src="https://capsule-render.vercel.app/api?type=waving&color=BDBDC8&height=150&section=header" />
<img src="https://capsule-render.vercel.app/api?type=waving&color=BDBDC8&height=150&section=footer" />
[![Top Langs](https://github-readme-stats.vercel.app/api/top-langs/?username=JIWOONGHEE)](https://github.com/anuraghazra/github-readme-stats)

Account
Spring boot 와 Java를 활용하여 Account(계좌 관리) 시스템을 만드는 프로젝트 과제

요구 사항
Spring boot 와 Java을 활용한다.
단위테스트를 작성하여 작성한 코드를 검증한다.
DB는 H2 DB를 활용한다.
DB를 접근하는 방법은 Spring data jpa를 활용한다.
Embedded redis를 활용한다.
API Request body와 Response body는 json 타입으로 표현한다.
각각의 API들은 각자의 요청과 응답 객체 구조를 갖습니다. (다만, 요청을 처리하다가 실패하는 경우의 응답은 공통된 구조를 갖도록 한다.)
프로젝트 소개
Account(계좌) 시스템은 사용자와 계좌 정보에 대한 거래 관리 기능을 제공하는 시스템이다.
구현의 편리를 위해 사용자 정보는 프로젝트 시작 시 자동으로 데이터가 입력 되도록 한다.
계좌 추가/해지/확인, 거래 생성/취소/확인 총 6 가지 API를 제공한다.
거래금액을 늘리거나 줄이는 과정에서 여러 쓰레드 혹은 인스턴스에서 같은 계좌에 접근할 경우 동시성 이슈로 인한 lost update 가 발생할 수 있으므로 이 부분 해결 필요.
각 API 명세서
계좌 관련 API
계좌 생성

파라미터 : 사용자 ID, 초기 잔액
결과
실패 : 사용자 없는 경우, 계좌가 10개(사용자당 최대 보유 기능 계좌)인 경우 실패 응답
성공 : 생성된 계좌번호 (10자리 랜덤 숫자), 사용자 아이디, 등록일시
[계좌생성 시 주의사항]
계좌 생성 시 계좌 번호는 10자리 정수로 구성되며, 기존에 동일 계좌 번호가 있는지 중복체크 해야 한다.
기본적으로 계좌번호는 순차 증가 방식으로 생성한다. (응용하는 방식으로는 계좌 번호를 랜덤 숫자 10자리로 구성하는 것도 가능)
계좌 해지

파라미터 : 사용자 ID, 계좌번호
결과
실패 : 사용자 없는 경우, 사용자 아이디와 계좌 소유주가 다른 경우, 계좌가 이미 해지 상태인 경우, 잔액이 있는 경우 실패 응답
성공 : 사용자 아이디, 계좌번호, 해지일시
계좌 확인

파라미터 : 사용자 ID
결과
실패 : 사용자가 없는 경우 실패 응답
성공 : (계좌번호, 잔액) 정보를 Json list 형식으로 응답
거래 관련 API
잔액 사용
파라미터 : 사용자 ID, 계좌 번호, 거래 금액
결과
실패 : 사용자가 없는 경우, 사용자 아이디와 계좌 소유주가 다른 경우, 계좌가 이미 해지 상태인 경우, 거래금액이 잔액보다 큰 경우, 거래금액이 너무 작거나 큰 경우
성공 : 계좌번호, transaction_result, transaction_id, 거래금액, 거래일시
잔액 사용 취소
파라미터 : transaction_id, 계좌번호, 거래금액
결과
실패 : 원거래 금액과 취소 금액이 다른 경우, 트랜잭션이 해당 계좌의 거래가 아닌 경우
성공 : 계좌 번호, transaction_result, transaction_id, 취소 거래금액, 거래 일시
거래 확인
파라미터 : transaction_id
결과
실패 : 해당 transaction_id 없는 경우
성공 : 계좌번호, 거래종류(잔액 사용, 잔액 사용 취소), transaction_result, transaction_id, 거래금액, 거래일시
성공 거래 뿐 아니라 실패한 거래도 거래 확인 할 수 있어야 한다.
