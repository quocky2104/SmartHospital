# Token Flow Explanation | Giải Thích Luồng Token

## English

### 1. Overview

The authentication system uses two types of JWT tokens:
- **Access Token**: Short-lived token used to authenticate API requests
- **Refresh Token**: Long-lived token used to obtain a new access token when it expires

---

### 2. Token Definitions

#### Access Token
- **Purpose**: Authenticate user requests to protected endpoints
- **Lifespan**: Short (typically 15 minutes to 1 hour)
- **Payload**: Contains user ID, role, permissions
- **Stored**: In memory or localStorage (client-side)
- **Usage**: Sent in `Authorization: Bearer <access_token>` header
- **Revocation**: Cannot be revoked immediately; expires automatically

#### Refresh Token
- **Purpose**: Obtain a new access token without requiring login again
- **Lifespan**: Long (typically 7-30 days)
- **Payload**: Contains user ID, token version/ID
- **Stored**: In HttpOnly cookie or secure storage (client-side)
- **Usage**: Sent in `POST /auth/refresh` request to get new access token
- **Revocation**: Can be revoked by deleting from blacklist or database

---

### 2.5. Token Storage Details

#### When & Where Access Token is Stored

| Step | Timing | Storage Location | Format | Duration |
|------|--------|------------------|--------|----------|
| 1 | Upon successful login/refresh | Browser memory/state variable | JWT string | Until page reload |
| 2 | Retrieved from login response | React state / Angular service / Vue computed | `accessToken: "eyJhbGc..."` | 15 minutes (server-side expiry) |
| 3 | Before each API request | HTTP Authorization header | `Authorization: Bearer eyJhbGc...` | Sent with every protected request |
| 4 | After page reload | NOT automatically restored* | Must re-fetch from /auth/refresh or re-login | Lost on page refresh |

**Note**: Access Token is intentionally NOT persisted to localStorage/cookies for security. It must be refreshed after page reload using the refresh token.

---

#### When & Where Refresh Token is Stored

| Step | Timing | Storage Location | Format | Duration |
|------|--------|------------------|--------|----------|
| 1 | Upon successful login | HttpOnly, Secure cookie (server-set) | `Set-Cookie: refreshToken=eyJhbGc...` | 7 days (server-side expiry) |
| 2 | Simultaneously | Server-side (Redis/Database) | Record: `{userId, token, expiryDate, blacklisted}` | Until logout or expiry |
| 3 | On every request | Automatically sent by browser | Cookie header (automatic) | Persists across page reloads |
| 4 | During token refresh | Cookie sent to /auth/refresh | Retrieved from HttpOnly cookie | Used to generate new access token |
| 5 | Upon logout | Added to blacklist | Redis/DB: `{token, blacklistedAt}` | Until expiry time reached |

**Note**: Refresh Token is stored as HttpOnly cookie so JavaScript cannot access it (prevents XSS attacks). Automatically sent by browser on requests.

---

### 3. Complete Authentication Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                    INITIAL LOGIN FLOW                           │
└─────────────────────────────────────────────────────────────────┘

User enters credentials
        ↓
POST /auth/login {username, password}
        ↓
Server validates credentials in database
        ↓
✓ Valid → Generate tokens:
        - Generate Access Token (expires in 15 min)
        - Generate Refresh Token (expires in 7 days)
        - Store Refresh Token in Redis/Database
        
✗ Invalid → Return 401 Unauthorized
        ↓
Return to Client:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 900  // seconds
}
        ↓
SERVER ACTIONS:
  - Set-Cookie header: refreshToken (HttpOnly, Secure)
  - Store refreshToken in Redis/DB with userId
  
CLIENT ACTIONS (upon receiving response):
  1. IMMEDIATELY: Store accessToken in memory/state variable
  2. IMMEDIATELY: Browser auto-receives & stores refreshToken in HttpOnly cookie
  3. Ready to make API requests with accessToken
        ↓
Storage State After Login:
  ✓ accessToken in: Memory/React state (volatile)
  ✓ refreshToken in: HttpOnly cookie (persistent, auto-sent)
  ✓ refreshToken in: Server database/Redis (persistent, for validation)
