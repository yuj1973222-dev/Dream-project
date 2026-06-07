from __future__ import annotations

import html
import re
from collections import defaultdict
from pathlib import Path

from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import PageBreak, Paragraph, SimpleDocTemplate, Spacer


ROOT = Path(__file__).resolve().parents[1]
OUT_MD = ROOT / "ADMIN_INGAME_MANUAL.md"
OUT_PDF = ROOT / "ADMIN_INGAME_MANUAL.pdf"
REMOTE_INVENTORY = ROOT / "REMOTE_PLUGIN_COMMAND_INVENTORY.txt"

OK = '<font color="#16a34a"><b>확인됨</b></font>'
TEST = '<font color="#d97706"><b>테스트 필요</b></font>'
WARN = '<font color="#dc2626"><b>주의</b></font>'


def command(command_text: str, desc: str, status: str = OK) -> str:
    return f"- `{command_text}` - {desc} 상태: {status}"


CUSTOM_SECTIONS: list[tuple[str, list[str]]] = [
    (
        "Velocity / 네트워크 이동: LeeSeolProxy",
        [
            command("/lobby", "현재 접속자를 Velocity의 `lobby` 백엔드 서버로 이동한다.", TEST),
            command("/hub", "`/lobby` 별칭. 로비 서버로 이동한다.", TEST),
            command("/survival", "현재 접속자를 Velocity의 `survival` 백엔드 서버로 이동한다.", TEST),
            command("/wild", "`/survival` 별칭. 야생 서버로 이동한다.", TEST),
            command("/servers", "Velocity에 등록된 서버 목록과 현재 접속 인원을 표시한다."),
            command("/serverlist", "`/servers` 별칭."),
            command("/network", "`/servers` 별칭."),
        ],
    ),
    (
        "코어 기능: LeeSeolCore",
        [
            command("/serverinfo", "현재 Paper 서버명, 접속자 수, 최대 인원, 마인크래프트 버전을 표시한다."),
            command("/leeseolcore reload", "LeeSeolCore 설정, 런치패드, 포탈, 차원 제한 설정을 다시 읽는다.", TEST),
            command("/lscore reload", "`/leeseolcore reload`와 같은 관리자 reload 명령이다.", TEST),
            command("/leeseolcore launchpad set <id> [forward] [upward] [cooldownSeconds]", "현재 위치 또는 바라보는 블록을 런치패드로 등록한다.", TEST),
            command("/leeseolcore launchpad list", "등록된 런치패드 목록을 표시한다."),
            command("/leeseolcore launchpad remove <id>", "등록된 런치패드를 삭제한다.", WARN),
            command("/leeseolcore portal pos1", "포탈 cuboid 영역의 첫 번째 좌표를 현재 위치로 저장한다.", TEST),
            command("/leeseolcore portal pos2", "포탈 cuboid 영역의 두 번째 좌표를 현재 위치로 저장한다.", TEST),
            command("/leeseolcore portal create <id> <targetServer> [cooldownSeconds]", "WorldEdit 선택 영역 또는 pos1/pos2 영역을 Velocity 서버 이동 포탈로 등록한다.", TEST),
            command("/leeseolcore portal list", "등록된 Velocity 이동 포탈을 표시한다."),
            command("/leeseolcore portal remove <id>", "등록된 Velocity 이동 포탈을 삭제한다.", WARN),
        ],
    ),
    (
        "경제 / 상점 / Shift+F 메뉴: LeeSeolEconomy",
        [
            command("/won", "자신의 보유 원화를 확인한다."),
            command("/won balance [player]", "원화 잔액을 확인한다. 다른 플레이어 조회는 관리자 권한이 필요하다."),
            command("/won bal [player]", "`/won balance` 별칭."),
            command("/won money [player]", "`/won balance` 별칭."),
            command("/won pay <player> <amount>", "다른 접속자에게 원화를 송금한다.", TEST),
            command("/won give <player> <amount>", "관리자가 대상에게 원화를 지급한다.", WARN),
            command("/won take <player> <amount>", "관리자가 대상의 원화를 차감한다.", WARN),
            command("/won set <player> <amount>", "관리자가 대상의 원화 잔액을 지정값으로 설정한다.", WARN),
            command("/won reload", "경제/상점 설정을 다시 읽는다.", TEST),
            command("/shop [shopId]", "상점 GUI를 연다. shopId가 없으면 기본 상점을 연다.", TEST),
            command("/wonnpc create <id> <shopId> [skin:<playerName>] [displayName]", "현재 위치에 상점 NPC를 생성한다. 플레이어 스킨 지정 가능.", TEST),
            command("/wonnpc skin <id> <playerName|none>", "상점 NPC의 스킨을 변경하거나 제거한다.", TEST),
            command("/wonnpc remove <id>", "상점 NPC를 제거한다.", WARN),
            command("/wonnpc list", "등록된 상점 NPC 목록을 표시한다."),
            command("/servermenu", "Shift+F와 동일한 서버 이동 GUI를 연다. dungeon 월드에서는 기본 차단된다.", TEST),
        ],
    ),
    (
        "경매: LeeSeolAuction",
        [
            command("/auction", "경매 메인 GUI를 연다.", TEST),
            command("/ah", "`/auction` 별칭.", TEST),
            command("/auc", "`/auction` 별칭.", TEST),
            command("/auction submit", "유저가 경매 후보 아이템을 등록하는 GUI를 연다.", TEST),
            command("/auction admin", "관리자용 경매 후보 선택 GUI를 연다.", TEST),
            command("/auction open <lotId> [startingBid] [bidIncrement]", "선택한 후보 아이템으로 경매를 시작한다.", WARN),
            command("/auction increment <amount>", "현재 경매의 기본 입찰 상승폭을 변경한다.", TEST),
            command("/auction setincrement <amount>", "`/auction increment`와 같은 기능.", TEST),
            command("/auction end", "현재 경매를 종료하고 최고 입찰자를 낙찰 처리한다.", WARN),
            command("/auction reload", "경매 설정을 다시 읽는다.", TEST),
        ],
    ),
    (
        "던전: LeeSeolDungeon",
        [
            command("/dungeon reload", "던전 월드, 포탈, 보호, 루트 상자 설정을 다시 읽는다.", TEST),
            command("/dungeon enter", "관리자를 내부 `dungeon` 월드로 즉시 이동시킨다.", TEST),
            command("/dungeon exit", "관리자를 survival 반환 위치로 이동시킨다.", TEST),
            command("/dungeon portal pos1", "던전 포탈 영역 첫 번째 좌표를 저장한다.", TEST),
            command("/dungeon portal pos2", "던전 포탈 영역 두 번째 좌표를 저장한다.", TEST),
            command("/dungeon portal create <id> <targetWorld|return> [cooldownSeconds]", "survival 내부 다중월드 포탈을 생성한다.", TEST),
            command("/dungeon portal list", "던전 포탈 목록을 표시한다."),
            command("/dungeon portal remove <id>", "던전 포탈을 삭제한다.", WARN),
            command("/dungeon chest table <tableId>", "루트 테이블 GUI 편집기를 연다.", TEST),
            command("/dungeon chest addspot <id> <tableId> [chance] [respawnSeconds]", "현재 위치를 확률형 루트 상자 스폰 지점으로 등록한다.", TEST),
            command("/dungeon chest list", "루트 상자 스폰 지점 목록을 표시한다."),
            command("/dungeon chest spawn <id>", "지정 루트 상자를 즉시 생성한다.", TEST),
            command("/dungeon chest roll", "등록된 루트 상자 스폰 확률을 즉시 계산한다.", TEST),
            command("/dungeon chest removesp ot <id>".replace("removesp ot", "removespot"), "루트 상자 스폰 지점을 삭제한다.", WARN),
            command("/lsdungeon", "`/dungeon` 별칭."),
        ],
    ),
    (
        "로비 보호: LeeSeolLobby",
        [
            command("/lobbysetspawn", "현재 위치를 로비 고정 스폰 위치로 저장한다.", TEST),
        ],
    ),
    (
        "파티 / 국가 / 클레임 / 전쟁: LeeSeolTown",
        [
            command("/party create <name>", "파티를 생성한다. 유저는 하나의 파티에만 소속 가능하다.", TEST),
            command("/party invite <player>", "플레이어에게 파티 초대를 보낸다. 수락/거절 버튼 메시지와 연동된다.", TEST),
            command("/party accept <party>", "받은 파티 초대를 수락한다.", TEST),
            command("/party deny <party>", "받은 파티 초대를 거절한다.", TEST),
            command("/party join <party>", "공개 참가 흐름이 허용된 경우 파티 참가를 시도한다.", TEST),
            command("/party leave", "현재 파티/국가에서 나간다.", WARN),
            command("/party disband", "파티장이 파티를 해산한다. 확인 재입력 흐름이 적용된다.", WARN),
            command("/party transfer <player>", "파티장 권한을 다른 파티원에게 위임한다. 확인 재입력 흐름이 적용된다.", WARN),
            command("/party kick <player>", "파티장이 파티원을 강제 추방한다.", WARN),
            command("/party claim", "현재 청크를 국가 영토로 구매한다. 국가와 신호기 조건이 필요하다.", TEST),
            command("/party unclaim", "현재 청크의 국가 영토를 해제한다.", WARN),
            command("/party info [party]", "자신 또는 지정 파티 정보를 표시한다."),
            command("/party me", "자신의 파티/국가 소속 정보를 표시한다."),
            command("/party status", "`/party me`와 같은 소속 확인 명령."),
            command("/party chat <global|party|nation>", "개인 채팅 모드를 전체/파티/국가로 전환한다.", TEST),
            command("/tc [message]", "파티 채팅 전송 또는 파티 채팅 토글.", TEST),
            command("/pc [message]", "`/tc` 별칭.", TEST),
            command("/nc [message]", "국가 채팅 전송 또는 국가 채팅 토글.", TEST),
            command("/party nation create <name> <republic|empire> [party...]", "파티를 공화국/제국으로 승급하거나 여러 파티를 포함한 국가를 생성한다.", WARN),
            command("/party nation disband", "국가를 해산한다. 확인 재입력 흐름이 적용된다.", WARN),
            command("/party nation pvp <on|off>", "국가 내부 PVP 허용 여부를 설정한다.", TEST),
            command("/party nation build <on|off>", "비국가원의 블록 설치/파괴 보호를 켜거나 끈다.", TEST),
            command("/party nation treasury", "국가 금고 정보를 표시한다."),
            command("/party nation deposit <amount>", "자신의 원화를 국가 금고에 입금한다.", TEST),
            command("/party federation create <name> <party1> [party2] [party3] [...]", "관리자가 연방을 생성한다.", WARN),
            command("/party federation disband", "연방/국가를 해산한다.", WARN),
            command("/party war declare <nation>", "상대 국가에 전쟁을 선포한다.", WARN),
            command("/party war accept <attackerNation>", "상대 국가의 전쟁을 수락한다.", WARN),
            command("/party war surrender <enemyNation>", "전쟁에서 항복한다. 카르마/배상금/보호 해제와 연결된다.", WARN),
            command("/party war release <enemyNation>", "10분 보호를 국가장이 조기 해제한다.", WARN),
            command("/party war paydebt", "항복 배상금 미납금을 납부한다.", TEST),
            command("/party war finish <winnerNation> <loserNation>", "관리자가 전쟁 결과를 강제 종료 처리한다.", WARN),
            command("/party reload", "파티/국가 설정과 데이터를 다시 읽는다.", TEST),
            command("/town", "`/party` 루트 명령 별칭."),
            command("/village", "`/party` 루트 명령 별칭."),
            command("/towny", "`/party` 루트 명령 별칭."),
        ],
    ),
    (
        "홀로그램: LeeSeolHologram",
        [
            command("/holo create <id> [text]", "현재 위치에 홀로그램을 생성한다. RGB 형식 `&#RRGGBB` 또는 `<#RRGGBB>` 사용 가능.", TEST),
            command("/holo addline <id> <text>", "홀로그램 마지막 줄에 텍스트를 추가한다.", TEST),
            command("/holo setline <id> <line> <text>", "지정 줄 내용을 변경한다.", TEST),
            command("/holo insertline <id> <line> <text>", "지정 위치에 줄을 삽입한다.", TEST),
            command("/holo removeline <id> <line>", "지정 줄을 삭제한다.", WARN),
            command("/holo movehere <id>", "홀로그램을 현재 위치로 이동한다.", TEST),
            command("/holo spacing <id> <value>", "줄 간격을 변경한다.", TEST),
            command("/holo info <id>", "홀로그램 상세 정보를 표시한다."),
            command("/holo list", "등록된 홀로그램 목록을 표시한다."),
            command("/holo delete <id>", "홀로그램을 삭제한다.", WARN),
            command("/holo reload", "홀로그램 설정/데이터를 다시 읽는다.", TEST),
            command("/hologram", "`/holo` 별칭."),
            command("/lholo", "`/holo` 별칭."),
        ],
    ),
    (
        "전투 / 시체 / PVP 포인트: LeeSeolCombat",
        [
            command("/combat status", "전투 태그 수, 로그아웃 NPC 수, 관전 NPC 설정, PVP 기록 수를 표시한다."),
            command("/combat reload", "전투 설정을 다시 읽는다.", TEST),
            command("/combat force <player1> <player2>", "관리자가 두 플레이어를 강제로 전투 상태에 넣는다.", TEST),
            command("/combat spectatorclone <on|off>", "관전 모드 전환 시 NPC 생성 기능을 켜거나 끈다.", TEST),
            command("/combat pvp [player]", "자신 또는 대상의 PVP 포인트와 처치 수를 표시한다."),
            command("/combat pvppoints <set|add|take> <player> <amount>", "관리자가 PVP 포인트를 조정한다.", WARN),
            command("/leeseolcombat", "`/combat` 루트 명령 별칭."),
        ],
    ),
    (
        "아이템 청소: LeeSeolCleanup",
        [
            command("/leeseolcleanup status", "드랍 아이템 청소 활성 여부, 주기, 대상 월드를 표시한다."),
            command("/leeseolcleanup run", "대상 월드의 드랍 아이템을 즉시 청소한다.", WARN),
            command("/leeseolcleanup reload", "아이템 청소 설정을 다시 읽는다.", TEST),
            command("/cleanup", "`/leeseolcleanup` 별칭."),
            command("/itemcleanup", "`/leeseolcleanup` 별칭."),
        ],
    ),
    (
        "랭크: LeeSeolRanks",
        [
            command("/rank", "자신의 현재 랭크, 킬 수, 다음 랭크 조건을 표시한다."),
            command("/rank <player>", "지정 플레이어의 랭크 정보를 표시한다."),
            command("/rank progress", "다음 랭크 승급 조건 달성도를 표시한다."),
            command("/rank requirements", "D/C/B/A/S 승급 조건 목록을 표시한다."),
            command("/rank up", "`/rankup`과 동일하게 승급을 시도한다.", TEST),
            command("/rank rankup", "`/rankup`과 동일하게 승급을 시도한다.", TEST),
            command("/rankup", "조건을 만족하면 다음 랭크로 승급하고 기존 킬 카운트를 초기화한다.", TEST),
            command("/leeseolrank status", "랭크 데이터에 저장된 플레이어 수를 표시한다."),
            command("/leeseolrank reload", "랭크 설정과 권한 동기화 설정을 다시 읽는다.", TEST),
            command("/leeseolrank set <player> <PLAYER|D|C|B|A|S|ADMIN|DEV>", "관리자가 대상 랭크를 직접 지정한다.", WARN),
            command("/leeseolrank dev <player> <on|off>", "대상에게 DEV 랭크를 부여하거나 PLAYER로 되돌린다.", WARN),
            command("/lsrank", "`/leeseolrank` 별칭."),
            command("/ranks", "`/rank` 별칭."),
        ],
    ),
    (
        "튜토리얼 / 퀘스트: LeeSeolQuest",
        [
            command("/quest", "퀘스트 GUI를 연다.", TEST),
            command("/quests", "`/quest` 별칭.", TEST),
            command("/quest start <id>", "지정 퀘스트를 시작한다.", TEST),
            command("/quest progress", "현재 퀘스트 진행도를 표시한다."),
            command("/quest abandon", "현재 퀘스트를 포기한다.", WARN),
            command("/tutorial start", "기본 튜토리얼 퀘스트를 시작한다.", TEST),
            command("/tutorial skip", "튜토리얼을 완료 처리 또는 스킵 처리한다.", WARN),
            command("/tutorial reset <player>", "관리자가 대상의 튜토리얼/퀘스트 데이터를 초기화한다.", WARN),
            command("/lsquest reload", "퀘스트 설정을 다시 읽는다.", TEST),
            command("/lsquest set <player> <questId> <stage>", "온라인 대상의 퀘스트 단계를 강제로 지정한다.", WARN),
            command("/lsquest advance <player>", "온라인 대상의 현재 퀘스트를 한 단계 진행한다.", TEST),
            command("/lsquest objective <player> <type> [target]", "온라인 대상에게 퀘스트 목표 진행도를 1 추가한다.", TEST),
            command("/lsquest reset <player>", "대상의 퀘스트 데이터를 초기화한다.", WARN),
        ],
    ),
    (
        "초반 직업 보상: LeeSeolJobs",
        [
            command("/jobs", "광질/농사/낚시 일일 보상 통계를 표시한다."),
            command("/jobs stats", "`/jobs`와 동일하게 자신의 통계를 표시한다."),
            command("/jobs top", "현재 코드에서는 탭완성만 존재하며 별도 랭킹 출력은 미구현 상태다.", TEST),
            command("/lsjobs status", "Jobs 데이터에 저장된 플레이어 수를 표시한다."),
            command("/lsjobs reload", "Jobs 보상/제한 설정을 다시 읽는다.", TEST),
            command("/lsjobs stats <player>", "대상의 일일/누적 직업 통계를 표시한다."),
            command("/lsjobs reset <player>", "대상의 직업 통계를 초기화한다.", WARN),
        ],
    ),
    (
        "제작 / 가공 / 수리: LeeSeolCrafting",
        [
            command("/craftmenu", "기본 제작 GUI를 연다.", TEST),
            command("/forge", "장비/무기 제작 GUI를 연다.", TEST),
            command("/process", "광물/재료 가공 GUI를 연다.", TEST),
            command("/disassemble", "아이템 분해 GUI를 연다.", TEST),
            command("/repair", "원화 기반 수리 확인 GUI를 연다.", TEST),
            command("/lscrafting status", "등록된 제작 레시피 수를 표시한다."),
            command("/lscrafting reload", "제작 설정과 레시피를 다시 읽는다.", TEST),
            command("/lscrafting recipe list", "등록된 레시피 ID 목록을 표시한다."),
            command("/lscrafting recipe give <player> <recipeId>", "관리자가 레시피 결과 아이템을 대상에게 지급한다.", WARN),
        ],
    ),
    (
        "HUD / 나침반 / 체력 표시: LeeSeolHUD",
        [
            command("/hud", "자신의 HUD 기능 상태를 표시한다."),
            command("/hud compass on", "ItemsAdder 리소스팩 기반 보스바 나침반 HUD를 켠다.", TEST),
            command("/hud compass off", "나침반 HUD를 끈다.", TEST),
            command("/hud target on", "대상 체력 HUD를 켠다. 현재 기본 목표는 TAB 아래이름 체력 표시다.", TEST),
            command("/hud target off", "대상 체력 HUD를 끈다.", TEST),
            command("/compasshud on", "나침반 HUD를 직접 켠다.", TEST),
            command("/compasshud off", "나침반 HUD를 직접 끈다.", TEST),
            command("/lshud status", "HUD 설정 활성 여부와 온라인 수를 표시한다."),
            command("/lshud reload", "HUD 설정을 다시 읽는다.", TEST),
        ],
    ),
]


