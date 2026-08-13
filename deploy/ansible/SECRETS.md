# 시크릿 관리 (SOPS + age)

VM `.env`는 **수동 배치하지 않는다.** 시크릿은 환경별 SOPS 파일(암호화, git 커밋)에 두고,
Ansible이 **컨트롤러(CI/로컬)에서 복호화**해 VM의 `/opt/todakun/.env`로 렌더링한다(VM엔 age 키 불필요).

| 환경 | 시크릿 파일 |
|------|------------|
| dev  | `inventory/dev/group_vars/all.sops.yaml` |
| prod | `inventory/prod/group_vars/all.sops.yaml` |

```text
group_vars/all.sops.yaml (암호화, git) ──sops 복호화(컨트롤러)──▶ Ansible vars ──env.j2──▶ /opt/todakun/.env (VM, 0600)
```

> **⚠️ 파일 위치 필수 규칙**: `community.sops` vars 플러그인은 **`group_vars/<그룹>.sops.yaml` 플랫 파일만** 복호화한다
> (기본 `valid_extensions`: `.sops.yaml`/`.sops.yml`/`.sops.json`). `group_vars/all/` **하위 디렉터리**에 둔
> `*.sops.*.yaml`(예: `group_vars/all/secrets.sops.dev.yaml`)는 복호화되지 않고 `host_group_vars`가 **암호문 그대로** 로드하므로,
> 반드시 `group_vars/all.sops.yaml`(그룹명 = `all`) 경로에 둔다.
>
> 인벤토리는 `inventory/dev/`, `inventory/prod/`로 분리되어 있어(`-i inventory/dev/hosts.ini` / `-i inventory/prod/hosts.ini`)
> 각 환경 실행 시 인접한 `group_vars`만 로드되어 값이 섞이지 않는다. (디렉터리(`-i inventory/dev`)로 지정하면
> 그 안의 `hosts.ini.example` 같은 템플릿까지 파싱되므로 파일을 명시한다.)

## 1. 도구 설치 (로컬, 최초 1회)

```bash
mkdir -p ~/.config/sops/age
brew install sops age ansible
ansible-galaxy collection install -r collections/requirements.yaml
```

## 2. age 키페어 생성

```bash
age-keygen -o ~/.config/sops/age/keys.txt
# 출력된 "Public key: age1...." 를 복사 → .sops.yaml의 해당 환경 age 필드에 붙여넣기
```

- **개인키(`keys.txt`)**: 절대 커밋 금지(gitignore됨). 팀은 안전한 채널로 공유하거나 각자 키를 recipient에 추가.
- **공개키(`age1...`)**: `.sops.yaml`의 dev/prod `age:` 필드에 기입(여러 명이면 콤마로 나열).

## 3. 시크릿 작성 & 암호화

```bash
cd deploy/ansible
# 예제에서 복사 후 실제 값 채우기
cp inventory/secrets.sops.yaml.example inventory/dev/group_vars/all.sops.yaml
# 편집 완료 후 제자리 암호화
sops -e -i inventory/dev/group_vars/all.sops.yaml
# 암호화 확인 후 커밋
git add inventory/dev/group_vars/all.sops.yaml
```

- 이후 수정은 `sops inventory/dev/group_vars/all.sops.yaml` (에디터에서 평문 편집 → 저장 시 재암호화).
- 키 1개만 스크립트/비대화형으로 추가·변경할 땐 `sops set <file> '["key"]' '"value"'`도 동일하게 기존 recipient로 재암호화된다
  (에디터 세션 없이 단일 키만 바꿀 때 사용, 예: `sops set inventory/dev/group_vars/all.sops.yaml '["vertex_ai_location"]' '"asia-northeast3"'`).
  새 키를 추가했다면 `inventory/secrets.sops.yaml.example`에도 `"REPLACE"` 플레이스홀더로 스키마를 함께 갱신한다(실제 값은 넣지 않음).
- prod도 동일 절차 (`inventory/prod/group_vars/all.sops.yaml`).
- **Cloudflare Origin 인증서**(`cloudflare_origin_cert`/`cloudflare_origin_key`)도 이 시크릿에 PEM으로 넣으면
  Ansible(`tasks/render_certs.yaml`)이 VM `/opt/todakun/caddy/certs/`로 렌더링한다 — **수동 배치 불필요**.
