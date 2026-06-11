# Expedition 서버 관리자용 인게임 사용설명서

작성 기준일: 2026-06-05

## 서론: 현재 서버 개요

현재 Expedition 서버는 Velocity 프록시와 Paper 백엔드를 분리한 구조다. 외부 유저는 `25565` 포트의 Velocity로만 접속하고, 실제 게임 서버인 survival/lobby는 내부 주소 `127.0.0.1`에 묶여 있다. 던전은 별도 Velocity 서버가 아니라 survival Paper 내부의 `dungeon` 월드로 운영한다. Newworld 서버 폴더와 서비스는 존재하지만 현재 기본 상태는 비활성이다.

- 공개 진입점: Velocity `0.0.0.0:25565`
- Survival Paper: `/opt/minecraft/server`, 서비스 `minecraft`, 내부 포트 `25566`
- Lobby Paper: `/opt/minecraft/lobby`, 서비스 `lobby`, 내부 포트 `25567`
- Resource pack host: `resourcepack` 서비스, 포트 `8163`
- Newworld Paper: `/opt/minecraft/dungeon`, 서비스 `newworld`, 기본 비활성
- 공유 데이터: MariaDB 기반 LuckPerms, `/opt/minecraft/shared` 기반 커스텀 데이터

## 표기 규칙

- <font color="#16a34a"><b>확인됨</b></font>: 정보 조회 또는 이미 안정적으로 쓰는 명령어
- <font color="#d97706"><b>테스트 필요</b></font>: 서버 안에서 실제 플레이어/월드/GUI/리소스팩 동작 확인이 필요한 명령어
- <font color="#dc2626"><b>주의</b></font>: 돈, 데이터, 월드, NPC, 권한, 밴, 삭제, 전쟁 결과 등 운영 상태를 바꾸는 명령어. 테스트 서버나 백업 후 사용 권장

## 커스텀 플러그인 명령어

### Velocity / 네트워크 이동: LeeSeolProxy

- `/lobby` - 현재 접속자를 Velocity의 `lobby` 백엔드 서버로 이동한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/hub` - `/lobby` 별칭. 로비 서버로 이동한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/survival` - 현재 접속자를 Velocity의 `survival` 백엔드 서버로 이동한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/wild` - `/survival` 별칭. 야생 서버로 이동한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/servers` - Velocity에 등록된 서버 목록과 현재 접속 인원을 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/serverlist` - `/servers` 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/network` - `/servers` 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>

### 코어 기능: LeeSeolCore

- `/serverinfo` - 현재 Paper 서버명, 접속자 수, 최대 인원, 마인크래프트 버전을 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/leeseolcore reload` - LeeSeolCore 설정, 런치패드, 포탈, 차원 제한 설정을 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/lscore reload` - `/leeseolcore reload`와 같은 관리자 reload 명령이다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/leeseolcore launchpad set <id> [forward] [upward] [cooldownSeconds]` - 현재 위치 또는 바라보는 블록을 런치패드로 등록한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/leeseolcore launchpad list` - 등록된 런치패드 목록을 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/leeseolcore launchpad remove <id>` - 등록된 런치패드를 삭제한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/leeseolcore portal pos1` - 포탈 cuboid 영역의 첫 번째 좌표를 현재 위치로 저장한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/leeseolcore portal pos2` - 포탈 cuboid 영역의 두 번째 좌표를 현재 위치로 저장한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/leeseolcore portal create <id> <targetServer> [cooldownSeconds]` - WorldEdit 선택 영역 또는 pos1/pos2 영역을 Velocity 서버 이동 포탈로 등록한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/leeseolcore portal list` - 등록된 Velocity 이동 포탈을 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/leeseolcore portal remove <id>` - 등록된 Velocity 이동 포탈을 삭제한다. 상태: <font color="#dc2626"><b>주의</b></font>

### 경제 / 상점 / Shift+F 메뉴: LeeSeolEconomy

- `/won` - 자신의 보유 원화를 확인한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/won balance [player]` - 원화 잔액을 확인한다. 다른 플레이어 조회는 관리자 권한이 필요하다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/won bal [player]` - `/won balance` 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/won money [player]` - `/won balance` 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/won pay <player> <amount>` - 다른 접속자에게 원화를 송금한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/won give <player> <amount>` - 관리자가 대상에게 원화를 지급한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/won take <player> <amount>` - 관리자가 대상의 원화를 차감한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/won set <player> <amount>` - 관리자가 대상의 원화 잔액을 지정값으로 설정한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/won reload` - 경제/상점 설정을 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/shop [shopId]` - 상점 GUI를 연다. shopId가 없으면 기본 상점을 연다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/wonnpc create <id> <shopId> [skin:<playerName>] [displayName]` - 현재 위치에 상점 NPC를 생성한다. 플레이어 스킨 지정 가능. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/wonnpc skin <id> <playerName|none>` - 상점 NPC의 스킨을 변경하거나 제거한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/wonnpc remove <id>` - 상점 NPC를 제거한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/wonnpc list` - 등록된 상점 NPC 목록을 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/servermenu` - Shift+F와 동일한 서버 이동 GUI를 연다. dungeon 월드에서는 기본 차단된다. 상태: <font color="#d97706"><b>테스트 필요</b></font>

### 경매: LeeSeolAuction

