# Backend API

Base: `https://<server>/api`. JSON. Auth: `Authorization: Bearer <token>`.

| Method | Path | Body | Response |
|---|---|---|---|
| GET | /health (no /api) | – | `{ok:true}` |
| POST | /auth/register | `{shopName, mobile(10 digit 6-9), password(8-128)}` | 201 `{token}`; 403 agar SINGLE_OWNER aur owner maujood; 409 mobile duplicate |
| POST | /auth/login | `{mobile, password}` | `{token}`; 401 galat details |
| GET | /backup | – | `{version, updatedAt, payload}`; 404 agar backup nahi |
| PUT | /backup | `{baseVersion, payload}` | `{version}`; 409 agar `baseVersion` server se alag; 400 invalid |

- `payload` = app ka JSON backup (string), `format == "GramCall-AI-backup"`, max 20 MB.
- Pehli baar `baseVersion = 0`. Har successful upload par `version + 1`.
- Rate limit: auth 10 / 15 min, baaki 300 / 15 min per IP. Passwords bcrypt (cost 12). JWT HS256, 30 din.
- Audit: register, login (safal/asafal), backup upload/download `audit_logs` table mein.
- Saari SQL parameterized hai (injection se bachav).
