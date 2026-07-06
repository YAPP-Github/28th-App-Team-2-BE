# Cloudflare Origin Certificate (SOPS로 관리)

Caddy의 origin TLS 인증서는 **SOPS 시크릿에 넣고 Ansible이 이 경로(`/opt/todakun/caddy/certs/`)로 렌더링**한다.
VM에 수동 배치(scp·mkdir)하지 않는다. 이 디렉터리의 로컬 `*.pem`/`*.key`는 **SOPS에 붙여넣기 위한 원본**일 뿐이며
`.gitignore` 처리되어 커밋되지 않는다.

## 발급 → SOPS 등록

1. Cloudflare 대시보드 → **SSL/TLS → Origin Server → Create Certificate**
   - 호스트명: **`*.todakun.com`** (와일드카드 — 비밀 서브도메인을 CT에 노출 않으려면 필수)
   - 키 타입: RSA 또는 ECDSA
2. 발급된 PEM 두 값을 SOPS 시크릿에 붙여넣는다(`deploy/ansible/SECRETS.md` 참고):
   ```yaml
   # secrets.sops.dev.yaml (평문 편집 → sops 재암호화)
   cloudflare_origin_cert: |
     -----BEGIN CERTIFICATE-----
     ...
   cloudflare_origin_key: |
     -----BEGIN PRIVATE KEY-----
     ...
   ```
3. Cloudflare **SSL/TLS → Overview → Full (Strict)** 로 설정.

배포(playbook) 시 Ansible이 `tasks/render_certs.yaml`로 `cloudflare-origin.pem`(0644) / `cloudflare-origin.key`(0600, root)를
VM에 생성한다. 파일명은 `Caddyfile.template`의 `tls` 지시자와 일치한다.

> Origin Certificate는 Cloudflare Origin CA 서명 — 공개 신뢰 CA가 아니라 **공개 CT 로그에 남지 않는다**.
> Cloudflare만 origin에 접속하고 Full(Strict)에서 이 인증서를 신뢰한다.