- `/auction` - 경매 메인 GUI를 연다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/ah` - `/auction` 별칭. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/auc` - `/auction` 별칭. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/auction submit` - 유저가 경매 후보 아이템을 등록하는 GUI를 연다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/auction admin` - 관리자용 경매 후보 선택 GUI를 연다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/auction open <lotId> [startingBid] [bidIncrement]` - 선택한 후보 아이템으로 경매를 시작한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/auction increment <amount>` - 현재 경매의 기본 입찰 상승폭을 변경한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/auction setincrement <amount>` - `/auction increment`와 같은 기능. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/auction end` - 현재 경매를 종료하고 최고 입찰자를 낙찰 처리한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/auction reload` - 경매 설정을 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>

### 던전: LeeSeolDungeon

- `/dungeon reload` - 던전 월드, 포탈, 보호, 루트 상자 설정을 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/dungeon enter` - 관리자를 내부 `dungeon` 월드로 즉시 이동시킨다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/dungeon exit` - 관리자를 survival 반환 위치로 이동시킨다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/dungeon portal pos1` - 던전 포탈 영역 첫 번째 좌표를 저장한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/dungeon portal pos2` - 던전 포탈 영역 두 번째 좌표를 저장한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/dungeon portal create <id> <targetWorld|return> [cooldownSeconds]` - survival 내부 다중월드 포탈을 생성한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/dungeon portal list` - 던전 포탈 목록을 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/dungeon portal remove <id>` - 던전 포탈을 삭제한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/dungeon chest table <tableId>` - 루트 테이블 GUI 편집기를 연다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/dungeon chest addspot <id> <tableId> [chance] [respawnSeconds]` - 현재 위치를 확률형 루트 상자 스폰 지점으로 등록한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/dungeon chest list` - 루트 상자 스폰 지점 목록을 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/dungeon chest spawn <id>` - 지정 루트 상자를 즉시 생성한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/dungeon chest roll` - 등록된 루트 상자 스폰 확률을 즉시 계산한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/dungeon chest removespot <id>` - 루트 상자 스폰 지점을 삭제한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/lsdungeon` - `/dungeon` 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>

### 로비 보호: LeeSeolLobby

- `/lobbysetspawn` - 현재 위치를 로비 고정 스폰 위치로 저장한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>

### 파티 / 국가 / 클레임 / 전쟁: LeeSeolTown

- `/party create <name>` - 파티를 생성한다. 유저는 하나의 파티에만 소속 가능하다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party invite <player>` - 플레이어에게 파티 초대를 보낸다. 수락/거절 버튼 메시지와 연동된다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party accept <party>` - 받은 파티 초대를 수락한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party deny <party>` - 받은 파티 초대를 거절한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party join <party>` - 공개 참가 흐름이 허용된 경우 파티 참가를 시도한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party leave` - 현재 파티/국가에서 나간다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party disband` - 파티장이 파티를 해산한다. 확인 재입력 흐름이 적용된다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party transfer <player>` - 파티장 권한을 다른 파티원에게 위임한다. 확인 재입력 흐름이 적용된다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party kick <player>` - 파티장이 파티원을 강제 추방한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party claim` - 현재 청크를 국가 영토로 구매한다. 국가와 신호기 조건이 필요하다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party unclaim` - 현재 청크의 국가 영토를 해제한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party info [party]` - 자신 또는 지정 파티 정보를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/party me` - 자신의 파티/국가 소속 정보를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/party status` - `/party me`와 같은 소속 확인 명령. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/party chat <global|party|nation>` - 개인 채팅 모드를 전체/파티/국가로 전환한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/tc [message]` - 파티 채팅 전송 또는 파티 채팅 토글. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/pc [message]` - `/tc` 별칭. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/nc [message]` - 국가 채팅 전송 또는 국가 채팅 토글. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party nation create <name> <republic|empire> [party...]` - 파티를 공화국/제국으로 승급하거나 여러 파티를 포함한 국가를 생성한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party nation disband` - 국가를 해산한다. 확인 재입력 흐름이 적용된다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party nation pvp <on|off>` - 국가 내부 PVP 허용 여부를 설정한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party nation build <on|off>` - 비국가원의 블록 설치/파괴 보호를 켜거나 끈다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party nation treasury` - 국가 금고 정보를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/party nation deposit <amount>` - 자신의 원화를 국가 금고에 입금한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party federation create <name> <party1> [party2] [party3] [...]` - 관리자가 연방을 생성한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party federation disband` - 연방/국가를 해산한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party war declare <nation>` - 상대 국가에 전쟁을 선포한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party war accept <attackerNation>` - 상대 국가의 전쟁을 수락한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party war surrender <enemyNation>` - 전쟁에서 항복한다. 카르마/배상금/보호 해제와 연결된다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party war release <enemyNation>` - 10분 보호를 국가장이 조기 해제한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party war paydebt` - 항복 배상금 미납금을 납부한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party war finish <winnerNation> <loserNation>` - 관리자가 전쟁 결과를 강제 종료 처리한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/party reload` - 파티/국가 설정과 데이터를 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/town` - `/party` 루트 명령 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/village` - `/party` 루트 명령 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/towny` - `/party` 루트 명령 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>

### 홀로그램: LeeSeolHologram

- `/holo create <id> [text]` - 현재 위치에 홀로그램을 생성한다. RGB 형식 `&#RRGGBB` 또는 `<#RRGGBB>` 사용 가능. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/holo addline <id> <text>` - 홀로그램 마지막 줄에 텍스트를 추가한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/holo setline <id> <line> <text>` - 지정 줄 내용을 변경한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/holo insertline <id> <line> <text>` - 지정 위치에 줄을 삽입한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/holo removeline <id> <line>` - 지정 줄을 삭제한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/holo movehere <id>` - 홀로그램을 현재 위치로 이동한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/holo spacing <id> <value>` - 줄 간격을 변경한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/holo info <id>` - 홀로그램 상세 정보를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/holo list` - 등록된 홀로그램 목록을 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/holo delete <id>` - 홀로그램을 삭제한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/holo reload` - 홀로그램 설정/데이터를 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/hologram` - `/holo` 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/lholo` - `/holo` 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>

### 전투 / 시체 / PVP 포인트: LeeSeolCombat

- `/combat status` - 전투 태그 수, 로그아웃 NPC 수, 관전 NPC 설정, PVP 기록 수를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/combat reload` - 전투 설정을 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/combat force <player1> <player2>` - 관리자가 두 플레이어를 강제로 전투 상태에 넣는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/combat spectatorclone <on|off>` - 관전 모드 전환 시 NPC 생성 기능을 켜거나 끈다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/combat pvp [player]` - 자신 또는 대상의 PVP 포인트와 처치 수를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/combat pvppoints <set|add|take> <player> <amount>` - 관리자가 PVP 포인트를 조정한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/leeseolcombat` - `/combat` 루트 명령 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>

### 아이템 청소: LeeSeolCleanup

- `/leeseolcleanup status` - 드랍 아이템 청소 활성 여부, 주기, 대상 월드를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/leeseolcleanup run` - 대상 월드의 드랍 아이템을 즉시 청소한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/leeseolcleanup reload` - 아이템 청소 설정을 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/cleanup` - `/leeseolcleanup` 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/itemcleanup` - `/leeseolcleanup` 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>

### 랭크: LeeSeolRanks