RISKY_WORDS = {
    "ban", "banip", "tempban", "tempbanip", "kick", "kickall", "kill", "sudo",
    "eco", "give", "item", "iagive", "iaget", "iadel", "iaremove", "iareload",
    "iazip", "iacleancache", "setspawn", "sethome", "setwarp", "delhome",
    "delwarp", "deljail", "remove", "clearinventory", "mute", "unban",
    "unbanip", "vault-convert", "packet", "packetlog", "filter",
}


def parse_remote_inventory() -> dict[tuple[str, str], dict[str, object]]:
    if not REMOTE_INVENTORY.exists():
        return {}

    lines = REMOTE_INVENTORY.read_text(encoding="utf-8-sig", errors="replace").splitlines()
    server = None
    plugin = None
    jar = None
    in_commands = False
    current = None
    commands: dict[tuple[str, str], dict[str, object]] = {}

    def ensure(cmd: str) -> dict[str, object]:
        key = (plugin or jar or "unknown", cmd)
        entry = commands.setdefault(key, {"plugin": plugin or jar or "unknown", "command": cmd, "servers": set(), "desc": "", "usage": "", "aliases": ""})
        if server:
            entry["servers"].add(server)
        return entry

    for line in lines:
        if line.startswith("===== SERVER "):
            parts = line.split()
            server = parts[2] if len(parts) > 2 else "unknown"
            in_commands = False
            current = None
            continue
        if line.startswith("--- "):
            jar = line[4:].strip()
            plugin = None
            in_commands = False
            current = None
            continue
        if line.startswith("name: "):
            plugin = line[6:].strip() or plugin
            continue
        stripped = line.strip()
        if stripped == "commands:":
            in_commands = True
            current = None
            continue
        if stripped == "commands: none" or line.startswith("permissions:"):
            in_commands = False
            current = None
            continue
        if not in_commands:
            continue
        if line.startswith("  ") and not line.startswith("    ") and ":" in line:
            cmd = stripped.split(":", 1)[0].strip().strip("'\"")
            if cmd:
                current = ensure(cmd)
            continue
        if current is None:
            continue
        if "description:" in stripped:
            current["desc"] = stripped.split("description:", 1)[1].strip().strip("'\"")
        elif "usage:" in stripped:
            current["usage"] = stripped.split("usage:", 1)[1].strip().strip("'\"")
        elif "aliases:" in stripped:
            current["aliases"] = stripped.split("aliases:", 1)[1].strip().strip("'\"")

    return commands