```

---

### 4. Using Access Token to Access Protected Resources

```
┌─────────────────────────────────────────────────────────────────┐
│              ACCESSING PROTECTED ENDPOINTS                      │
└─────────────────────────────────────────────────────────────────┘

Client makes request
        ↓
GET /api/patient/profile
Authorization: Bearer eyJhbGc...
        ↓
Server extracts JWT from Authorization header
        ↓
Server validates token signature
        ↓
Token is valid AND not expired?
        ├─ YES → Extract claims (userId, role)
        │        ↓
        │        Authorization check (role, permissions)
        │        ↓
        │        Return 200 + Resource
        │
        └─ NO → Return 401 Unauthorized
```

---

### 5. Token Refresh Flow (When Access Token Expires)

```
┌─────────────────────────────────────────────────────────────────┐
│              TOKEN REFRESH FLOW                                 │
└─────────────────────────────────────────────────────────────────┘

Access Token expires (15 min passed)
        ↓
Client tries to access protected endpoint
        ↓
Server returns 401 (Invalid/Expired Token)
        ↓
Client detects 401 (token expired)
        ↓
Client calls refresh endpoint:
POST /auth/refresh
Cookie: refreshToken=eyJhbGc...
(or send refreshToken in body)
        ↓
Server extracts refreshToken from cookie/body
        ↓
Validate refreshToken:
├─ Check signature is valid
├─ Check token not expired (7 days not passed)
├─ Check token not blacklisted (logged out)
└─ Check token exists in database/Redis
        ↓
✓ Valid → Generate new access token
         Return {accessToken: "eyJhbGc...", expiresIn: 900}
         
✗ Invalid → Return 401/403 (must login again)
        ↓
CLIENT STORES NEW TOKEN:
  1. IMMEDIATELY: Store new accessToken in memory/state variable
  2. refreshToken remains in HttpOnly cookie (unchanged)
  3. Ready to retry original request with new accessToken
        ↓
Retry original request with new accessToken
        ↓
Request succeeds ✓

Storage State After Refresh:
  ✓ accessToken in: Memory/state (NEW, refreshed)
  ✓ refreshToken in: HttpOnly cookie (UNCHANGED, still valid)
  ✓ refreshToken in: Server database/Redis (UNCHANGED)
```

---

### 6.5. Why Token Blacklist is Critical

#### Problem Without Blacklist
Without blacklisting, even after logout:
- ❌ Refresh token remains VALID (not expired)
- ❌ Attacker with stolen refresh token can still generate new access tokens
- ❌ User cannot immediately revoke session even after logging out
- ❌ Compromised token can be used until natural expiry (7 days later)

#### Why Blacklist Solves This

**Blacklist Purpose**: Maintain a record of revoked (invalidated) tokens that should NO LONGER work

**Why We Need It for Refresh Tokens:**
1. **Immediate Revocation**: JWTs cannot be "unissued" once created. Blacklist acts as a kill switch.
2. **Session Control**: User explicitly logged out and expects complete session termination immediately, not 7 days later.
3. **Security Event Response**: If token is compromised or stolen, blacklist stops attacker instantly without waiting for expiry.
4. **Account Lock**: If account is locked/disabled, blacklist prevents old tokens from working.
5. **Logout Guarantee**: User clicks logout → token becomes instantly invalid, not "will be invalid later."

**Why We Don't Blacklist Access Tokens:**
- Access tokens already expire in 15 minutes (short window)
- Attacker has only 15-minute window before token becomes useless anyway
- Blacklisting every access token would create massive database overhead
- Refresh token blacklist is sufficient (prevents generating new access tokens)

#### Blacklist Mechanism

**Storage**: Redis or PostgreSQL blacklist table
```
BLACKLIST_TABLE {
  token_id: UUID,
  token_hash: string,        // hash of actual token for security
  user_id: UUID,
  blacklisted_at: timestamp, // when it was revoked
  reason: string,            // "LOGOUT", "SECURITY_INCIDENT", "ACCOUNT_LOCKED"
  expires_at: timestamp      // when to remove from blacklist (matches token expiry)
}
```

**Check Process**:
```
When /auth/refresh is called:
  1. Extract refreshToken from request
  2. Validate signature & expiry time
  3. CHECK BLACKLIST: Is this token in blacklist? 
     ├─ YES → Return 401 (Unauthorized, token revoked)
     └─ NO → Generate new access token (proceed normally)