- `/rank` - 자신의 현재 랭크, 킬 수, 다음 랭크 조건을 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/rank <player>` - 지정 플레이어의 랭크 정보를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/rank progress` - 다음 랭크 승급 조건 달성도를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/rank requirements` - D/C/B/A/S 승급 조건 목록을 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/rank up` - `/rankup`과 동일하게 승급을 시도한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/rank rankup` - `/rankup`과 동일하게 승급을 시도한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/rankup` - 조건을 만족하면 다음 랭크로 승급하고 기존 킬 카운트를 초기화한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/leeseolrank status` - 랭크 데이터에 저장된 플레이어 수를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/leeseolrank reload` - 랭크 설정과 권한 동기화 설정을 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/leeseolrank set <player> <PLAYER|D|C|B|A|S|ADMIN|DEV>` - 관리자가 대상 랭크를 직접 지정한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/leeseolrank dev <player> <on|off>` - 대상에게 DEV 랭크를 부여하거나 PLAYER로 되돌린다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/lsrank` - `/leeseolrank` 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/ranks` - `/rank` 별칭. 상태: <font color="#16a34a"><b>확인됨</b></font>

### 튜토리얼 / 퀘스트: LeeSeolQuest

- `/quest` - 퀘스트 GUI를 연다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/quests` - `/quest` 별칭. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/quest start <id>` - 지정 퀘스트를 시작한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/quest progress` - 현재 퀘스트 진행도를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/quest abandon` - 현재 퀘스트를 포기한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/tutorial start` - 기본 튜토리얼 퀘스트를 시작한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/tutorial skip` - 튜토리얼을 완료 처리 또는 스킵 처리한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/tutorial reset <player>` - 관리자가 대상의 튜토리얼/퀘스트 데이터를 초기화한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/lsquest reload` - 퀘스트 설정을 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/lsquest set <player> <questId> <stage>` - 온라인 대상의 퀘스트 단계를 강제로 지정한다. 상태: <font color="#dc2626"><b>주의</b></font>
- `/lsquest advance <player>` - 온라인 대상의 현재 퀘스트를 한 단계 진행한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/lsquest objective <player> <type> [target]` - 온라인 대상에게 퀘스트 목표 진행도를 1 추가한다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/lsquest reset <player>` - 대상의 퀘스트 데이터를 초기화한다. 상태: <font color="#dc2626"><b>주의</b></font>

### 초반 활동 보상: LeeSeolJobs

- `/activity` - 광질/농사/낚시/탐험 일일 활동 보상 통계를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/jobs` - `/activity`와 같은 기존 호환 명령어다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/jobs stats` - `/activity`와 동일하게 자신의 통계를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/jobs top` - 현재 코드에서는 탭완성만 존재하며 별도 랭킹 출력은 미구현 상태다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/lsjobs status` - Jobs 데이터에 저장된 플레이어 수를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/lsjobs reload` - 활동 보상/제한 설정을 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/lsjobs stats <player>` - 대상의 일일/누적 활동 통계를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/lsjobs reset <player>` - 대상의 활동 통계를 초기화한다. 상태: <font color="#dc2626"><b>주의</b></font>

### 제작 / 가공 / 수리: LeeSeolCrafting

- `/craftmenu` - 기본 제작 GUI를 연다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/forge` - 장비/무기 제작 GUI를 연다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/process` - 광물/재료 가공 GUI를 연다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/disassemble` - 아이템 분해 GUI를 연다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/repair` - 원화 기반 수리 확인 GUI를 연다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/lscrafting status` - 등록된 제작 레시피 수를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/lscrafting reload` - 제작 설정과 레시피를 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/lscrafting recipe list` - 등록된 레시피 ID 목록을 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/lscrafting recipe give <player> <recipeId>` - 관리자가 레시피 결과 아이템을 대상에게 지급한다. 상태: <font color="#dc2626"><b>주의</b></font>

### HUD / 나침반 / 체력 표시: LeeSeolHUD

- `/hud` - 자신의 HUD 기능 상태를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/hud compass on` - ItemsAdder 리소스팩 기반 보스바 나침반 HUD를 켠다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/hud compass off` - 나침반 HUD를 끈다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/hud target on` - 대상 체력 HUD를 켠다. 현재 기본 목표는 TAB 아래이름 체력 표시다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/hud target off` - 대상 체력 HUD를 끈다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/compasshud on` - 나침반 HUD를 직접 켠다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/compasshud off` - 나침반 HUD를 직접 끈다. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/lshud status` - HUD 설정 활성 여부와 온라인 수를 표시한다. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/lshud reload` - HUD 설정을 다시 읽는다. 상태: <font color="#d97706"><b>테스트 필요</b></font>

## 외부 플러그인 루트 명령어 부록

아래 목록은 live VM의 survival/lobby/velocity `plugins/*.jar` descriptor에서 추출한 루트 명령어다. 외부 플러그인의 세부 하위 명령어는 플러그인 자체 help와 설정에 따라 달라질 수 있다.

### AdvancedEnchantments

- `/AdvancedEnchantments` - Main AE Command 사용법 원문: `/AdvancedEnchantments`. 별칭 원문: `[ ae, customenchants ]`. 적용 서버: `survival`. 상태: <font color="#d97706"><b>테스트 필요</b></font>

### Citizens

- `/citizens` - Administration commands 적용 서버: `survival`. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/npc` - Basic commands for all NPC-related things 적용 서버: `survival`. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/template` - Template commands 별칭 원문: `[tpl]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/trait` - Trait commands 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/waypoints` - Waypoint commands 별칭 원문: `[wp]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>

### Essentials

