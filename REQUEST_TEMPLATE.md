# REQUEST_TEMPLATE.md

Use this format to reduce token usage and avoid broad, unnecessary code reads.

Before sending a feature request, check `PLUGIN_INDEX.md` if you know the target
plugin. Naming the target plugin is the biggest token saver.

## Preferred Request Format

```text
대상:
- 플러그인/서버/파일:

목표:
- 이번에 바꿀 기능 하나:

범위:
- 건드려도 되는 것:
- 건드리면 안 되는 것:

검증:
- 성공 기준:

배포:
- 서버에 바로 적용할지:
- 서버 재시작 허용 여부:
```

## Short Example

```text
대상:
- LeeSeolAuction

목표:
- 경매 GUI의 뒤로가기 버튼 위치만 바꿔줘.

범위:
- Auction GUI 코드와 config만 수정.
- 경제/상점/다른 플러그인은 건드리지 말 것.

검증:
- 빌드 성공.
- 서버 로그에 ERROR 없음.

배포:
- 서버에 바로 적용.
- minecraft와 lobby 재시작 허용.
```

## Operating Rule

- One request should target one feature whenever possible.
- If a request touches multiple plugins, state the required order.
- If logs are needed, prefer a short error summary. The agent can inspect server logs
  directly when VM access is available.
- If the task is exploratory, say "분석만" so no code is changed.
- If the task should not deploy to the live VM, say "로컬 수정만".
