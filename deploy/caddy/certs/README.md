# Cloudflare Origin Certificate 배치

Caddy가 origin TLS에 사용하는 **Cloudflare Origin Certificate**를 여기에 둔다.
개인키를 포함하므로 **git에 커밋하지 않는다**(`.gitignore` 처리됨). VM에 수동 배치한다.

## 발급 & 배치

1. Cloudflare 대시보드 → **SSL/TLS → Origin Server → Create Certificate**
   - 호스트명: **`*.todakun.com`** (와일드카드 — 비밀 서브도메인을 노출하지 않으려면 반드시 와일드카드로)
   - 키 타입: RSA 또는 ECDSA
2. 발급된 두 값을 VM의 `/opt/todakun/caddy/certs/`에 저장:
   - Origin Certificate(PEM) → `cloudflare-origin.pem`
   - Private Key → `cloudflare-origin.key`
3. 권한: `chmod 600 cloudflare-origin.key`
4. Cloudflare **SSL/TLS → Overview → Full (Strict)** 로 설정.

파일명은 `Caddyfile.template`의 `tls` 지시자와 일치해야 한다:
`/etc/caddy/certs/cloudflare-origin.pem`, `/etc/caddy/certs/cloudflare-origin.key`.

> Origin Certificate는 Cloudflare Origin CA가 서명 — 공개 신뢰 CA가 아니라 **공개 CT 로그에 남지 않는다**.
> Cloudflare만 origin에 접속하고 Full(Strict)에서 이 인증서를 신뢰하므로 문제없다.