- `/afk` - Marks you as away-from-keyboard. 사용법 원문: `/<command> [player/message...]`. 별칭 원문: `[eafk,away,eaway]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/antioch` - A little surprise for operators. 사용법 원문: `/<command> [message]`. 별칭 원문: `[eantioch,grenade,egrenade,tnt,etnt]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/anvil` - Opens up an Anvil. 사용법 원문: `/<command>`. 별칭 원문: `[eanvil]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/back` - Teleports you to your location prior to tp/spawn/warp. 사용법 원문: `/<command> [player]`. 별칭 원문: `[eback,return,ereturn]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/backup` - Runs the backup if configured. 사용법 원문: `/<command>`. 별칭 원문: `[ebackup]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/balance` - States the current balance of a player. 사용법 원문: `/<command> [player]`. 별칭 원문: `[bal,ebal,ebalance,money,emoney]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/balancetop` - Gets the top balance values. 사용법 원문: `/<command> [page]`. 별칭 원문: `[ebalancetop,baltop,ebaltop]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/ban` - Bans a player. 사용법 원문: `/<command> <player> [reason]`. 별칭 원문: `[eban]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/banip` - Bans an IP address. 사용법 원문: `/<command> <address> [reason]`. 별칭 원문: `[ebanip]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/beezooka` - Throw an exploding bee at your opponent. 사용법 원문: `/<command>`. 별칭 원문: `[ebeezooka,beecannon,ebeecannon]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/bigtree` - Spawn a big tree where you are looking. 사용법 원문: `/<command> <tree|redwood|jungle|darkoak>`. 별칭 원문: `[ebigtree,largetree,elargetree]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/book` - Allows reopening and editing of sealed books. 사용법 원문: `/<command> [title|author [name]]`. 별칭 원문: `[ebook]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/bottom` - Teleport to the lowest block at your current position. 사용법 원문: `/<command>`. 별칭 원문: `[ ebottom ]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/break` - Breaks the block you are looking at. 사용법 원문: `/<command>`. 별칭 원문: `[ebreak]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/broadcast` - Broadcasts a message to the entire server. 사용법 원문: `/<command> <msg>`. 별칭 원문: `[bc,ebc,bcast,ebcast,ebroadcast,shout,eshout]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/broadcastworld` - Broadcasts a message to a world. 사용법 원문: `/<command> <world> <msg>`. 별칭 원문: `[bcw,ebcw,bcastw,ebcastw,ebroadcastworld,shoutworld,eshoutworld]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/burn` - Set a player on fire. 사용법 원문: `/<command> <player> <seconds>`. 별칭 원문: `[eburn]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/cartographytable` - Opens up a cartography table. 사용법 원문: `/<command>`. 별칭 원문: `[ecartographytable, carttable, ecarttable]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/clearinventory` - Clear all items in your inventory. 사용법 원문: `/<command> [player|*] [item[:<data>]|*|**] [amount]`. 별칭 원문: `[ci,eci,clean,eclean,clear,eclear,clearinvent,eclearinvent,eclearinventory]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/clearinventoryconfirmtoggle` - Toggles whether you are prompted to confirm inventory clears. 사용법 원문: `/<command>`. 별칭 원문: `[eclearinventoryconfirmtoggle, clearinventoryconfirmoff, eclearinventoryconfirmoff, clearconfirmoff, eclearconfirmoff, clearconfirmon, eclearconfirmon, clearconfirm, eclearconfirm]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/compass` - Describes your current bearing. 사용법 원문: `/<command>`. 별칭 원문: `[ecompass,direction,edirection]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/condense` - Condenses items into a more compact blocks. 사용법 원문: `/<command> [itemname]`. 별칭 원문: `[econdense,compact,ecompact,blocks,eblocks,toblocks,etoblocks]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/createkit` - Create a kit in game! 사용법 원문: `/<command> <kitname> <delay>`. 별칭 원문: `[kitcreate,createk,kc,ck]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/customtext` - Allows you to create custom text commands. 사용법 원문: `/<alias> - Define in bukkit.yml`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/delhome` - Removes a home. 사용법 원문: `/<command> [player:]<name>`. 별칭 원문: `[edelhome,remhome,eremhome,rmhome,ermhome]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/deljail` - Removes a jail. 사용법 원문: `/<command> <jailname>`. 별칭 원문: `[edeljail,remjail,eremjail,rmjail,ermjail]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/delkit` - Deletes the specified kit. 사용법 원문: `/<command> <kit>`. 별칭 원문: `[edelkit,remkit,eremkit,rmkit,ermkit,deletekit,edeletekit]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/delwarp` - Deletes the specified warp. 사용법 원문: `/<command> <warp>`. 별칭 원문: `[edelwarp,remwarp,eremwarp,rmwarp,ermwarp]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/depth` - States current depth, relative to sea level. 사용법 원문: `/<command>`. 별칭 원문: `[edepth,height,eheight]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/disposal` - Opens a portable disposal menu. 사용법 원문: `/<command>`. 별칭 원문: `[edisposal,trash,etrash]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/eco` - Manages the server economy. 사용법 원문: `/<command> <give|take|set|reset> <player> <amount>`. 별칭 원문: `[eeco,economy,eeconomy]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/editsign` - Edits a sign in the world. 사용법 원문: `/<command> <set/clear/copy/paste> [line number] [text]`. 별칭 원문: `[sign, esign, eeditsign]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/enchant` - Enchants the item the user is holding. 사용법 원문: `/<command> <enchantmentname> [level]`. 별칭 원문: `[eenchant,enchantment,eenchantment]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/enderchest` - Lets you see inside an enderchest. 사용법 원문: `/<command> [player]`. 별칭 원문: `[echest,eechest,eenderchest,endersee,eendersee,ec,eec]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/essentials` - Reloads essentials. 사용법 원문: `/<command>`. 별칭 원문: `[eessentials, ess, eess, essversion]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/exp` - Give, set, reset, or look at a players experience. 사용법 원문: `/<command> [reset|show|set|give] [playername [amount]]`. 별칭 원문: `[eexp,xp]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/ext` - Extinguish players. 사용법 원문: `/<command> [player]`. 별칭 원문: `[eext,extinguish,eextinguish]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/feed` - Satisfy the hunger. 사용법 원문: `/<command> [player]`. 별칭 원문: `[eat,eeat,efeed]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/fireball` - Throw a fireball or other assorted projectiles. 사용법 원문: `/<command> [fireball|small|large|arrow|skull|egg|snowball|expbottle|dragon|splashpotion|lingeringpotion|trident] [speed]`. 별칭 원문: `[efireball,fireentity,efireentity,fireskull,efireskull]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/firework` - Allows you to modify a stack of fireworks. 사용법 원문: `/<command> <<meta param>|power [amount]|clear|fire [amount]>`. 별칭 원문: `[efirework]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/fly` - Take off, and soar! 사용법 원문: `/<command> [player] [on|off]`. 별칭 원문: `[efly]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/gamemode` - Change player gamemode. 사용법 원문: `/<command> <survival|creative|adventure|spectator> [player]`. 별칭 원문: `[adventure,eadventure,adventuremode,eadventuremode,creative,ecreative,eecreative,creativemode,ecreativemode,egamemode,gm,egm,gma,egma,gmc,egmc,gms,egms,gmt,egmt,survival,esurvival,survivalmode,esurvivalmode,gmsp,sp,egmsp,spec,spectator]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/gc` - Reports memory, uptime and tick info. 사용법 원문: `/<command>`. 별칭 원문: `[lag,elag,egc,mem,emem,memory,ememory,uptime,euptime,tps,etps,entities,eentities]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/getpos` - Get your current coordinates or those of a player. 사용법 원문: `/<command> [player]`. 별칭 원문: `[coords,egetpos,position,eposition,whereami,ewhereami,getlocation,egetlocation,getloc,egetloc]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/give` - Give a player an item. 사용법 원문: `/<command> <player> <item|numeric> [amount [itemmeta...]]`. 별칭 원문: `[egive]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/god` - Enables your godly powers. 사용법 원문: `/<command> [player] [on|off]`. 별칭 원문: `[egod,godmode,egodmode,tgm,etgm]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/grindstone` - Opens up a grindstone. 사용법 원문: `/<command>`. 별칭 원문: `[egrindstone]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/hat` - Get some cool new headgear. 사용법 원문: `/<command> [remove]`. 별칭 원문: `[ehat,head,ehead]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/heal` - Heals you or the given player. 사용법 원문: `/<command> [player]`. 별칭 원문: `[eheal]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/help` - Views a list of available commands. 사용법 원문: `/<command> [search term] [page]`. 별칭 원문: `[ehelp]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/helpop` - Message online admins. 사용법 원문: `/<command> <message>`. 별칭 원문: `[ac,eac,amsg,eamsg,ehelpop]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/home` - Teleport to your home. 사용법 원문: `/<command> [player:][name]`. 별칭 원문: `[ehome,homes,ehomes]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/ice` - Cools a player off. 사용법 원문: `/<command> [player]`. 별칭 원문: `[eice, efreeze]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/ignore` - Ignore or unignore other players. 사용법 원문: `/<command> <player>`. 별칭 원문: `[eignore,unignore,eunignore,delignore,edelignore,remignore,eremignore,rmignore,ermignore]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/info` - Shows information set by the server owner. 사용법 원문: `/<command> [chapter] [page]`. 별칭 원문: `[about,eabout,ifo,eifo,einfo,inform,einform,news,enews]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/invsee` - See the inventory of other players. 사용법 원문: `/<command> <player>`. 별칭 원문: `[einvsee]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/item` - Spawn an item. 사용법 원문: `/<command> <item|numeric> [amount [itemmeta...]]`. 별칭 원문: `[i,eitem,ei]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/itemdb` - Searches for an item. 사용법 원문: `/<command> <item>`. 별칭 원문: `[dura,edura,durability,edurability,eitemdb,itemno,eitemno]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/itemlore` - Edit the lore of an item. 사용법 원문: `/<command> <add/set/clear> [text/line] [text]`. 별칭 원문: `[lore, elore, ilore, eilore, eitemlore]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/itemname` - Names an item. 사용법 원문: `/<command> [name]`. 별칭 원문: `[iname, einame, eitemname, itemrename, irename, eitemrename, eirename]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/jailedplayers` - List all jailed players. 사용법 원문: `/<command>`. 별칭 원문: `[ejailedplayers, ejailed, ejp]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/jails` - List all jails. 사용법 원문: `/<command>`. 별칭 원문: `[ejails]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/jump` - Jumps to the nearest block in the line of sight. 사용법 원문: `/<command>`. 별칭 원문: `[j,ej,ejump,jumpto,ejumpto]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/kick` - Kicks a specified player with a reason. 사용법 원문: `/<command> <player> [reason]`. 별칭 원문: `[ekick]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/kickall` - Kicks all players off the server except the issuer. 사용법 원문: `/<command> [reason]`. 별칭 원문: `[ekickall]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/kill` - Kills specified player. 사용법 원문: `/<command> <player>`. 별칭 원문: `[ekill]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/kit` - Obtains the specified kit or views all available kits. 사용법 원문: `/<command> [kit] [player]`. 별칭 원문: `[ekit,kits,ekits]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/kitreset` - Resets the cooldown on the specified kit. 사용법 원문: `/<command> <kit> [player]`. 별칭 원문: `[ekitreset, kitr, ekitr, resetkit, eresetkit]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/kittycannon` - Throw an exploding kitten at your opponent. 사용법 원문: `/<command>`. 별칭 원문: `[ekittycannon]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/lightning` - The power of Thor. Strike at cursor or player. 사용법 원문: `/<command> [player] [power]`. 별칭 원문: `[elightning,shock,eshock,smite,esmite,strike,estrike,thor,ethor]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/list` - List all online players. 사용법 원문: `/<command> [group]`. 별칭 원문: `[elist,online,eonline,playerlist,eplayerlist,plist,eplist,who,ewho]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/loom` - Opens up a loom. 사용법 원문: `/<command>`. 별칭 원문: `[eloom]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/mail` - Manages inter-player, intra-server mail. 사용법 원문: `/<command> [read|clear|clear [number]|clear <player> [number]|send [to] [message]|sendtemp [to] [expire time] [message]|sendall [message]]`. 별칭 원문: `[email,eemail,memo,ememo]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/me` - Describes an action in the context of the player. 사용법 원문: `/<command> <description>`. 별칭 원문: `[action,eaction,describe,edescribe,eme]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/more` - Fills the item stack in hand to specified amount, or to maximum size if none is specified. 사용법 원문: `/<command> [amount]`. 별칭 원문: `[emore]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/motd` - Views the Message Of The Day. 사용법 원문: `/<command> [chapter] [page]`. 별칭 원문: `[emotd]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/msg` - Sends a private message to the specified player. 사용법 원문: `/<command> <to> <message>`. 별칭 원문: `[w,m,t,pm,emsg,epm,tell,etell,whisper,ewhisper]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/msgtoggle` - Blocks receiving all private messages. 사용법 원문: `/<command> [player] [on|off]`. 별칭 원문: `[emsgtoggle]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/mute` - Mutes or unmutes a player. 사용법 원문: `/<command> <player> [datediff] [reason]`. 별칭 원문: `[emute,silence,esilence,unmute,eunmute]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/near` - Lists the players near by or around a player. 사용법 원문: `/<command> [playername] [radius]`. 별칭 원문: `[enear,nearby,enearby]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/nick` - Change your nickname or that of another player. 사용법 원문: `/<command> [player] <nickname|off>`. 별칭 원문: `[enick,nickname,enickname]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/nuke` - May death rain upon them. 사용법 원문: `/<command> [player]`. 별칭 원문: `[enuke]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/pay` - Pays another player from your balance. 사용법 원문: `/<command> <player> <amount>`. 별칭 원문: `[epay]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/payconfirmtoggle` - Toggles whether you are prompted to confirm payments. 사용법 원문: `/<command>`. 별칭 원문: `[epayconfirmtoggle, payconfirmoff, epayconfirmoff, payconfirmon, epayconfirmon, payconfirm, epayconfirm]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/paytoggle` - Toggles whether you are accepting payments. 사용법 원문: `/<command> [player]`. 별칭 원문: `[epaytoggle, payoff, epayoff, payon, epayon]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/ping` - Pong! 사용법 원문: `/<command>`. 별칭 원문: `[echo,eecho,eping,pong,epong]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/playtime` - Shows a player's time played in game 사용법 원문: `/<command> [player]`. 별칭 원문: `[eplaytime]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/potion` - Adds custom potion effects to a potion. 사용법 원문: `/<command> <clear|apply|effect:<effect> power:<power> duration:<duration>>`. 별칭 원문: `[epotion,elixer,eelixer]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/powertool` - Assigns a command to the item in hand. 사용법 원문: `/<command> [l:|a:|r:|c:|d:][command] [arguments] - {player} can be replaced by name of a clicked player.`. 별칭 원문: `[epowertool,pt,ept]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/powertoollist` - Lists all current powertools. 사용법 원문: `/<command>`. 별칭 원문: `[epowertoollist,ptlist,eptlist]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/powertooltoggle` - Enables or disables all current powertools. 사용법 원문: `/<command>`. 별칭 원문: `[epowertooltoggle,ptt,eptt,pttoggle,epttoggle]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/ptime` - Adjust player's client time. Add @ prefix to fix. 사용법 원문: `/<command> [list|reset|day|night|dawn|17:30|4pm|4000ticks] [player|*]`. 별칭 원문: `[playertime,eplayertime,eptime]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/pweather` - Adjust a player's weather 사용법 원문: `/<command> [list|reset|storm|sun|clear] [player|*]`. 별칭 원문: `[playerweather,eplayerweather,epweather]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/r` - Quickly reply to the last player to message you. 사용법 원문: `/<command> <message>`. 별칭 원문: `[er,reply,ereply]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/realname` - Displays the username of a user based on nick. 사용법 원문: `/<command> <nickname>`. 별칭 원문: `[erealname]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/recipe` - Displays how to craft items. 사용법 원문: `/<command> <item> [number]`. 별칭 원문: `[formula,eformula,method,emethod,erecipe,recipes,erecipes]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/remove` - Removes entities in your world. 사용법 원문: `/<command> <all|tamed|named|drops|arrows|boats|minecarts|xp|paintings|itemframes|endercrystals|monsters|animals|ambient|mobs|[mobType]> [radius|world]`. 별칭 원문: `[eremove,butcher,ebutcher,killall,ekillall,mobkill,emobkill]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/renamehome` - Renames a home. 사용법 원문: `/<command> <[player:]name> <new name>`. 별칭 원문: `[erenamehome]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/repair` - Repairs the durability of one or all items. 사용법 원문: `/<command> [hand|all]`. 별칭 원문: `[fix,efix,erepair]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/rest` - Rests you or the given player. 사용법 원문: `/<command> [player]`. 별칭 원문: `[erest]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/rtoggle` - Change whether the recipient of the reply is last recipient or last sender 사용법 원문: `/<command> [player] [on|off]`. 별칭 원문: `[ertoggle, replytoggle, ereplytoggle]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/rules` - Views the server rules. 사용법 원문: `/<command> [chapter] [page]`. 별칭 원문: `[erules]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/seen` - Shows the last logout time of a player. 사용법 원문: `/<command> <playername>`. 별칭 원문: `[eseen, ealts, alts]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/sell` - Sells the item currently in your hand. 사용법 원문: `/<command> <<itemname>|<id>|hand|inventory|blocks> [amount]`. 별칭 원문: `[esell]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/sethome` - Set your home to your current location. 사용법 원문: `/<command> [[player:]name]`. 별칭 원문: `[esethome,createhome,ecreatehome]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/setjail` - Creates a jail where you specified named [jailname]. 사용법 원문: `/<command> <jailname>`. 별칭 원문: `[esetjail,createjail,ecreatejail]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/settpr` - Set the random teleport location and parameters. 사용법 원문: `/<command> [center|minrange|maxrange] [value]`. 별칭 원문: `[esettpr, settprandom, esettprandom]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/setwarp` - Creates a new warp. 사용법 원문: `/<command> <warp>`. 별칭 원문: `[createwarp,ecreatewarp,esetwarp]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/setworth` - Set the sell value of an item. 사용법 원문: `/<command> [itemname|id] <price>`. 별칭 원문: `[esetworth]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/showkit` - Show contents of a kit. 사용법 원문: `/<command> <kitname>`. 별칭 원문: `[kitpreview,preview,kitshow]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/skull` - Set the owner of a player skull 사용법 원문: `/<command> [owner] [player]`. 별칭 원문: `[eskull, playerskull, eplayerskull, head, ehead]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/smithingtable` - Opens up a smithing table. 사용법 원문: `/<command>`. 별칭 원문: `[esmithingtable, smithtable, esmithtable]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/socialspy` - Toggles if you can see msg/mail commands in chat. 사용법 원문: `/<command> [player] [on|off]`. 별칭 원문: `[esocialspy]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/spawner` - Change the mob type of a spawner. 사용법 원문: `/<command> <mob> [delay]`. 별칭 원문: `[changems,echangems,espawner,mobspawner,emobspawner]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/spawnmob` - Spawns a mob. 사용법 원문: `/<command> <mob>[:data][,<mount>[:data]] [amount] [player]`. 별칭 원문: `[mob,emob,spawnentity,espawnentity,espawnmob]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/speed` - Change your speed limits. 사용법 원문: `/<command> [type] <speed> [player]`. 별칭 원문: `[flyspeed,eflyspeed,fspeed,efspeed,espeed,walkspeed,ewalkspeed,wspeed,ewspeed]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/stonecutter` - Opens up a stonecutter. 사용법 원문: `/<command>`. 별칭 원문: `[estonecutter]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/sudo` - Make another user perform a command. 사용법 원문: `/<command> <player> <command [args]>`. 별칭 원문: `[esudo]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/suicide` - Causes you to perish. 사용법 원문: `/<command>`. 별칭 원문: `[esuicide]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tempban` - Temporary ban a user. 사용법 원문: `/<command> <playername> <datediff> [reason]`. 별칭 원문: `[etempban]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/tempbanip` - Temporarily ban an IP Address. 사용법 원문: `/<command> <playername> <datediff> [reason]`. 별칭 원문: `[etempbanip]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/thunder` - Enable/disable thunder. 사용법 원문: `/<command> <true/false> [duration]`. 별칭 원문: `[ethunder]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/time` - Display/Change the world time. Defaults to current world. 사용법 원문: `/<command> [set|add] [day|night|dawn|17:30|4pm|4000ticks] [worldname|all]`. 별칭 원문: `[day,eday,night,enight,etime]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/togglejail` - Jails/Unjails a player, TPs them to the jail specified. 사용법 원문: `/<command> <player> <jailname> [datediff]`. 별칭 원문: `[jail,ejail,tjail,etjail,etogglejail,unjail,eunjail]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/top` - Teleport to the highest block at your current position. 사용법 원문: `/<command>`. 별칭 원문: `[etop]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tp` - Teleport to a player. 사용법 원문: `/<command> <player> [otherplayer]`. 별칭 원문: `[tele,etele,teleport,eteleport,etp,tp2p,etp2p]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpa` - Request to teleport to the specified player. 사용법 원문: `/<command> <player>`. 별칭 원문: `[call,ecall,etpa,tpask,etpask]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpaall` - Requests all players online to teleport to you. 사용법 원문: `/<command> <player>`. 별칭 원문: `[etpaall]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpacancel` - Cancel all outstanding teleport requests. Specify [player] to cancel requests with them. 사용법 원문: `/<command> [player]`. 별칭 원문: `[etpacancel]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpaccept` - Accepts teleport requests. 사용법 원문: `/<command> [player|*]`. 별칭 원문: `[etpaccept,tpyes,etpyes]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpahere` - Request that the specified player teleport to you. 사용법 원문: `/<command> <player>`. 별칭 원문: `[etpahere]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpall` - Teleport all online players to another player. 사용법 원문: `/<command> [player]`. 별칭 원문: `[etpall]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpauto` - Automatically accept teleportation requests. 사용법 원문: `/<command> [player]`. 별칭 원문: `[etpauto]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpdeny` - Rejects teleport requests. 사용법 원문: `/<command> [player|*]`. 별칭 원문: `[etpdeny,tpno,etpno]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tphere` - Teleport a player to you. 사용법 원문: `/<command> <player>`. 별칭 원문: `[s,etphere]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpo` - Teleport override for tptoggle. 사용법 원문: `/<command> <player> [otherplayer]`. 별칭 원문: `[etpo]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpoffline` - Teleport to a player's last known logout location 사용법 원문: `/<command> <player>`. 별칭 원문: `[otp, offlinetp, tpoff, tpoffline, etpoffline]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpohere` - Teleport here override for tptoggle. 사용법 원문: `/<command> <player>`. 별칭 원문: `[etpohere]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tppos` - Teleport to coordinates. 사용법 원문: `/<command> <x> <y> <z> [yaw] [pitch] [world]`. 별칭 원문: `[etppos]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tpr` - Teleport randomly. 사용법 원문: `/<command>`. 별칭 원문: `[etpr, tprandom, etprandom]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tptoggle` - Blocks all forms of teleportation. 사용법 원문: `/<command> [player] [on|off]`. 별칭 원문: `[etptoggle]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/tree` - Spawn a tree where you are looking. 사용법 원문: `/<command> <tree|birch|redwood|redmushroom|brownmushroom|jungle|junglebush|swamp>`. 별칭 원문: `[etree]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/unban` - Unbans the specified player. 사용법 원문: `/<command> <player>`. 별칭 원문: `[pardon,eunban,epardon]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/unbanip` - Unbans the specified IP address. 사용법 원문: `/<command> <address>`. 별칭 원문: `[eunbanip,pardonip,epardonip]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/unlimited` - Allows the unlimited placing of items. 사용법 원문: `/<command> <list|item|clear> [player]`. 별칭 원문: `[eunlimited,ul,unl,eul,eunl]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/vanish` - Hide yourself from other players. 사용법 원문: `/<command> [player] [on|off]`. 별칭 원문: `[v,ev,evanish]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/warp` - List all warps or warp to the specified location. 사용법 원문: `/<command> <pagenumber|warp> [player]`. 별칭 원문: `[ewarp,warps,ewarps]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/warpinfo` - Finds location information for a specified warp. 사용법 원문: `/<command> <warp>`. 별칭 원문: `[ewarpinfo]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/weather` - Sets the weather. 사용법 원문: `/<command> <storm/sun> [duration]`. 별칭 원문: `[rain,erain,sky,esky,storm,estorm,sun,esun,eweather]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/whois` - Determine basic information about the specified player. 사용법 원문: `/<command> <nickname>`. 별칭 원문: `[ewhois]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/workbench` - Opens up a workbench. 사용법 원문: `/<command>`. 별칭 원문: `[craft,ecraft,wb,ewb,wbench,ewbench,eworkbench]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/world` - Switch between worlds. 사용법 원문: `/<command> [world]`. 별칭 원문: `[eworld]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/worth` - Calculates the worth of items in hand or as specified. 사용법 원문: `/<command> <<itemname>|<id>|hand|inventory|blocks> [-][amount]`. 별칭 원문: `[eprice,price,eworth]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>

