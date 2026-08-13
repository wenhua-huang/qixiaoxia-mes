# 销售订单开放接口对接指南

本文档面向需要将外部系统（如 CRM、电商平台、订单中台）对接到企小侠 MES 的开发人员。阅读本文后，你将能够：

- 了解开放接口的认证机制
- 调用接口在 MES 中自动创建销售订单
- 处理各类错误场景

---

## 1. 概述

企小侠 MES 提供基于 HTTP 的 RESTful 开放接口，外部系统通过 **API Key** 认证后推送销售订单。订单进入 MES 后自动标记来源为「CRM系统」，后续可直接转工单、排产、发货。

```
外部系统(CRM)  ──HTTP+JSON──▶  MES 开放接口  ──▶  销售订单(待确认)
                     ▲                                    │
                     │ X-API-Key 认证                     ▼
                     │                              转工单 → 生产 → 发货
              管理员在 MES 后台签发
```

### 当前开放接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 创建销售订单 | POST | `/open-api/sal/order/create` | 推送一笔带明细行的销售订单 |

> 后续将逐步开放库存查询、发货回传等接口，关注本文档更新。

---

## 2. 接入准备

### 2.1 获取 API Key

联系 MES 系统管理员，由管理员在 **系统管理 → API Key 管理** 中生成凭证。生成时需指定：

- **凭证名称**：标识用途，如「CRM 生产环境」
- **绑定工厂**：该 Key 推送的订单归属哪个工厂（多工厂隔离的关键）
- **过期时间**：可选，不填则永不过期

生成后管理员会获得一串明文 API Key（约 43 位字符），形如：

```
M8t_Uhb0jlNbxNBMNlkOweZQ6My82ejCF38-NSCdxgc
```

> ⚠️ **明文 Key 仅在生成时显示一次**，关闭后无法再次查看，请妥善保存。如遗失，需吊销旧 Key 并重新生成。

### 2.2 接口地址

| 环境 | Base URL |
|------|----------|
| 生产环境 | `http://115.29.234.204/prod-api` |

完整接口地址为 Base URL + 接口路径，例如：

```
http://115.29.234.204/prod-api/open-api/sal/order/create
```

### 2.3 安全须知

- API Key 等同于账号密码，**严禁** 写入前端代码、APP 包或公开仓库
- 建议通过 HTTPS 调用（生产环境配置 SSL 后使用 `https://`）
- 定期轮换 Key；怀疑泄露时立即在管理后台吊销
- 每个 Key 绑定唯一工厂，不同工厂应使用不同 Key

---

## 3. 认证方式

所有开放接口请求必须在 HTTP Header 中携带 API Key：

```
X-API-Key: {你的API Key}
```

**不需要** 携带 `Authorization: Bearer <token>`（那是内部用户登录用的 JWT）。

工厂归属由 API Key 绑定关系决定，请求体中 **不需要也不应该** 传递 `factoryId`。这样可以防止外部系统越权向其他工厂推单。

---

## 4. 创建销售订单

### 4.1 请求

```
POST /open-api/sal/order/create
Content-Type: application/json
X-API-Key: {你的API Key}
```

### 4.2 请求参数

#### 订单主表

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| orderName | String | ✅ | 订单名称，便于识别，如「XX客户8月采购订单」 |
| clientName | String | ✅ | 客户名称 |
| lines | Array | ✅ | 明细行列表，至少一行 |
| orderCode | String | ❌ | 订单号。不传则由 MES 按编码规则自动生成（推荐） |
| clientCode | String | ❌ | 客户编码 |
| clientOrderCode | String | ❌ | 客户 PO 号 |
| salesperson | String | ❌ | 业务员 |
| orderDate | String | ❌ | 订单日期，格式 `yyyy-MM-dd`，不传则取当天 |
| requestDate | String | ❌ | 需求交期，格式 `yyyy-MM-dd` |
| remark | String | ❌ | 备注 |

#### 明细行（lines[]）

| 字段 | 类型 | 必填 | 说明 |
|------|------|:----:|------|
| productCode | String | ✅ | 物料编码（MES 中已维护的物料编码） |
| quantity | Number | ✅ | 订单数量（正数，支持小数） |
| unitPrice | Number | ❌ | 单价（不传则为 0，后续可在 MES 中补充） |
| requestDate | String | ❌ | 行交期，格式 `yyyy-MM-dd` |
| remark | String | ❌ | 行备注 |