```

**Cleanup**: Blacklist records automatically purged after token expiry date (7 days) to save storage.

---

```
┌─────────────────────────────────────────────────────────────────┐
│              LOGOUT FLOW                                        │
└─────────────────────────────────────────────────────────────────┘

User clicks logout
        ↓
Client calls:
POST /auth/logout
Authorization: Bearer <accessToken>
Cookie: refreshToken=...
        ↓
Server receives logout request
        ↓
Extract userId from accessToken
        ↓
SERVER ACTIONS:
1. Add refreshToken to blacklist in Redis/Database
2. Delete all refresh tokens for this userId from DB
3. Clear user session/online status
4. Remove token from Redis cache
        ↓
Return 200 OK
        ↓
CLIENT ACTIONS (upon successful logout):
  1. DELETE accessToken from memory/state variable
  2. DELETE refreshToken from HttpOnly cookie (via Set-Cookie: max-age=0)
  3. Clear any other session data
  4. Redirect to login page
        ↓
Storage State After Logout:
  ✗ accessToken in: Memory (CLEARED)
  ✗ refreshToken in: Cookie (CLEARED/DELETED)
  ✗ refreshToken in: Server DB (BLACKLISTED/DELETED)
  
Now refreshToken is completely invalid:
- Future attempts to use it will fail (blacklist check fails)
- All API access blocked (requires new login)
- Browser won't auto-send cookie (it was deleted)
```

---

### 7. Security Considerations

| Aspect | Access Token | Refresh Token |
|--------|--------------|---------------|
| **Lifetime** | Short (15 min) | Long (7 days) |
| **Storage** | Memory/localStorage | HttpOnly cookie |
| **Needs Blacklist?** | NO (expires too quickly) | YES (long-lived, needs revocation) |
| **Attack Impact** | Limited by short expiry (max 15 min exposure) | HIGH without blacklist (up to 7 days exposure) |
| **Revocation Method** | Auto-expire | Manual blacklist + cleanup after expiry |
| **Blacklist Benefit** | N/A | Immediate session termination on logout |

---

### 8. Blacklist Use Cases

#### Case 1: User Clicks Logout (Expected)
```
User logs out voluntarily
  ↓
refreshToken added to blacklist
  ↓
User navigates back to app
  ↓
Browser still has old refreshToken in cookie
  ↓
Client tries /auth/refresh
  ↓
Server checks: Is token blacklisted? YES ✓
  ↓
Return 401 → Client redirects to login ✓
```

#### Case 2: Security Incident (Token Stolen)
```
Admin detects unauthorized login from IP address
  ↓
Admin revokes user session (calls internal API)
  ↓
User's all refresh tokens added to blacklist with reason: "SECURITY_INCIDENT"
  ↓
Attacker tries to use stolen refreshToken
  ↓
Server checks: Is token blacklisted? YES ✓
  ↓
Return 401 → Attack prevented ✓
```

#### Case 3: Account Compromised / Password Changed
```
User changes password
  ↓
All refresh tokens for this user added to blacklist (reason: "PASSWORD_CHANGED")
  ↓
Attacker with old refreshToken tries to access app
  ↓
Server checks: Is token blacklisted? YES ✓
  ↓
Return 401 → Old session invalidated ✓
  ↓
User must login again with new password
```

#### Case 4: Logout on Another Device (Multi-Device)
```
User logs in on Phone, Tablet, Desktop (3 sessions)
  ↓
User clicks "Logout from all devices"
  ↓
All 3 refreshTokens added to blacklist
  ↓