### EssentialsChat

- `/toggleshout` - Toggles whether you are talking in shout mode 사용법 원문: `/<command> [player] [on|off]`. 별칭 원문: `[etoggleshout]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>

### EssentialsSpawn

- `/setspawn` - Sets the spawn point to your current position. 사용법 원문: `/<command> <group>`. 별칭 원문: `[esetspawn]`. 적용 서버: `survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/spawn` - Teleports to the spawn point. 사용법 원문: `/<command> [player]`. 별칭 원문: `[espawn]`. 적용 서버: `survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>

### ItemsAdder

- `/crops` - Enable or disable crops view for a player. 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#16a34a"><b>확인됨</b></font>
- `/ia` - Opens items list GUI (or optionally directly open a category) 사용법 원문: `/<command> [category] [player]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iablock` - Show info about block you're looking at 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iacleancache` - Cleans unused IDs from cache to allow them to be used by future added blocks / items. 사용법 원문: `/<command> <items|blocks>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iacolor` - Main command to color items in hand. 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaconfig` - Main command to execute some operations on the plugin configurations. 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iacustommodeldata` - Shows custom item CustomModelData 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iadebug` - Main command to run some debug actions. Useful only to developers! 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iadisguise` - Disguise as a custom entity. 사용법 원문: `/<command> <entityId>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iadrop` - Drop custom item at coords or at player location 사용법 원문: `/iadrop <itemname> <player|x y z world> [amount]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iadurability` - Modify durability of current item (vanilla or custom) 사용법 원문: `/<command>`. 별칭 원문: `[iadur]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaemote` - Play a custom player animation. 사용법 원문: `/<command> <emote|stop|stop-now> [player]`. 별칭 원문: `[emote]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaentity` - Main command to manage custom entities. 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaget` - Get custom item by namespaced id or by id 사용법 원문: `/<command> <itemname> [<amount>]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iagive` - Give custom item to player 사용법 원문: `/<command> <player> <itemname> [amount] [silent]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iahitbox` - Shows the hitbox of placed furnitures. 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iahud` - Force show/hide a HUD manually by namespaced id 사용법 원문: `/<command> [name]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaimage` - Show list of font images (emojis, huds...) 사용법 원문: `/<command>`. 별칭 원문: `[iaemoji, emoji, e]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iainfo` - Shows info about the plugin 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaitem` - Main command to manage custom items. 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/ialiquid` - Show info about liquid you're looking at 사용법 원문: `/<command> [x] [y] [z]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaplayerstat` - Set a custom player stat (and update HUD) value 사용법 원문: `/<command> <read|write|increment|decrement> <player> <attribute> [value]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaplaysound` - Play itemsadder sounds. Useful in console since vanilla /playsound is bugged in console. 사용법 원문: `/<command> <soundname> <player> [volume] [pitch]`. 별칭 원문: `[playcustomsound]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaplaytotemanimation` - Shows a Totem Of Undying animation 사용법 원문: `/<command> <totem> <player> [silent]`. 별칭 원문: `[totemanimation]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iarecipe` - Show item recipe GUI 사용법 원문: `/<command> [item name] [player]`. 별칭 원문: `[iaguide]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iareload` - Reloads configuration files 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaremove` - Remove custom item from player inventory 사용법 원문: `/<command> <player> <itemname> [amount] [silent]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iarename` - Rename current item (vanilla or custom). Supports emojis (font_images) and ItemsAdder text-effects. 사용법 원문: `/<command> <name>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iarepair` - Repairs current item (vanilla or custom) 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iasha1` - Calculates sha1 of the current resourcepack. 사용법 원문: `/<command> <local|online>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaspawntree` - Spawns a custom tree 사용법 원문: `/<command> <namespace>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iatag` - Shows custom item debug info 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iatexture` - Force the client to reload resourcepack (a player or everyone) 사용법 원문: `/<command> [all|player]`. 별칭 원문: `[iatexture, iapack]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iaundisguise` - Undisguise from a custom entity. 사용법 원문: `/<command>`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/iazip` - Executes /iareload and generates the resourcepack zip file 사용법 원문: `/<command> [--uncompressed]`. 별칭 원문: `[iaz]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>

### LuckPerms

- `/luckperms` - Manage permissions 별칭 원문: `[lp, perm, perms, permission, permissions]`. 적용 서버: `lobby, survival`. 상태: <font color="#d97706"><b>테스트 필요</b></font>

### PlaceholderAPI

- `/placeholderapi` - PlaceholderAPI Command 별칭 원문: `["papi"]`. 적용 서버: `lobby, survival`. 상태: <font color="#d97706"><b>테스트 필요</b></font>

### ProtocolLib

- `/filter` - Add or remove programmable filters to the packet listeners. 사용법 원문: `/<command> add|remove name [ID start]-[ID stop]`. 별칭 원문: `[packet_filter]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/packet` - Add or remove a simple packet listener. 사용법 원문: `/<command> add|remove|names client|server [ID start]-[ID stop] [detailed]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/packetlog` - Logs hex representations of packets to a file or console 사용법 원문: `/<command> <protocol> <sender> <packet> [location]`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/protocol` - Performs administrative tasks regarding ProtocolLib. 사용법 원문: `/<command> config|check|update|timings|listeners|version|dump`. 적용 서버: `lobby, survival`. 상태: <font color="#d97706"><b>테스트 필요</b></font>

