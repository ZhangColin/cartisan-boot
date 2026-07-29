# 01 — ApiKeyInfo.permissions / @RequireSignature(permission) 是否必要？消费方倾向不要 per-key ACL

Type: task
Status: resolved
Raised by: aieducenter-app-registry（签名 facet 设计）

## 背景

cartisan-openapi 的签名 facet 契约 `ApiKeyInfo(appId, appName, apiSecret, permissions, status)` 含 `permissions: Set<String>`；`@RequireSignature(permission="X")` 据此做机机调用的权限 ACL（"调用方 app 有没有 X 权限"）。

## 问题

aieducenter-app-registry 在设计签名 facet（`ar_api_keys`）时审视 `permissions`：

- 想不出当前有哪个 provider 服务真的用 `@RequireSignature(permission=...)` 做细粒度 ACL。
- per-key/per-app 权限码管理（增删改、分配、校验）让 apiKey 管理变复杂、难维护。
- 这种粒度的机机 ACL，更像是腾讯/AWS 量级、多租户/不可信接入才需要的；对内部 first-party 平台，"已登记的应用可调"（签名 = 认证）已足够。

## 疑问（请框架侧判定）

1. `ApiKeyInfo.permissions` + `@RequireSignature(permission)` 是**真实被某个消费方使用**，还是**为臆想需求预留**（speculative）？
2. 若无消费方使用：是否考虑**简化框架**——从 `ApiKeyInfo` 去掉 `permissions`、去掉 `@RequireSignature` 的 permission 参数，缩小框架表面积（对齐框架 YAGNI / 窄 SPI 一贯取向）？
3. 若保留（作为真实能力）：请**确认并文档化**"signature-only 认证"是一等用法——即消费方传 `Set.of()`（空 permissions）+ 用无参 `@RequireSignature`（只验签、不做 ACL）是受支持、可长期依赖的路径。

## 影响范围

- 若框架**去掉** permissions：`ApiKeyInfo` 变 4 字段（appId/appName/apiSecret/status）；app-registry 的 `ApiKeyInfo` 构造简化、无需维护空 `Set.of()`；`@RequireSignature` 只留无参形式。
- 若框架**保留**：app-registry 永远传 `Set.of()`、不建 permissions 列、不做权限管理 UI；无参 `@RequireSignature` 作默认。
- 向后兼容：若去掉 permissions，需框架侧评估对现有消费方（若有用 permission-arg 的）的影响。

## 期望

框架侧 triage：判定 permissions 是真实需求还是 YAGNI；择"简化去掉"或"保留并文档化空集为默认"。app-registry 按结论落地（去掉 → 少传一字段；保留 → 传空集）。

## 来源 / 上下文

- 提出：aieducenter-app-registry 签名 facet 设计（讨论中，尚未落 ADR）。
- 相关代码：`cartisan-openapi` 的 `ApiKeyInfo`、`@RequireSignature`、`SignatureVerificationInterceptor`。
- app-registry 侧倾向：不要 per-key ACL，permissions 永远传空。

## Comments

**框架侧 triage（2026-07-28）**：✅ 判定为**臆想需求（speculative）**，采纳疑问 2 的"简化去掉"。

- 疑问 1（是否真实被使用）：**否**。`hcy_payment` 全部裸用 `@RequireSignature`（无 `permission=`）；`aieducenter-platform` 的 permissions 全是 admin RBAC，与签名无关；app-registry 不打算用。
- 疑问 2（简化去掉）：**采纳**。硬删 `permissions` 字段 + `hasPermission`、`@RequireSignature.permission()` 属性、拦截器 403 块、`RemoteApiKeyProvider` 的 permissions 解析；不留 hook（纯 YAGNI）。对齐 CONTEXT.md 标准 4 + Issue 02 破坏性补全先例。
- 疑问 3（保留并文档化空集默认）：**否决**——为臆想概念留一等地位，维护负担不减。

signature 回归纯**认证**（验签 = 已登记应用可调）；未来若真需机机 ACL，由应用层实现或届时新增窄 SPI。
决策详见 `CONTEXT.md` → Issue 04。已实现：`cartisan-openapi` 测试 42 全绿。