- **딥링크 검증 파일**(App Links / Universal Links)도 동일하게 시크릿에서 렌더링된다
  (`tasks/render_wellknown.yaml` → `/opt/todakun/caddy/well-known/` → Caddy가 `/.well-known/` 아래로 서빙).
  자세한 내용은 아래 "딥링크 검증 파일" 절 참고.

## 딥링크 검증 파일 (assetlinks.json / apple-app-site-association)

```text
SOPS(지문·패키지명·bundleId) ──▶ templates/*.j2 ──▶ VM caddy/well-known/ ──:ro 마운트──▶ Caddy file_server
                                                                                        └ /.well-known/assetlinks.json
                                                                                        └ /.well-known/apple-app-site-association
```

| 키 | dev | prod | 비고 |
|----|-----|------|------|
| `android_package_name` / `android_sha256_fingerprint` | ✅ | ✅ | release 서명 지문 |
| `android_debug_package_name` / `android_debug_sha256_fingerprint` | ✅ | ❌ | **dev 전용** — 운영 도메인에 개발용 서명키 지문 미노출. 넣거나 뺄 땐 **반드시 쌍으로** |
| `apple_bundle_id` (+ 기존 `apple_team_id`) | ✅ | ✅ | `appID = {team_id}.{bundle_id}` |

- JSON **구조**는 git의 `templates/*.j2`에 있어 리뷰 가능하고, **식별자만** SOPS에 있다.
  경로 패턴(현재 iOS `/share/*`)을 바꾸려면 템플릿을 고친다.
- 렌더 결과는 배치 전에 `python3 -c "json.load(...)"`로 유효성 검증된다(깨진 파일이 서빙되는 것 방지).
- 이 파일들은 **`playbook.yaml`(프로비저닝)에서만** 렌더링된다(certs와 동일). 지문을 회전했다면
  `deploy.yaml`만 돌리지 말고 `playbook.yaml`을 먼저 실행할 것. (CI는 provision → deploy 순으로 둘 다 돈다.)
- ⚠️ 이 값들은 SOPS로 **저장소에서만** 보호된다. 검증 파일 자체는 규격상 HTTPS로 **공개 서빙**되므로
  누구나 `https://<도메인>/.well-known/assetlinks.json`으로 조회할 수 있다(딥링크 동작에 필수).
  즉 SOPS는 "git에 평문으로 남지 않게" 하는 장치이지 값 자체를 비밀로 만들지는 않는다.

## 4. CI(GitHub Actions) 연결

- GitHub 저장소 Secret에 **`SOPS_AGE_KEY`** 추가 = `keys.txt`의 **개인키 한 줄**(`AGE-SECRET-KEY-1...`).
- 워크플로가 sops를 설치하고 이 키로 복호화한다(`.github/workflows/deploy-dev.yaml`).

## 5. 로컬에서 배포 실행 시

```bash
# ~/.config/sops/age/keys.txt 가 있으면 sops가 자동으로 찾는다
ansible-playbook -i inventory/dev/hosts.ini playbook.yaml
# 또는 명시적으로
export SOPS_AGE_KEY=$(grep -v '^#' ~/.config/sops/age/keys.txt | grep AGE-SECRET-KEY)
ansible-playbook -i inventory/dev/hosts.ini playbook.yaml
```

## 회전(rotation)

- 값 변경: `sops inventory/dev/group_vars/all.sops.yaml` 편집 → 커밋 → 재배포.
- recipient(팀원/키) 추가·제거: `.sops.yaml` 수정 후 `sops updatekeys inventory/dev/group_vars/all.sops.yaml`.

## 주의

- 복호화된 평문을 파일로 떨구지 말 것(`sops -d > x.yaml` 금지). 편집은 항상 `sops <file>`로.
- `secrets.sops.{env}.yaml`은 **암호화 상태로만** 커밋. 실수로 평문 커밋 시 즉시 전 시크릿 회전.