### TAB

- `/tab` - Plugin's main command 적용 서버: `lobby, survival`. 상태: <font color="#d97706"><b>테스트 필요</b></font>

### Vault

- `/vault-convert` - Converts all data in economy1 and dumps it into economy2 사용법 원문: `|`. 적용 서버: `lobby, survival`. 상태: <font color="#dc2626"><b>주의</b></font>
- `/vault-info` - Displays information about Vault 사용법 원문: `|`. 적용 서버: `lobby, survival`. 상태: <font color="#d97706"><b>테스트 필요</b></font>

## 서버에서 우선 테스트할 항목

- `/lobby, /survival` - Velocity 이동, 리소스팩 유지, 인벤토리/권한 표시 정상 여부 확인. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/servermenu` - Shift+F와 동일한 서버 메뉴 GUI, dungeon 월드 차단, 관리자 전용 항목 표시 확인. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/hud compass on/off` - 나침반 보스바 이미지, 보스바 배경 숨김, 중심 정렬, 리소스팩 적용 여부 확인. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party chat <global|party|nation>, /tc, /nc` - survival/lobby 채팅 포맷, 권한 이미지, 파티/국가 prefix 확인. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/party claim` - 국가/신호기/돈 조건, 보호/채굴피로/PVP 옵션 확인. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/auction submit, /auction open, /auction end` - 등록-관리자 선정-입찰-낙찰-돈 차감/지급 흐름 확인. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/dungeon enter/exit, /dungeon chest roll` - 내부 dungeon 월드 이동, 보호, 랜덤 상자 생성, survival 복귀 랜덤 범위 확인. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/combat force, 로그아웃/관전 clone` - 전투 태그, 전투 중 종료 즉사, 일반 로그아웃 시체 NPC, 타격/드랍 처리 확인. 상태: <font color="#d97706"><b>테스트 필요</b></font>
- `/quest, /jobs, /craftmenu, /rankup` - 초반 플레이 루프의 퀘스트 진행, 돈 지급, 제작, 랭크업 연동 확인. 상태: <font color="#d97706"><b>테스트 필요</b></font>
