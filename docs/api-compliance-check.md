# API 规范一致性检查报告

## 检查日期：2026-06-15

## 问题概述

服务器端 Android 实现与 OpenAPI spec 存在多处不一致，导致客户端解析错误。

---

## 1. UserAdminResponseDto 不一致

### OpenAPI Spec 定义

| 字段 | 类型 | 格式/约束 | 必需 |
|------|------|----------|------|
| `id` | String | UUID 格式 | ✅ |
| `email` | String | email 格式 | ✅ |
| `name` | String | - | ✅ |
| `avatarColor` | Enum | `primary`, `pink`, `red`, `yellow`, `blue`, `green`, `purple`, `orange`, `gray`, `amber` | ✅ |
| `createdAt` | String | date-time (ISO 8601) | ✅ |
| `deletedAt` | String? | date-time, nullable | ✅ (nullable) |
| `isAdmin` | Boolean | - | ✅ |
| `license` | UserLicense? | 对象，不是 String | ❌ |
| `oauthId` | String | - | ✅ |
| `profileChangedAt` | String? | date-time, 不是 Long | ❌ |
| `profileImagePath` | String | - | ✅ |
| `quotaSizeInBytes` | Long? | integer, nullable | ❌ |
| `quotaUsageInBytes` | Long? | integer, nullable | ❌ |
| `shouldChangePassword` | Boolean | - | ✅ |
| `status` | Enum | `active`, `removing`, `deleted` | ✅ |
| `storageLabel` | String? | nullable | ❌ |
| `updatedAt` | String | date-time | ✅ |

### 服务器端当前实现问题

| 字段 | 当前实现 | 问题 |
|------|----------|------|
| `id` | `"admin-user-id"` | ❌ 不是 UUID 格式 |
| `license` | `String?` | ❌ 应为 `UserLicense?` 对象 |
| `profileChangedAt` | `Long?` | ❌ 应为 `String?` (ISO 8601) |

---

## 2. LoginResponse 不一致

### OpenAPI Spec 定义

需要检查 `LoginResponse` 是否与 spec 一致。

### 服务器端当前实现

```kotlin
data class LoginResponse(
    val accessToken: String,
    val userId: String,
    val userEmail: String,
    val name: String,
    val isAdmin: Boolean = false,
    val isOnboarded: Boolean = true,
    val profileImagePath: String = "",
    val shouldChangePassword: Boolean = false
)
```

---

## 3. Sync API 不一致

### 问题

客户端调用 `DELETE /api/sync/ack`，但服务器端之前没有实现。

### 已修复

已添加 sync API stubs，但需要完整实现。

---

## 4. 其他需要检查的 API

| API | 状态 | 说明 |
|-----|------|------|
| `/server-info` | ✅ 已实现 | 需检查字段一致性 |
| `/server-info/features` | ❌ 未实现 | 客户端可能需要 |
| `/server-info/version` | ✅ 已实现 | 需检查字段一致性 |
| `/auth/login` | ✅ 已实现 | 需检查字段一致性 |
| `/auth/admin-sign-up` | ✅ 已实现 | 需检查字段一致性 |
| `/users/me` | ⚠️ 部分实现 | 字段格式问题 |
| `/assets` | ❌ 未实现 | 备份需要 |
| `/albums` | ❌ 未实现 | 相册同步需要 |

---

## 修复建议

### 立即修复

1. **UserAdminResponse.id** - 使用真实 UUID 格式
2. **UserAdminResponse.license** - 改为 `UserLicense?` 对象
3. **UserAdminResponse.profileChangedAt** - 改为 `String?` (ISO 8601)

### 后续完善

1. 从登录响应中获取真实用户信息
2. 实现 JWT token 解析获取用户 ID
3. 实现完整的用户管理 API

---

## 文档更新建议

### 需要创建的文档

1. `docs/server-android/api-implementation-status.md` - API 实现状态跟踪
2. `docs/server-android/openapi-compliance.md` - OpenAPI 合规性检查清单

### 需要更新的文档

1. 在 `README.md` 中添加 API 实现状态说明
2. 在开发文档中添加 OpenAPI spec 参考链接

---

## 结论

服务器端 Android 实现是独立开发的，没有严格按照 OpenAPI spec 进行。这导致了：

1. **字段类型不匹配** - 客户端解析失败
2. **字段格式不正确** - UUID、日期时间格式问题
3. **API 缺失** - 客户端调用失败

建议：

1. **立即修复** 当前发现的解析错误
2. **建立流程** 每次添加 API 时对照 OpenAPI spec
3. **自动化测试** 添加 API 合规性测试