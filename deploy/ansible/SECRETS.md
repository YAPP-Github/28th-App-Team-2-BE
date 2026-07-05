# 시크릿 관리 (SOPS + age)

VM `.env`는 **수동 배치하지 않는다.** 시크릿은 `inventory/group_vars/all/secrets.sops.yaml`(SOPS 암호화, git 커밋)에
두고, Ansible이 **컨트롤러(CI/로컬)에서 복호화**해 VM의 `/opt/todakun/.env`로 렌더링한다(VM엔 age 키 불필요).

```
secrets.sops.yaml (암호화, git) ──sops 복호화(컨트롤러)──▶ Ansible vars ──env.j2──▶ /opt/todakun/.env (VM, 0600)
```

## 1. 도구 설치 (로컬, 최초 1회)

```bash
brew install sops age            # macOS (또는 각 OS 패키지)
ansible-galaxy collection install -r collections/requirements.yml
```

## 2. age 키페어 생성

```bash
age-keygen -o ~/.config/sops/age/keys.txt
# 출력된 "Public key: age1...." 를 복사 → .sops.yaml의 age 필드에 붙여넣기
```

- **개인키(`keys.txt`)**: 절대 커밋 금지(gitignore됨). 팀은 안전한 채널로 공유하거나 각자 키를 recipient에 추가.
- **공개키(`age1...`)**: `.sops.yaml`의 `age:`에 기입(여러 명이면 콤마로 나열).

## 3. 시크릿 작성 & 암호화

```bash
cd deploy/ansible
cp inventory/group_vars/all/secrets.sops.yaml.example inventory/group_vars/all/secrets.sops.yaml
# 실제 값(DB 비번, GC_TOKEN, DOMAIN, ghcr_user/ghcr_pat 등) 채운 뒤:
sops -e -i inventory/group_vars/all/secrets.sops.yaml    # 제자리 암호화
git add inventory/group_vars/all/secrets.sops.yaml       # 암호문이라 커밋 안전
```

- 이후 수정은 `sops inventory/group_vars/all/secrets.sops.yaml` (에디터에서 평문 편집 → 저장 시 재암호화).

## 4. CI(GitHub Actions) 연결

- GitHub 저장소 Secret에 **`SOPS_AGE_KEY`** 추가 = `keys.txt`의 **개인키 한 줄**(`AGE-SECRET-KEY-1...`).
- 워크플로가 sops를 설치하고 이 키로 복호화한다(`.github/workflows/deploy-dev.yml`).

## 5. 로컬에서 배포 실행 시

```bash
export SOPS_AGE_KEY=$(grep -v '^#' ~/.config/sops/age/keys.txt | grep AGE-SECRET-KEY)
ansible-playbook -i inventory/hosts.ini playbook.yml
```
(또는 `~/.config/sops/age/keys.txt`가 있으면 sops가 자동으로 찾는다.)

## 회전(rotation)

- 값 변경: `sops secrets.sops.yaml` 편집 → 커밋 → 재배포(`deploy.yml`이 `.env` 재렌더).
- recipient(팀원/키) 추가·제거: `.sops.yaml` 수정 후 `sops updatekeys inventory/group_vars/all/secrets.sops.yaml`.

## 주의

- 복호화된 평문을 파일로 떨구지 말 것(`sops -d > x.yaml` 금지). 편집은 항상 `sops <file>`로.
- `secrets.sops.yaml`은 **암호화 상태로만** 커밋. 실수로 평문 커밋 시 즉시 전 시크릿 회전.