All devices become logged out simultaneously ✓
```

---

### 9. Best Practices Implemented

✅ **Short-lived Access Tokens**: Minimize damage if token is stolen (15-min window)
✅ **Long-lived Refresh Tokens**: Convenience for users (don't require login every 15 min)
✅ **Blacklist for Refresh Tokens**: Enables immediate revocation without waiting for expiry
✅ **HttpOnly Cookies**: Prevent XSS attacks from accessing refresh token (JavaScript cannot read it)
✅ **Signature Verification**: Validate token wasn't tampered with (cryptographic check)
✅ **Expiry Checks**: Reject expired tokens even if not blacklisted (defense-in-depth)
✅ **Database Validation**: Verify token record exists (prevents replay attacks, enables multi-device logout)
✅ **Audit Logging**: Log reason for blacklist (LOGOUT, SECURITY_INCIDENT, PASSWORD_CHANGED, etc.)
✅ **Automatic Cleanup**: Remove blacklist entries after token expiry (saves storage)

---

## Vietnamese | Tiếng Việt

### 1. Tổng Quan

Hệ thống xác thực sử dụng hai loại token JWT:
- **Access Token**: Token ngắn hạn dùng để xác thực các yêu cầu API
- **Refresh Token**: Token dài hạn dùng để lấy access token mới khi hết hạn

---

### 2. Định Nghĩa Token

#### Access Token
- **Mục đích**: Xác thực các yêu cầu của người dùng tới các endpoint được bảo vệ
- **Thời hạn**: Ngắn (thường 15 phút đến 1 giờ)
- **Nội dung**: Chứa ID người dùng, vai trò, quyền hạn
- **Lưu trữ**: Trong bộ nhớ hoặc localStorage (phía client)
- **Sử dụng**: Gửi trong header `Authorization: Bearer <access_token>`
- **Thu hồi**: Không thể thu hồi ngay lập tức; hết hạn tự động

#### Refresh Token
- **Mục đích**: Lấy access token mới mà không cần đăng nhập lại
- **Thời hạn**: Dài (thường 7-30 ngày)
- **Nội dung**: Chứa ID người dùng, phiên bản/ID token
- **Lưu trữ**: Trong cookie HttpOnly hoặc lưu trữ bảo mật (phía client)
- **Sử dụng**: Gửi trong yêu cầu `POST /auth/refresh` để lấy access token mới
- **Thu hồi**: Có thể thu hồi bằng cách xóa khỏi danh sách đen hoặc cơ sở dữ liệu

---

### 2.5. Chi Tiết Lưu Trữ Token

#### Khi & Nơi Access Token Được Lưu Trữ

| Bước | Thời Điểm | Vị Trí Lưu Trữ | Định Dạng | Thời Gian Tồn Tại |
|------|----------|-----------------|-----------|------------------|
| 1 | Khi đăng nhập/làm mới thành công | Bộ nhớ trình duyệt/biến state | JWT string | Cho đến khi tải lại trang |
| 2 | Lấy từ response đăng nhập | React state / Angular service / Vue computed | `accessToken: "eyJhbGc..."` | 15 phút (hết hạn phía server) |
| 3 | Trước mỗi yêu cầu API | HTTP Authorization header | `Authorization: Bearer eyJhbGc...` | Gửi với mỗi yêu cầu được bảo vệ |
| 4 | Sau khi tải lại trang | KHÔNG được khôi phục tự động* | Phải làm mới từ /auth/refresh hoặc đăng nhập lại | Mất khi tải lại trang |

**Lưu ý**: Access Token không được lưu trữ vào localStorage/cookies để bảo mật. Nó phải được làm mới sau khi tải lại trang bằng cách sử dụng refresh token.

---

#### Khi & Nơi Refresh Token Được Lưu Trữ

| Bước | Thời Điểm | Vị Trí Lưu Trữ | Định Dạng | Thời Gian Tồn Tại |
|------|----------|-----------------|-----------|------------------|
| 1 | Khi đăng nhập thành công | Cookie HttpOnly, Secure (được server thiết lập) | `Set-Cookie: refreshToken=eyJhbGc...` | 7 ngày (hết hạn phía server) |
| 2 | Đồng thời | Phía server (Redis/Database) | Bản ghi: `{userId, token, expiryDate, blacklisted}` | Cho đến khi đăng xuất hoặc hết hạn |
| 3 | Trên mỗi yêu cầu | Được gửi tự động bởi trình duyệt | Cookie header (tự động) | Persists across page reloads |
| 4 | Khi làm mới token | Cookie được gửi tới /auth/refresh | Lấy từ HttpOnly cookie | Dùng để tạo access token mới |
| 5 | Khi đăng xuất | Thêm vào danh sách đen | Redis/DB: `{token, blacklistedAt}` | Cho đến khi hết hạn |

**Lưu ý**: Refresh Token được lưu trữ dưới dạng HttpOnly cookie nên JavaScript không thể truy cập nó (ngăn chặn tấn công XSS). Được tự động gửi bởi trình duyệt trên các yêu cầu.

---

### 3. Luồng Xác Thực Hoàn Chỉnh

```
┌─────────────────────────────────────────────────────────────────┐
│                    LUỒNG ĐĂNG NHẬP BAN ĐẦU                     │
└─────────────────────────────────────────────────────────────────┘

