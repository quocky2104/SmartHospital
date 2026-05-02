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
Client stores:
- accessToken → memory/state
- refreshToken → HttpOnly cookie or secure storage
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
Client stores new accessToken
        ↓
Retry original request with new accessToken
        ↓
Request succeeds ✓
```

---

### 6. Logout Flow

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
Actions:
1. Add refreshToken to blacklist (Redis/Database)
2. Delete all refresh tokens for this userId
3. Clear user session/online status
4. Remove token from Redis cache
        ↓
Return 200 OK
        ↓
Client:
1. Clear localStorage/memory (accessToken)
2. Delete cookie (refreshToken)
3. Redirect to login page
        ↓
Now refreshToken is invalid:
- Future attempts to use it will fail
- All accesses require new login
```

---

### 7. Security Considerations

| Aspect | Access Token | Refresh Token |
|--------|--------------|---------------|
| **Lifetime** | Short (15 min) | Long (7 days) |
| **Storage** | Memory/localStorage | HttpOnly cookie |
| **Attack Impact** | Limited by short expiry | Mitigated by rotation/blacklist |
| **Signature Validation** | Required | Required |
| **Blacklist Check** | Optional | Required |
| **Revocation** | Auto-expire | Manual blacklist |

---

### 8. Best Practices Implemented

✅ **Short-lived Access Tokens**: Minimize damage if token is stolen
✅ **HttpOnly Cookies**: Prevent XSS attacks from accessing refresh token
✅ **Token Rotation**: New refresh token can be issued on refresh
✅ **Blacklist on Logout**: Immediately invalidate refresh tokens
✅ **Signature Verification**: Validate token hasn't been tampered
✅ **Expiry Checks**: Reject expired tokens
✅ **Database Validation**: Verify token record exists (prevents replay attacks)

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
Client lưu trữ:
- accessToken → bộ nhớ/state
- refreshToken → HttpOnly cookie hoặc lưu trữ bảo mật
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
Client lưu trữ accessToken mới
        ↓
Thử lại yêu cầu ban đầu với accessToken mới
        ↓
Yêu cầu thành công ✓
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
Các hành động:
1. Thêm refreshToken vào danh sách đen (Redis/Database)
2. Xóa tất cả refresh tokens của người dùng này
3. Xóa phiên/trạng thái trực tuyến của người dùng
4. Xóa token khỏi Redis cache
        ↓
Trả về 200 OK
        ↓
Client:
1. Xóa localStorage/bộ nhớ (accessToken)
2. Xóa cookie (refreshToken)
3. Chuyển hướng đến trang đăng nhập
        ↓
Bây giờ refreshToken không hợp lệ:
- Những cố gắng sử dụng nó trong tương lai sẽ không thành công
- Tất cả các truy cập đều yêu cầu đăng nhập mới
```

---

### 7. Các Xem Xét Bảo Mật

| Khía Cạnh | Access Token | Refresh Token |
|----------|--------------|---------------|
| **Thời gian tồn tại** | Ngắn (15 phút) | Dài (7 ngày) |
| **Lưu trữ** | Bộ nhớ/localStorage | HttpOnly cookie |
| **Tác động của cuộc tấn công** | Hạn chế do thời gian ngắn | Giảm thiểu bằng xoay vòng/danh sách đen |
| **Xác thực chữ ký** | Cần thiết | Cần thiết |
| **Kiểm tra danh sách đen** | Tùy chọn | Cần thiết |
| **Thu hồi** | Hết hạn tự động | Thu hồi thручных |

---

### 8. Các Thực Tiễn Tốt Nhất Được Thực Hiện

✅ **Access Tokens Ngắn Hạn**: Giảm thiểu thiệt hại nếu token bị đánh cắp
✅ **HttpOnly Cookies**: Ngăn chặn các cuộc tấn công XSS từ việc truy cập refresh token
✅ **Xoay Vòng Token**: Refresh token mới có thể được cấp khi làm mới
✅ **Danh Sách Đen Khi Đăng Xuất**: Ngay lập tức vô hiệu hóa refresh tokens
✅ **Xác Thực Chữ Ký**: Xác thực token chưa bị giả mạo
✅ **Kiểm Tra Hết Hạn**: Từ chối các token hết hạn
✅ **Xác Thực Cơ Sở Dữ Liệu**: Xác minh bản ghi token tồn tại (ngăn chặn các cuộc tấn công phát lại)

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