> 💡 **productCode 必须是 MES 中已存在的物料编码**。接口会按编码自动反查物料，填充产品名称、规格、单位等信息。如果物料编码不存在，接口返回错误，整单不创建。

### 4.3 请求示例

```json
{
  "orderName": "上海XX贸易8月采购订单",
  "clientName": "上海XX贸易有限公司",
  "clientCode": "SH-001",
  "clientOrderCode": "PO-2026-0812",
  "salesperson": "张三",
  "orderDate": "2026-08-12",
  "requestDate": "2026-08-25",
  "remark": "CRM 系统自动推送，请勿重复录入",
  "lines": [
    {
      "productCode": "PAPER-XBZ-A",
      "quantity": 500,
      "unitPrice": 2.50,
      "requestDate": "2026-08-25",
      "remark": "加急"
    },
    {
      "productCode": "PAPER-ZBZ-B",
      "quantity": 200,
      "unitPrice": 3.20
    }
  ]
}
```

### 4.4 响应

成功时 HTTP 状态码 200，响应体：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "orderId": 238,
    "orderCode": "00SO20260812001"
  }
}
```

| 字段 | 说明 |
|------|------|
| data.orderId | MES 内部订单 ID，建议保存用于后续关联查询 |
| data.orderCode | 订单编号（若请求未传 orderCode，此处返回系统生成的编号） |

### 4.5 订单创建后的默认值

通过本接口创建的订单，以下字段由系统自动设定，**无需外部传递**：

| 字段 | 默认值 | 说明 |
|------|--------|------|
| source | 2（CRM系统） | 标记订单来源，与内部手动新增（1=直接新增）区分 |
| status | PREPARE（待确认） | 订单创建后为待确认状态，需在 MES 内确认后才能转工单 |
| orderType | NEW（普通订单） | |
| sampleFlag | N（非样品） | |
| factoryId | API Key 绑定的工厂 | 由 Key 决定，不在请求体中传递 |
| createBy | open-api | 审计字段，标识由开放接口创建 |

---

## 5. 调用示例

### cURL

```bash
curl -X POST 'http://115.29.234.204/prod-api/open-api/sal/order/create' \
  -H 'Content-Type: application/json' \
  -H 'X-API-Key: M8t_Uhb0jlNbxNBMNlkOweZQ6My82ejCF38-NSCdxgc' \
  -d '{
    "orderName": "8月采购订单",
    "clientName": "上海XX贸易有限公司",
    "lines": [
      {
        "productCode": "PAPER-XBZ-A",
        "quantity": 500,
        "unitPrice": 2.50
      }
    ]
  }'
```

### Python

```python
import requests

API_KEY = "M8t_Uhb0jlNbxNBMNlkOweZQ6My82ejCF38-NSCdxgc"
BASE_URL = "http://115.29.234.204/prod-api"

payload = {
    "orderName": "8月采购订单",
    "clientName": "上海XX贸易有限公司",
    "clientOrderCode": "PO-2026-0812",
    "requestDate": "2026-08-25",
    "lines": [
        {
            "productCode": "PAPER-XBZ-A",
            "quantity": 500,
            "unitPrice": 2.50
        }
    ]
}

resp = requests.post(
    f"{BASE_URL}/open-api/sal/order/create",
    json=payload,
    headers={"X-API-Key": API_KEY},
    timeout=10
)
result = resp.json()

if result["code"] == 200:
    print(f"推单成功，订单号: {result['data']['orderCode']}")
else:
    print(f"推单失败: {result['msg']}")
```

### Java（OkHttp）

```java
OkHttpClient client = new OkHttpClient();

String json = """
    {
      "orderName": "8月采购订单",
      "clientName": "上海XX贸易有限公司",
      "lines": [
        { "productCode": "PAPER-XBZ-A", "quantity": 500, "unitPrice": 2.50 }
      ]
    }
    """;

Request request = new Request.Builder()
    .url("http://115.29.234.204/prod-api/open-api/sal/order/create")
    .addHeader("Content-Type", "application/json")
    .addHeader("X-API-Key", "M8t_Uhb0jlNbxNBMNlkOweZQ6My82ejCF38-NSCdxgc")
    .post(RequestBody.create(json, MediaType.parse("application/json")))
    .build();