Người dùng nhập thông tin đăng nhập
        ↓
POST /auth/login {tên_đăng_nhập, mật_khẩu}
        ↓
Server kiểm tra thông tin đăng nhập trong cơ sở dữ liệu
        ↓
✓ Hợp lệ → Tạo tokens:
        - Tạo Access Token (hết hạn trong 15 phút)
        - Tạo Refresh Token (hết hạn trong 7 ngày)
        - Lưu trữ Refresh Token trong Redis/Database
        
✗ Không hợp lệ → Trả về 401 Unauthorized
        ↓
Trả về Client:
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 900  // giây
}
        ↓
CÁC HÀNH ĐỘNG CỦA SERVER:
  - Set-Cookie header: refreshToken (HttpOnly, Secure)
  - Lưu trữ refreshToken trong Redis/DB với userId
  
CÁC HÀNH ĐỘNG CỦA CLIENT (khi nhận được response):
  1. NGAY LẬP TỨC: Lưu trữ accessToken trong bộ nhớ/biến state
  2. NGAY LẬP TỨC: Trình duyệt tự động nhận & lưu trữ refreshToken trong HttpOnly cookie
  3. Sẵn sàng tạo yêu cầu API với accessToken
        ↓
Trạng Thái Lưu Trữ Sau Khi Đăng Nhập:
  ✓ accessToken trong: Bộ nhớ/React state (volatile)
  ✓ refreshToken trong: HttpOnly cookie (persistent, tự động gửi)
  ✓ refreshToken trong: Server database/Redis (persistent, để xác thực)
```

---

### 4. Sử Dụng Access Token để Truy Cập Tài Nguyên Được Bảo Vệ

```
┌─────────────────────────────────────────────────────────────────┐
│           TRUY CẬP CÁC ENDPOINT ĐƯỢC BẢO VỆ                    │
└─────────────────────────────────────────────────────────────────┘

Client gửi yêu cầu
        ↓
GET /api/patient/profile
Authorization: Bearer eyJhbGc...
        ↓
Server trích xuất JWT từ header Authorization
        ↓
Server xác thực chữ ký token
        ↓
Token hợp lệ VÀ chưa hết hạn?
        ├─ CÓ → Trích xuất thông tin (userId, vai trò)
        │       ↓
        │       Kiểm tra quyền hạn (vai trò, quyền)
        │       ↓
        │       Trả về 200 + Tài nguyên
        │
        └─ KHÔNG → Trả về 401 Unauthorized
```

---

### 5. Luồng Làm Mới Token (Khi Access Token Hết Hạn)

```
┌─────────────────────────────────────────────────────────────────┐
│              LUỒNG LÀM MỚI TOKEN                                │
└─────────────────────────────────────────────────────────────────┘

Access Token hết hạn (15 phút đã trôi qua)
        ↓
Client cố gắng truy cập endpoint được bảo vệ
        ↓
Server trả về 401 (Token không hợp lệ/hết hạn)
        ↓
Client phát hiện 401 (token hết hạn)
        ↓
Client gọi endpoint làm mới:
POST /auth/refresh
Cookie: refreshToken=eyJhbGc...
(hoặc gửi refreshToken trong body)
        ↓
Server trích xuất refreshToken từ cookie/body
        ↓
Xác thực refreshToken:
├─ Kiểm tra chữ ký có hợp lệ
├─ Kiểm tra token chưa hết hạn (7 ngày chưa qua)
├─ Kiểm tra token không bị đưa vào danh sách đen (đã đăng xuất)
└─ Kiểm tra token tồn tại trong database/Redis
        ↓