def status_for_external(command_name: str) -> str:
    lowered = command_name.lower()
    if lowered in RISKY_WORDS or any(lowered.startswith(prefix) for prefix in ("ia", "vault-convert", "packet")):
        return WARN
    if lowered in {"tab", "luckperms", "placeholderapi", "protocol", "vault-info", "npc", "citizens", "advancedenchantments"}:
        return TEST
    return OK


def build_markdown() -> str:
    lines: list[str] = []
    lines.append("# Expedition 서버 관리자용 인게임 사용설명서")
    lines.append("")
    lines.append("작성 기준일: 2026-06-05")
    lines.append("")
    lines.append("## 서론: 현재 서버 개요")
    lines.append("")
    lines.append("현재 Expedition 서버는 Velocity 프록시와 Paper 백엔드를 분리한 구조다. 외부 유저는 `25565` 포트의 Velocity로만 접속하고, 실제 게임 서버인 survival/lobby는 내부 주소 `127.0.0.1`에 묶여 있다. 던전은 별도 Velocity 서버가 아니라 survival Paper 내부의 `dungeon` 월드로 운영한다. Newworld 서버 폴더와 서비스는 존재하지만 현재 기본 상태는 비활성이다.")
    lines.append("")
    lines.append("- 공개 진입점: Velocity `0.0.0.0:25565`")
    lines.append("- Survival Paper: `/opt/minecraft/server`, 서비스 `minecraft`, 내부 포트 `25566`")
    lines.append("- Lobby Paper: `/opt/minecraft/lobby`, 서비스 `lobby`, 내부 포트 `25567`")
    lines.append("- Resource pack host: `resourcepack` 서비스, 포트 `8163`")
    lines.append("- Newworld Paper: `/opt/minecraft/dungeon`, 서비스 `newworld`, 기본 비활성")
    lines.append("- 공유 데이터: MariaDB 기반 LuckPerms, `/opt/minecraft/shared` 기반 커스텀 데이터")
    lines.append("")
    lines.append("## 표기 규칙")
    lines.append("")
    lines.append(f"- {OK}: 정보 조회 또는 이미 안정적으로 쓰는 명령어")
    lines.append(f"- {TEST}: 서버 안에서 실제 플레이어/월드/GUI/리소스팩 동작 확인이 필요한 명령어")
    lines.append(f"- {WARN}: 돈, 데이터, 월드, NPC, 권한, 밴, 삭제, 전쟁 결과 등 운영 상태를 바꾸는 명령어. 테스트 서버나 백업 후 사용 권장")
    lines.append("")
    lines.append("## 커스텀 플러그인 명령어")
    lines.append("")
    for title, commands in CUSTOM_SECTIONS:
        lines.append(f"### {title}")
        lines.append("")
        lines.extend(commands)
        lines.append("")

    external = parse_remote_inventory()
    lines.append("## 외부 플러그인 루트 명령어 부록")
    lines.append("")
    lines.append("아래 목록은 live VM의 survival/lobby/velocity `plugins/*.jar` descriptor에서 추출한 루트 명령어다. 외부 플러그인의 세부 하위 명령어는 플러그인 자체 help와 설정에 따라 달라질 수 있다.")
    lines.append("")
    grouped: dict[str, list[dict[str, object]]] = defaultdict(list)
    for entry in external.values():
        plugin = str(entry["plugin"])
        if plugin.startswith("LeeSeol"):
            continue
        grouped[plugin].append(entry)

    for plugin in sorted(grouped):
        entries = sorted(grouped[plugin], key=lambda item: str(item["command"]).lower())
        lines.append(f"### {plugin}")
        lines.append("")
        for entry in entries:
            cmd = str(entry["command"])
            desc = str(entry.get("desc") or "").strip() or "플러그인 descriptor에 등록된 루트 명령어."
            usage = str(entry.get("usage") or "").strip()
            aliases = str(entry.get("aliases") or "").strip()
            servers = ", ".join(sorted(entry["servers"]))
            detail = desc
            if usage:
                detail += f" 사용법 원문: `{usage}`."
            if aliases:
                detail += f" 별칭 원문: `{aliases}`."
            detail += f" 적용 서버: `{servers}`."
            lines.append(command("/" + cmd, detail, status_for_external(cmd)))
        lines.append("")

    lines.append("## 서버에서 우선 테스트할 항목")
    lines.append("")
    lines.append(command("/lobby, /survival", "Velocity 이동, 리소스팩 유지, 인벤토리/권한 표시 정상 여부 확인.", TEST))
    lines.append(command("/servermenu", "Shift+F와 동일한 서버 메뉴 GUI, dungeon 월드 차단, 관리자 전용 항목 표시 확인.", TEST))
    lines.append(command("/hud compass on/off", "나침반 보스바 이미지, 보스바 배경 숨김, 중심 정렬, 리소스팩 적용 여부 확인.", TEST))
    lines.append(command("/party chat <global|party|nation>, /tc, /nc", "survival/lobby 채팅 포맷, 권한 이미지, 파티/국가 prefix 확인.", TEST))
    lines.append(command("/party claim", "국가/신호기/돈 조건, 보호/채굴피로/PVP 옵션 확인.", TEST))
    lines.append(command("/auction submit, /auction open, /auction end", "등록-관리자 선정-입찰-낙찰-돈 차감/지급 흐름 확인.", TEST))
    lines.append(command("/dungeon enter/exit, /dungeon chest roll", "내부 dungeon 월드 이동, 보호, 랜덤 상자 생성, survival 복귀 랜덤 범위 확인.", TEST))
    lines.append(command("/combat force, 로그아웃/관전 clone", "전투 태그, 전투 중 종료 즉사, 일반 로그아웃 시체 NPC, 타격/드랍 처리 확인.", TEST))
    lines.append(command("/quest, /jobs, /craftmenu, /rankup", "초반 플레이 루프의 퀘스트 진행, 돈 지급, 제작, 랭크업 연동 확인.", TEST))
    lines.append("")
    return "\n".join(lines)


