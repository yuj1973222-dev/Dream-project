# REQUEST_TEMPLATE.md

Use this short format when you want to save tokens.

```text
Target:
- Plugin/server/file:

Goal:
- One concrete feature or bug:

Scope:
- Touch:
- Do not touch:

Verify:
- Success criteria:

Deploy:
- Apply live? yes/no
- Restart allowed? which service:
```

Example:

```text
Target:
- LeeSeolTown

Goal:
- Change neutral-zone actionbar text only.

Scope:
- Touch LeeSeolTown config/message only.
- Do not edit WorldGuard, BlueMap, or claim logic.

Verify:
- Build if code changes.
- Recent minecraft logs have no new errors.

Deploy:
- Apply live: yes
- Restart allowed: minecraft
```

Short keywords:

- `분석만`: inspect and explain; do not edit files.
- `로컬 수정만`: edit locally; do not deploy live.
- `배포까지`: build, deploy, restart only the affected service, and verify.