✓ Hợp lệ → Tạo access token mới
          Trả về {accessToken: "eyJhbGc...", expiresIn: 900}
          
✗ Không hợp lệ → Trả về 401/403 (phải đăng nhập lại)
        ↓
CLIENT LƯU TRỮ TOKEN MỚI:
  1. NGAY LẬP TỨC: Lưu trữ accessToken mới trong bộ nhớ/biến state
  2. refreshToken vẫn ở trong HttpOnly cookie (không thay đổi)
  3. Sẵn sàng thử lại yêu cầu ban đầu với accessToken mới
        ↓
Thử lại yêu cầu ban đầu với accessToken mới
        ↓
Yêu cầu thành công ✓

Trạng Thái Lưu Trữ Sau Khi Làm Mới:
  ✓ accessToken trong: Bộ nhớ/state (MỚI, đã làm mới)
  ✓ refreshToken trong: HttpOnly cookie (KHÔNG THAY ĐỔI, vẫn hợp lệ)
  ✓ refreshToken trong: Server database/Redis (KHÔNG THAY ĐỔI)
```

---

### 6. Luồng Đăng Xuất

```
┌─────────────────────────────────────────────────────────────────┐
│              LUỒNG ĐĂNG XUẤT                                    │
└─────────────────────────────────────────────────────────────────┘

Người dùng nhấp vào đăng xuất
        ↓
Client gọi:
POST /auth/logout
Authorization: Bearer <accessToken>
Cookie: refreshToken=...
        ↓
Server nhận yêu cầu đăng xuất
        ↓
Trích xuất userId từ accessToken
        ↓
CÁC HÀNH ĐỘNG CỦA SERVER:
1. Thêm refreshToken vào danh sách đen trong Redis/Database
2. Xóa tất cả refresh tokens của người dùng này khỏi DB
3. Xóa phiên/trạng thái trực tuyến của người dùng
4. Xóa token khỏi Redis cache
        ↓
Trả về 200 OK
        ↓
CÁC HÀNH ĐỘNG CỦA CLIENT (khi đăng xuất thành công):
  1. XÓA accessToken khỏi bộ nhớ/biến state
  2. XÓA refreshToken khỏi HttpOnly cookie (qua Set-Cookie: max-age=0)
  3. Xóa bất kỳ dữ liệu phiên nào khác
  4. Chuyển hướng đến trang đăng nhập
        ↓
Trạng Thái Lưu Trữ Sau Khi Đăng Xuất:
  ✗ accessToken trong: Bộ nhớ (ĐÃ XÓA)
  ✗ refreshToken trong: Cookie (ĐÃ XÓA/ĐÃ HỦY)
  ✗ refreshToken trong: Server DB (ĐÃ ĐƯA VÀO DANH SÁCH ĐEN/ĐÃ XÓA)
  