def protect_reportlab_tags(text: str) -> tuple[str, dict[str, str]]:
    tags: dict[str, str] = {}

    def repl(match: re.Match[str]) -> str:
        key = f"@@TAG{len(tags)}@@"
        tags[key] = match.group(0)
        return key

    protected = re.sub(r"<font color=\"#[0-9A-Fa-f]{6}\"><b>.*?</b></font>", repl, text)
    return protected, tags


def paragraph_text(text: str) -> str:
    protected, tags = protect_reportlab_tags(text)
    escaped = html.escape(protected)
    escaped = re.sub(r"`([^`]+)`", r"<font name='MalgunGothic'>\1</font>", escaped)
    escaped = escaped.replace("&lt;", "&lt;").replace("&gt;", "&gt;")
    for key, value in tags.items():
        escaped = escaped.replace(key, value)
    return escaped


def build_pdf(markdown: str) -> None:
    font_path = Path(r"C:\Windows\Fonts\malgun.ttf")
    if not font_path.exists():
        raise FileNotFoundError("Malgun Gothic font not found at C:\\Windows\\Fonts\\malgun.ttf")
    pdfmetrics.registerFont(TTFont("MalgunGothic", str(font_path)))

    styles = getSampleStyleSheet()
    base = ParagraphStyle(
        "KoreanBase",
        parent=styles["Normal"],
        fontName="MalgunGothic",
        fontSize=9,
        leading=13,
        spaceAfter=4,
    )
    title = ParagraphStyle(
        "KoreanTitle",
        parent=base,
        fontSize=18,
        leading=24,
        alignment=TA_CENTER,
        spaceAfter=10,
    )
    h2 = ParagraphStyle(
        "KoreanH2",
        parent=base,
        fontSize=14,
        leading=18,
        spaceBefore=8,
        spaceAfter=6,
    )
    h3 = ParagraphStyle(
        "KoreanH3",
        parent=base,
        fontSize=11,
        leading=15,
        spaceBefore=6,
        spaceAfter=4,
    )
    bullet = ParagraphStyle(
        "KoreanBullet",
        parent=base,
        leftIndent=10,
        firstLineIndent=-7,
    )

    story = []
    for raw in markdown.splitlines():
        line = raw.rstrip()
        if not line:
            story.append(Spacer(1, 2))
            continue
        if line.startswith("# "):
            story.append(Paragraph(paragraph_text(line[2:]), title))
            continue
        if line.startswith("## "):
            if story:
                story.append(Spacer(1, 3))
            story.append(Paragraph(paragraph_text(line[3:]), h2))
            continue
        if line.startswith("### "):
            story.append(Paragraph(paragraph_text(line[4:]), h3))
            continue
        if line.startswith("- "):
            story.append(Paragraph("&bull; " + paragraph_text(line[2:]), bullet))
            continue
        story.append(Paragraph(paragraph_text(line), base))

    doc = SimpleDocTemplate(
        str(OUT_PDF),
        pagesize=A4,
        rightMargin=14 * mm,
        leftMargin=14 * mm,
        topMargin=12 * mm,
        bottomMargin=12 * mm,
        title="Expedition 서버 관리자용 인게임 사용설명서",
        author="Codex",
    )
    doc.build(story)


def main() -> None:
    markdown = build_markdown()
    OUT_MD.write_text(markdown, encoding="utf-8")
    build_pdf(markdown)
    print(OUT_MD)
    print(OUT_PDF)


if __name__ == "__main__":
    main()
