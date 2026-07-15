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
- prod도 동일 절차 (`inventory/prod/group_vars/all.sops.yaml`).
- **Cloudflare Origin 인증서**(`cloudflare_origin_cert`/`cloudflare_origin_key`)도 이 시크릿에 PEM으로 넣으면
  Ansible(`tasks/render_certs.yaml`)이 VM `/opt/todakun/caddy/certs/`로 렌더링한다 — **수동 배치 불필요**.

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