try (Response response = client.newCall(request).execute()) {
    System.out.println(response.body().string());
}
```

---

## 6. 错误码

所有错误响应格式统一为：

```json
{
  "code": <错误码>,
  "msg": "<错误说明>"
}
```

### 6.1 认证类错误（HTTP 200，业务码 401）

| 场景 | code | msg 示例 | 处理方式 |
|------|:----:|----------|----------|
| 未携带 X-API-Key | 401 | 缺少 API Key，请在请求头携带 X-API-Key | 检查请求头是否添加 |
| Key 不存在 | 401 | 无效或已过期的 API Key | 核对 Key 是否正确 |
| Key 已停用 | 401 | 无效或已过期的 API Key | 联系管理员启用 |
| Key 已过期 | 401 | 无效或已过期的 API Key | 联系管理员续期或重签 |

### 6.2 参数校验错误（HTTP 200，业务码 500）

当必填字段缺失时，返回 500 及具体提示，例如：

| 场景 | msg 示例 |
|------|----------|
| orderName 为空 | 订单名称不能为空 |
| clientName 为空 | 客户名称不能为空 |
| lines 为空数组 | 明细行不能为空 |
| productCode 为空 | 物料编码不能为空 |
| quantity 为空 | 订单数量不能为空 |

### 6.3 业务错误（HTTP 200，业务码 500）

| 场景 | msg 示例 | 处理方式 |
|------|----------|----------|
| 物料编码在 MES 中不存在 | 物料编码不存在:XXX-001 | 先在 MES 基础数据中维护该物料，或核对编码 |
| orderCode 重复 | 销售订单号已存在 | 不传 orderCode 让系统自动生成，或换一个单号 |

> 💡 **接口是原子的**：任一行明细校验失败（如物料不存在），整单回滚，不会产生部分创建的订单。

---

## 7. 常见问题

### Q: orderCode 应该传还是不传？

**推荐不传**，让 MES 按系统编码规则自动生成订单号，在响应中获取。如果 CRM 有自己的单号体系，可通过 `clientOrderCode`（客户 PO 号）字段传递，不影响 MES 内部编码。

### Q: 推送成功后还能修改吗？

通过接口创建的订单状态为「待确认」，可以在 MES 系统内编辑、调整明细行后再确认。确认后可转工单进入生产环节。当前暂不开放通过 API 修改/取消订单的接口。

### Q: 同一笔订单重复推送了怎么办？

如果传了 `orderCode`，重复推送会因单号重复而报错，不会产生重复订单。如果不传 orderCode，每次推送会生成新订单号——因此 **CRM 侧应做好幂等控制**（如记录首次推送返回的 orderId），避免因网络重试重复推单。

### Q: 如何确认订单在 MES 中的后续状态？

当前可在 MES 销售管理界面查看订单状态（待确认 → 已确认 → 已转工单 → 生产中 → 已发货）。状态回传接口规划中，后续可通过 API 自动获取。

### Q: 一个 API Key 能推多个工厂的订单吗？

不能。每个 Key 在生成时绑定一个工厂，推送的订单自动归属该工厂。如需向多工厂推单，请为每个工厂分别申请 Key，并在调用时选择对应工厂的 Key。

### Q: 接口有调用频率限制吗？

当前无硬性限流，但建议单 Key 调用频率不超过 10 次/秒。批量推单时请在客户端做适当间隔，避免对系统造成压力。

---

## 8. 对接检查清单

对接完成后，请逐项确认：

- [ ] API Key 已获取并妥善保存在服务端配置中（未写入前端/APP）
- [ ] 请求头携带 `X-API-Key`，Content-Type 为 `application/json`
- [ ] 必填字段（orderName、clientName、lines）均已传值
- [ ] productCode 使用的是 MES 中已存在的物料编码
- [ ] 日期字段使用 `yyyy-MM-dd` 格式
- [ ] 成功时判断 `code === 200`，保存返回的 orderId/orderCode
- [ ] 失败时记录 msg 并重试或告警
- [ ] 生产环境使用 `http://115.29.234.204/prod-api` 作为 Base URL
- [ ] CRM 侧做了幂等控制，防止重复推单

---

*如对接中遇到本文档未覆盖的问题，请联系 MES 系统管理员。*