Bây giờ refreshToken hoàn toàn không hợp lệ:
- Những cố gắng sử dụng nó trong tương lai sẽ không thành công (kiểm tra danh sách đen thất bại)
- Tất cả các truy cập API bị chặn (yêu cầu đăng nhập mới)
- Trình duyệt sẽ không tự động gửi cookie (nó đã bị xóa)
```

---

### 6.5. Tại Sao Danh Sách Đen Token Rất Quan Trọng

#### Vấn Đề Khi Không Có Danh Sách Đen
Mà không có danh sách đen, ngay cả sau khi đăng xuất:
- ❌ Refresh token vẫn HỢP LỆ (chưa hết hạn)
- ❌ Kẻ tấn công có token bị đánh cắp vẫn có thể tạo access tokens mới
- ❌ Người dùng không thể hủy phiên ngay lập tức dù đã đăng xuất
- ❌ Token bị xâm phạm có thể được sử dụng cho đến khi hết hạn tự nhiên (7 ngày sau)

#### Tại Sao Danh Sách Đen Giải Quyết Điều Này

**Mục Đích Danh Sách Đen**: Duy trì bản ghi các token bị thu hồi (vô hiệu hóa) không nên còn hoạt động

**Tại Sao Chúng Ta Cần Nó cho Refresh Tokens:**
1. **Thu Hồi Ngay Lập Tức**: JWT không thể "hủy phát hành" một khi đã tạo. Danh sách đen là công tắc tắt khẩn cấp.
2. **Kiểm Soát Phiên**: Người dùng đã rõ ràng đăng xuất và mong đợi chấm dứt phiên hoàn toàn ngay lập tức, không phải 7 ngày sau.
3. **Ứng Phó Sự Cố Bảo Mật**: Nếu token bị xâm phạm hoặc bị đánh cắp, danh sách đen sẽ ngăn chặn kẻ tấn công ngay lập tức mà không cần chờ hết hạn.
4. **Khóa Tài Khoản**: Nếu tài khoản bị khóa/vô hiệu hóa, danh sách đen ngăn token cũ hoạt động.
5. **Đảm Bảo Đăng Xuất**: Người dùng nhấp đăng xuất → token trở nên vô hiệu hóa ngay lập tức, không phải "sẽ vô hiệu hóa sau này."

**Tại Sao Chúng Ta Không Đưa Access Tokens Vào Danh Sách Đen:**
- Access tokens đã hết hạn trong 15 phút (cửa sổ ngắn)
- Kẻ tấn công chỉ có 15 phút trước khi token trở nên vô dụng
- Đưa mỗi access token vào danh sách đen sẽ tạo ra chi phí lưu trữ cơ sở dữ liệu khổng lồ
- Danh sách đen refresh token là đủ (ngăn tạo access tokens mới)

#### Cơ Chế Danh Sách Đen

**Lưu Trữ**: Redis hoặc bảng danh sách đen PostgreSQL
```
BLACKLIST_TABLE {
  token_id: UUID,
  token_hash: string,        // mã hóa của token thực tế để bảo mật
  user_id: UUID,
  blacklisted_at: timestamp, // khi nó được thu hồi
  reason: string,            // "LOGOUT", "SECURITY_INCIDENT", "ACCOUNT_LOCKED"
  expires_at: timestamp      // khi xóa khỏi danh sách đen (khớp với hết hạn token)
}
```

**Quá Trình Kiểm Tra**:
```
Khi /auth/refresh được gọi:
  1. Trích xuất refreshToken từ yêu cầu
  2. Xác thực chữ ký & thời gian hết hạn
  3. KIỂM TRA DANH SÁCH ĐEN: Token này có trong danh sách đen không?
     ├─ CÓ → Trả về 401 (Unauthorized, token bị thu hồi)
     └─ KHÔNG → Tạo access token mới (tiếp tục bình thường)
```

**Dọn Dẹp**: Bản ghi danh sách đen tự động xóa sau ngày hết hạn token (7 ngày) để tiết kiệm lưu trữ.

---

| Khía Cạnh | Access Token | Refresh Token |
|----------|--------------|---------------|
| **Thời gian tồn tại** | Ngắn (15 phút) | Dài (7 ngày) |
| **Lưu trữ** | Bộ nhớ/localStorage | HttpOnly cookie |
| **Cần Danh Sách Đen?** | KHÔNG (hết hạn quá nhanh) | CÓ (dài hạn, cần thu hồi) |
| **Tác động của cuộc tấn công** | Hạn chế bởi hết hạn ngắn (tối đa 15 phút tiếp xúc) | CAO nếu không có danh sách đen (lên đến 7 ngày tiếp xúc) |
| **Phương Pháp Thu Hồi** | Hết hạn tự động | Danh sách đen thủ công + dọn dẹp sau hết hạn |
| **Lợi Ích Danh Sách Đen** | N/A | Chấm dứt phiên ngay lập tức khi đăng xuất |

---

### 8. Các Trường Hợp Sử Dụng Danh Sách Đen

#### Trường Hợp 1: Người Dùng Nhấp Đăng Xuất (Như Dự Kiến)
```
Người dùng đăng xuất tự nguyện
  ↓
refreshToken được thêm vào danh sách đen
  ↓
Người dùng quay trở lại ứng dụng
  ↓
Trình duyệt vẫn có refreshToken cũ trong cookie
  ↓
Client cố gắng /auth/refresh
  ↓
Server kiểm tra: Token này có trong danh sách đen không? CÓ ✓
  ↓
