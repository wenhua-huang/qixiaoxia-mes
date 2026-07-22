# E2E 快速验证方法

> 教训：一次简单 E2E 花了 1 小时+，以下方法确保几分钟内完成。

## 核心原则：最小化 E2E，分层验证

E2E 只验证"前端能渲染 + 用户能操作"，数据正确性用 API 测试验证。不要在 E2E 里做复杂的双弹窗嵌套操作。

## 快速验证 3 步法

### Step 1: 后端 API 验证（30 秒）

用 curl 验证接口 CRUD + 数据结构，不需要浏览器：

```bash
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)

# 验证查询接口返回新字段
curl -s "http://localhost:8081/mes/pur/order-line/list?pageNum=1&pageSize=1&orderId=1" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | grep -A5 lineAttrs

# 验证新增含 lineAttrs
curl -s -X POST "http://localhost:8081/mes/pur/order-line" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"orderId":1,"itemId":1,"itemCode":"TEST","itemName":"测试","unitOfMeasure":"PCS","unitName":"个","quantityOrdered":1,"lineAttrs":{"paper":{"width":"925","weight":"120"}}}' \
  | python3 -m json.tool

# 验证回查 lineAttrs 正确存储
curl -s "http://localhost:8081/mes/pur/order-line/list?pageNum=1&pageSize=1&orderId=1" \
  -H "Authorization: Bearer $TOKEN" | python3 -c "
import sys,json
d=json.load(sys.stdin)
for r in d.get('rows',[]):
    la=r.get('lineAttrs')
    if la and la.get('paper',{}).get('width')=='925':
        print('✅ lineAttrs JSON 存储验证通过')
        break
else:
    print('❌ 未找到 lineAttrs')
"
```

### Step 2: 前端渲染验证（2 分钟）

Playwright 只做 3 件事：打开页面 -> 打开弹窗 -> 截图验证 Tab 可见。不做表单填写和数据断言。

```typescript
test('属性 Tab 可见', async ({ page }) => {
  // 1. 打开采购订单页面
  await page.goto('/')
  await page.waitForTimeout(3000)

  // 2. 新增订单行弹窗
  // 3. 验证 el-tabs 有 4 个 Tab：基本信息/产品属性/纸张属性/纸袋属性
  const tabs = page.locator('.el-tabs__item')
  await expect(tabs).toHaveCount(4)
})
```

### Step 3: Flyway 迁移验证（10 秒）

```bash
docker exec qxx-mysql mysql -uroot -pqxx123456 mes -e "DESC qxx_pur_order_line;" | grep line_attrs
```

## 环境检查清单（动手前 30 秒做完）

| 检查项 | 命令 | 预期 |
|--------|------|------|
| 后端运行 | `curl -so /dev/null -w '%{http_code}' http://localhost:8081/` | 200 |
| 前端运行 | `curl -so /dev/null -w '%{http_code}' http://localhost:5173/` | 200 |
| 系统Chrome | `ls "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"` | 存在 |
| Playwright配置 | playwright.config.ts 有 `channel: 'chrome'` | 用系统Chrome |
| Flyway迁移 | `docker exec qxx-mysql mysql -uroot -pqxx123456 mes -e "SELECT version FROM flyway_schema_history ORDER BY version DESC LIMIT 3;"` | 含新版本号 |

## 反模式（禁止）

| 反模式 | 为什么慢 | 正确做法 |
|--------|---------|---------|
| E2E 里做双弹窗嵌套操作 | selector 脆弱，容易卡死 | API 验证数据 + E2E 只验证渲染 |
| 下载 Playwright 自带浏览器 | 169MB 下载，可能卡住 | 用 `channel: 'chrome'` 走系统 Chrome |
| 手动算 Flyway checksum | 算法容易搞错（CRC32 vs CRC32C） | 直接 `DELETE FROM flyway_schema_history WHERE version='N'` 让 Flyway 重跑（幂等迁移） |
| E2E 里做 API 断言 | 混合了两层职责 | API 测试归 API 测试，E2E 归 E2E |
| 先跑测试再修环境问题 | 每次失败浪费 1-2 分钟 | 先过环境检查清单，再跑测试 |