Trả về 401 → Client chuyển hướng đến đăng nhập ✓
```

#### Trường Hợp 2: Sự Cố Bảo Mật (Token Bị Đánh Cắp)
```
Admin phát hiện đăng nhập trái phép từ địa chỉ IP
  ↓
Admin thu hồi phiên người dùng (gọi API nội bộ)
  ↓
Tất cả refresh tokens của người dùng được thêm vào danh sách đen với lý do: "SECURITY_INCIDENT"
  ↓
Kẻ tấn công cố gắng sử dụng refreshToken bị đánh cắp
  ↓
Server kiểm tra: Token này có trong danh sách đen không? CÓ ✓
  ↓
Trả về 401 → Cuộc tấn công bị ngăn chặn ✓
```

#### Trường Hợp 3: Tài Khoản Bị Xâm Phạm / Đổi Mật Khẩu
```
Người dùng đổi mật khẩu
  ↓
Tất cả refresh tokens cho người dùng này được thêm vào danh sách đen (lý do: "PASSWORD_CHANGED")
  ↓
Kẻ tấn công có refreshToken cũ cố gắng truy cập ứng dụng
  ↓
Server kiểm tra: Token này có trong danh sách đen không? CÓ ✓
  ↓
Trả về 401 → Phiên cũ bị vô hiệu hóa ✓
  ↓
Người dùng phải đăng nhập lại bằng mật khẩu mới
```

#### Trường Hợp 4: Đăng Xuất Trên Tất Cả Thiết Bị (Đa Thiết Bị)
```
Người dùng đăng nhập trên Điện thoại, Máy tính bảng, Máy tính để bàn (3 phiên)
  ↓
Người dùng nhấp vào "Đăng xuất từ tất cả thiết bị"
  ↓
Tất cả 3 refreshTokens được thêm vào danh sách đen
  ↓
Tất cả thiết bị trở thành đăng xuất đồng thời ✓
```

---

### 9. Các Thực Tiễn Tốt Nhất Được Thực Hiện

✅ **Access Tokens Ngắn Hạn**: Giảm thiểu thiệt hại nếu token bị đánh cắp (15 phút cửa sổ)
✅ **Refresh Tokens Dài Hạn**: Tiện lợi cho người dùng (không cần đăng nhập mỗi 15 phút)
✅ **Danh Sách Đen cho Refresh Tokens**: Cho phép thu hồi ngay lập tức mà không cần chờ hết hạn
✅ **HttpOnly Cookies**: Ngăn chặn cuộc tấn công XSS từ việc truy cập refresh token (JavaScript không thể đọc nó)
✅ **Xác Thực Chữ Ký**: Xác thực token chưa bị giả mạo (kiểm tra mật mã)
✅ **Kiểm Tra Hết Hạn**: Từ chối các token hết hạn ngay cả khi không có danh sách đen (phòng chữa)
✅ **Xác Thực Cơ Sở Dữ Liệu**: Xác minh bản ghi token tồn tại (ngăn chặn cuộc tấn công phát lại, cho phép đăng xuất trên tất cả thiết bị)
✅ **Ghi Nhật Ký Kiểm Tra**: Ghi lý do danh sách đen (LOGOUT, SECURITY_INCIDENT, PASSWORD_CHANGED, v.v.)
✅ **Dọn Dẹp Tự Động**: Xóa mục danh sách đen sau hết hạn token (tiết kiệm lưu trữ)

---

## Diagram Flow

```
TIMELINE VIEW
=============

│ Time
│
├─ T=0s     User logs in → Get AccessToken (expires in 900s) + RefreshToken (expires in 7 days)
│
├─ T=300s   User makes request with AccessToken ✓ Valid
│
├─ T=600s   User makes request with AccessToken ✓ Valid
│
├─ T=900s   AccessToken expires
│           User makes request → 401
│           Client calls /refresh with RefreshToken ✓ Valid
│           Get new AccessToken (expires in 900s)
│
├─ T=1200s  User makes request with new AccessToken ✓ Valid
│
└─ T=...    User logs out → RefreshToken added to blacklist
            Future /refresh calls → 401
            User must login again
```

---

**Document Version**: 1.0  
**Last Updated**: May 2, 2026  
**Tech Stack**: Spring Boot 4.0.0, Spring Security 7.0.0, JWT (JSON Web Tokens)
