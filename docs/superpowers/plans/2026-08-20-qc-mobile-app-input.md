# 质检移动端 App 录入 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 uni-app 移动端新增 IPQC + IQC 两类质检的现场录入与判定能力，复用后端已有判定引擎。

**Architecture:** 后端新增一个扫码查询端点（收货单号→IQC），其余复用 IQC/IPQC 现有 list/getInfo/edit/add/judge 端点。App 端新增 `app/api/mes/qc/`、`app/utils/qc.js`、两个录入子组件和四个页面；判定采用与 PC 端 `QcJudgeDialog` 相同的"客户端预判 + 单次 judge 提交（让步理由随本次提交）"模式。

**Tech Stack:** Spring Boot 4.0.3 + JDK 17（后端）；uni-app + Vue 3 `<script setup>`（无 TS）+ uni-ui（App）；MinIO 图片上传。

## Global Constraints

- 后端所有 SQL/查询受 `FactoryIdInterceptor` 约束，新增接口不加 `@SkipFactoryId`。
- 前端字段命名与后端 camelCase 完全一致；后端有固化拼写：`standerVal`（非 standard）、`qcResultType`（非 valueType）、`crQuantity/majQuantity/minQuantity`、`defectQuantity`、`processMethod`、`defectImage`、`thresholdMin/thresholdMax`。
- 判定行字段（checkResult/inspector/inspectDate/各数量率）由后端 judge 写入，edit 时传入会被忽略；COMPLETED/CLOSED 单不可 edit/judge。
- edit 的 `lines`/`defectRecords` 非 null 时为**全删全插**，提交必须带全量。
- App 代码风格：`<script setup>` + Composition API；uni-ui 组件必须显式 import（H5 发行 easycom 失效）；`uni-section` 路径为 `@/components/uni-section/uni-section.vue`；反馈用 `proxy.$modal.*`，导航用 `proxy.$tab.navigateTo`。
- 函数长度约束：后端 ≤50 行，前端组件 ≤300 行。
- 改后端后必须重新打包并重启 `java -jar` 进程、用真实 token 实测接口（AGENTS.md 红线）。
- 图片走 `POST /common/uploadMinio`，返回的 `url` 在 AjaxResult 顶层（非 `data.url`）。

---

## File Structure

**后端（修改 3 + 新建 1）**
- Modify: `backend/ruoyi-system/.../service/mes/wm/IWmItemRecptService.java` — 暴露 selectByRecptCode
- Modify: `backend/ruoyi-system/.../service/mes/wm/impl/WmItemRecptServiceImpl.java` — 实现委托 mapper
- Create: `backend/ruoyi-admin/.../controller/mes/qc/QcScanController.java` — 收货单扫码查 IQC
- Modify: `backend/ruoyi-admin/.../controller/mes/qc/QcIpqcController.java` — add 返回新单 ID

**App（新建 11 + 修改 2）**
- Create: `app/api/mes/qc/index.js` — IQC/IPQC 统一 API
- Create: `app/api/mes/qc/defect.js` — 缺陷字典 API
- Create: `app/api/mes/qc/upload.js` — MinIO 图片上传
- Create: `app/utils/qc.js` — 枚举常量 + 预判逻辑（镜像 PC QcJudgeDialog）
- Create: `app/pages/mes/qc/components/qc-line-card.vue` — 单个检测项卡片（5 种值类型）
- Create: `app/pages/mes/qc/components/qc-defect-editor.vue` — 缺陷记录编辑器
- Create: `app/pages/mes/qc/list.vue` — 待检列表首页（IPQC/IQC Tab + 扫码 + 建单入口）
- Create: `app/pages/mes/qc/inspect.vue` — 检验录入页（核心）
- Create: `app/pages/mes/qc/ipqc-create.vue` — 扫码手工建 IPQC
- Create: `app/pages/mes/qc/history.vue` — 已完成历史（只读）
- Modify: `app/pages.json` — 注册 4 个页面
- Modify: `app/pages/work/index.vue` — 工作台加"质检"入口

---

## Task 1: 后端——收货单号扫码查 IQC 端点

**Files:**
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/wm/IWmItemRecptService.java`
- Modify: `backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/wm/impl/WmItemRecptServiceImpl.java`
- Create: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/qc/QcScanController.java`
- Test: `backend/ruoyi-admin/src/test/java/...`（如无 controller 层测试，靠 Task 12 接口实测）

**Interfaces:**
- Produces: `GET /mes/qc/scan/iqc?code={recptCode}` → `AjaxResult`，data 为 `{ recpt: WmItemRecpt, iqcList: List<QcIqc> }`；收货单不存在返回 `AjaxResult.error("收货单不存在")`。
- IWmItemRecptService 新增：`WmItemRecpt selectByRecptCode(String recptCode);`

- [ ] **Step 1: 在 IWmItemRecptService 接口加方法**

在 `IWmItemRecptService.java` 的 `selectWmItemRecptByRecptId` 声明下方加：

```java
    /**
     * 按收货单号精确查询收货单（供移动端扫码）
     */
    public WmItemRecpt selectByRecptCode(String recptCode);
```

- [ ] **Step 2: 在 WmItemRecptServiceImpl 实现**

在类中加（mapper 已有 `selectByRecptCode`，第 423 行已被内部调用，直接委托）：

```java
    @Override
    public WmItemRecpt selectByRecptCode(String recptCode)
    {
        return wmItemRecptMapper.selectByRecptCode(recptCode);
    }
```

- [ ] **Step 3: 新建 QcScanController**

创建 `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/qc/QcScanController.java`：

```java
package com.ruoyi.web.controller.mes.qc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.mes.qc.QcIqc;
import com.ruoyi.system.domain.mes.wm.WmItemRecpt;
import com.ruoyi.system.service.mes.qc.IQcIqcService;
import com.ruoyi.system.service.mes.wm.IWmItemRecptService;

/**
 * 质检移动端扫码查询Controller（factory_id 由 FactoryIdInterceptor 自动注入）
 *
 * @author qixiaoxia
 * @date 2026-08-20
 */
@RestController
@RequestMapping("/mes/qc/scan")
public class QcScanController extends BaseController
{
    @Autowired
    private IWmItemRecptService wmItemRecptService;

    @Autowired
    private IQcIqcService qcIqcService;

    /**
     * 扫收货单号：返回收货单及其 IQC 检验单列表
     */
    @PreAuthorize("@ss.hasPermi('mes:qc:iqc:query')")
    @GetMapping("/iqc")
    public AjaxResult scanIqc(@RequestParam("code") String recptCode)
    {
        WmItemRecpt recpt = wmItemRecptService.selectByRecptCode(recptCode);
        if (recpt == null)
        {
            return AjaxResult.error("收货单不存在：" + recptCode);
        }
        List<QcIqc> iqcList = qcIqcService.selectBySource("wm_item_recpt", recpt.getRecptId());
        Map<String, Object> data = new HashMap<>();
        data.put("recpt", recpt);
        data.put("iqcList", iqcList);
        return AjaxResult.success(data);
    }
}
```

确认 `IQcIqcService` 有 `selectBySource(String, Long)` 方法（QcIqcMapper 第 39 行已有，service 层若未暴露则在 `IQcIqcService`/`QcIqcServiceImpl` 补一个同名委托方法）。先 grep 确认：

```bash
grep -n "selectBySource" backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/qc/IQcIqcService.java
```

若无，加：
```java
public List<QcIqc> selectBySource(String sourceDocType, Long sourceDocId);
```
实现：
```java
@Override
public List<QcIqc> selectBySource(String sourceDocType, Long sourceDocId) {
    return qcIqcMapper.selectBySource(sourceDocType, sourceDocId);
}
```

- [ ] **Step 4: 重新打包并重启后端**

```bash
cd backend
mvn -pl ruoyi-admin -am package -DskipTests -q
# 找到旧进程并重启
OLD_PID=$(pgrep -f 'ruoyi-admin.jar' || true)
[ -n "$OLD_PID" ] && kill $OLD_PID && sleep 3
nohup java -jar ruoyi-admin/target/ruoyi-admin.jar > /tmp/ruoyi-backend.log 2>&1 &
# 等待启动
for i in $(seq 1 30); do
  curl -s http://localhost:8081/captchaImage >/dev/null && echo "UP" && break
  sleep 2
done
```

- [ ] **Step 5: 实测扫码端点**

```bash
TOKEN=$(python3 backend/scripts/get_token.py 2>/dev/null)
# 用一个真实收货单号（可从 DB 或 PC 列表取一个已生成 IQC 的单号）
curl -s "http://localhost:8081/mes/qc/scan/iqc?code=REPLACE_WITH_REAL_RECPT_CODE" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

预期：`code=200`，`data.recpt` 含收货单，`data.iqcList` 为数组；不存在的单号返回 `code=500, msg="收货单不存在：xxx"`。

- [ ] **Step 6: Commit**

```bash
git add backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/wm/IWmItemRecptService.java \
        backend/ruoyi-system/src/main/java/com/ruoyi/system/service/mes/wm/impl/WmItemRecptServiceImpl.java \
        backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/qc/QcScanController.java
git commit -m "feat(qc): 移动端扫收货单号查 IQC 端点"
```

---

## Task 2: App API 模块

**Files:**
- Create: `app/api/mes/qc/index.js`
- Create: `app/api/mes/qc/defect.js`
- Create: `app/api/mes/qc/upload.js`

**Interfaces:**
- Produces: `listIqc(query)`, `getIqc(id)`, `updateIqc(data)`, `judgeIqc(id, concessionReason)`
- Produces: `listIpqc(query)`, `getIpqc(id)`, `addIpqc(data)`, `updateIpqc(data)`, `judgeIpqc(id, concessionReason)`
- Produces: `listTemplate(query)`（IPQC 建单选模板）
- Produces: `scanIqc(code)`
- Produces: `listDefect(query)`
- Produces: `uploadQcImage(filePath)`

- [ ] **Step 1: 创建 `app/api/mes/qc/index.js`**

```javascript
import request from '@/utils/request'

// ============ IQC 来料检验 ============
export function listIqc(query) {
  return request({ url: '/mes/qc/iqc/list', method: 'get', params: query })
}
export function getIqc(iqcId) {
  return request({ url: '/mes/qc/iqc/' + iqcId, method: 'get' })
}
export function updateIqc(data) {
  return request({ url: '/mes/qc/iqc', method: 'put', data })
}
export function judgeIqc(iqcId, concessionReason) {
  return request({
    url: '/mes/qc/iqc/judge/' + iqcId,
    method: 'put',
    data: { concessionReason: concessionReason || null }
  })
}
// 扫收货单号：返回 { recpt, iqcList }
export function scanIqc(recptCode) {
  return request({ url: '/mes/qc/scan/iqc', method: 'get', params: { code: recptCode } })
}

// ============ IPQC 过程检验 ============
export function listIpqc(query) {
  return request({ url: '/mes/qc/ipqc/list', method: 'get', params: query })
}
export function getIpqc(ipqcId) {
  return request({ url: '/mes/qc/ipqc/' + ipqcId, method: 'get' })
}
export function addIpqc(data) {
  return request({ url: '/mes/qc/ipqc', method: 'post', data })
}
export function updateIpqc(data) {
  return request({ url: '/mes/qc/ipqc', method: 'put', data })
}
export function judgeIpqc(ipqcId, concessionReason) {
  return request({
    url: '/mes/qc/ipqc/judge/' + ipqcId,
    method: 'put',
    data: { concessionReason: concessionReason || null }
  })
}

// ============ 检验模板（IPQC 手工建单选模板） ============
export function listTemplate(query) {
  return request({ url: '/mes/qc/template/list', method: 'get', params: query })
}
```

- [ ] **Step 2: 创建 `app/api/mes/qc/defect.js`**

```javascript
import request from '@/utils/request'

// 缺陷字典列表（可按 indexType=IQC/IPQC 过滤）
export function listDefect(query) {
  return request({ url: '/mes/qc/defect/list', method: 'get', params: query })
}
```

- [ ] **Step 3: 创建 `app/api/mes/qc/upload.js`**

```javascript
import upload from '@/utils/upload'

// 质检图片上传 MinIO，返回 AjaxResult（url 在顶层）
export function uploadQcImage(filePath) {
  return upload({ url: '/common/uploadMinio', name: 'file', filePath })
}
```

- [ ] **Step 4: Commit**

```bash
git add app/api/mes/qc/
git commit -m "feat(qc-app): 质检移动端 API 模块"
```

---

## Task 3: App QC 工具常量与预判逻辑

**Files:**
- Create: `app/utils/qc.js`

**Interfaces:**
- Produces: `QC_STATUS_MAP`/`qcStatusText`/`qcStatusTagType`、`QC_RESULT_MAP`/`qcResultText`/`qcResultTagType`、`IPQC_TYPE_MAP`、`DEFECT_LEVEL_MAP`/`defectLevelText`/`defectLevelTagType`/`DEFECT_LEVEL_ORDER`、`VALUE_TYPES`、`judgeLine(line)`、`predictOrder({ lines, defects, quantityCheck, acQuantity, crRateLimit, majRateLimit, minRateLimit })` → `{ result:'PASS'|'FAIL', reasons:string[], unqualified, cr, maj, min }`
- 预判公式与 PC `QcJudgeDialog.judgeLine`/`doPredict` 及后端 `QcJudgeServiceImpl` 完全一致。

- [ ] **Step 1: 创建 `app/utils/qc.js`**

```javascript
// 质检通用常量与客户端预判（公式镜像后端 QcJudgeServiceImpl / PC QcJudgeDialog）

export const QC_STATUS_MAP = {
  PENDING: '待检验',
  INSPECTING: '检验中',
  COMPLETED: '已判定',
  CLOSED: '已作废'
}
export function qcStatusText(s) { return QC_STATUS_MAP[s] || s || '' }
export function qcStatusTagType(s) {
  const m = { PENDING: 'info', INSPECTING: 'warning', COMPLETED: 'success', CLOSED: 'default' }
  return m[s] || 'default'
}

export const QC_RESULT_MAP = {
  PASS: '合格',
  FAIL: '不合格',
  CONCESSION: '让步接收'
}
export function qcResultText(s) { return QC_RESULT_MAP[s] || s || '' }
export function qcResultTagType(s) {
  const m = { PASS: 'success', FAIL: 'error', CONCESSION: 'warning' }
  return m[s] || 'default'
}

export const IPQC_TYPE_MAP = {
  FIRST_CHECK: '首检',
  TOUR_CHECK: '巡检',
  LAST_CHECK: '完工检',
  SPOT_CHECK: '抽检'
}
export function ipqcTypeText(s) { return IPQC_TYPE_MAP[s] || s || '' }

export const DEFECT_LEVEL_MAP = {
  CRITICAL: '致命',
  MAJOR: '严重',
  MINOR: '轻微'
}
export function defectLevelText(s) { return DEFECT_LEVEL_MAP[s] || s || '' }
export function defectLevelTagType(s) {
  const m = { CRITICAL: 'error', MAJOR: 'warning', MINOR: 'default' }
  return m[s] || 'default'
}
export const DEFECT_LEVEL_ORDER = ['CRITICAL', 'MAJOR', 'MINOR']

export const VALUE_TYPES = { NUMBER: 'NUMBER', COUNT: 'COUNT', DICT: 'DICT', TEXT: 'TEXT', FILE: 'FILE' }

function toNum(v) {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isNaN(n) ? null : n
}

/**
 * 单行预判：NUMBER 按标准值±偏差区间；DICT 取 PASS/FAIL；TEXT/COUNT/FILE 填了即 PASS。
 * 返回 'PASS'|'FAIL'|null（null 表示未录入）。
 */
export function judgeLine(line) {
  if (!line) return null
  if (line.qcResultType === 'NUMBER') {
    if (!line.checkValText) return null
    const val = toNum(line.checkValText)
    if (val === null) return null
    const std = line.standerVal
    const lo = std != null && line.thresholdMin != null ? Number(std) + Number(line.thresholdMin) : line.thresholdMin
    const hi = std != null && line.thresholdMax != null ? Number(std) + Number(line.thresholdMax) : line.thresholdMax
    const fail = (lo != null && val < Number(lo)) || (hi != null && val > Number(hi))
    return fail ? 'FAIL' : 'PASS'
  }
  if (line.qcResultType === 'DICT') {
    if (!line.checkValText) return null
    return line.checkValText === 'PASS' ? 'PASS' : 'FAIL'
  }
  // TEXT/COUNT/FILE：显式行结果优先，否则填了实测值即 PASS
  if (line.lineResult) return line.lineResult
  if (line.checkValText) return 'PASS'
  return null
}

/**
 * 整单预判：镜像服务端引擎（Ac 值/致命缺陷/三档缺陷率）。
 * 入参：{ lines, defects, quantityCheck, acQuantity, crRateLimit, majRateLimit, minRateLimit }
 * 返回：{ result:'PASS'|'FAIL', reasons:string[], unqualified, cr, maj, min, unentered }
 */
export function predictOrder({ lines, defects, quantityCheck, acQuantity, crRateLimit, majRateLimit, minRateLimit }) {
  const unentered = (lines || []).find(l => judgeLine(l) == null)
  if (unentered) {
    return { result: null, unentered, reasons: [] }
  }
  let failLines = 0
  for (const line of lines || []) {
    if (judgeLine(line) === 'FAIL') failLines++
  }
  let cr = 0, maj = 0, min = 0
  for (const d of defects || []) {
    const q = d.defectQuantity ?? 1
    if (d.defectLevel === 'CRITICAL') cr += q
    else if (d.defectLevel === 'MAJOR') maj += q
    else if (d.defectLevel === 'MINOR') min += q
  }
  const unqualified = Math.max(cr + maj + min, failLines)
  const qty = quantityCheck || 0
  const pct = (q) => qty > 0 ? Math.round((q * 10000) / qty) / 100 : 0
  const reasons = []
  let result = 'PASS'
  if (acQuantity != null && unqualified > acQuantity) {
    result = 'FAIL'
    reasons.push(`不合格数 ${unqualified} 超过 Ac 值 ${acQuantity}`)
  }
  if (cr > 0) {
    result = 'FAIL'
    reasons.push(`存在致命缺陷 ${cr} 件`)
  }
  if (pct(cr) > (crRateLimit ?? 0) || pct(maj) > (majRateLimit ?? 0) || pct(min) > (minRateLimit ?? 0)) {
    result = 'FAIL'
    reasons.push(`缺陷率超阈值（致命 ${pct(cr)}%/${crRateLimit ?? 0}%，严重 ${pct(maj)}%/${majRateLimit ?? 0}%，轻微 ${pct(min)}%/${minRateLimit ?? 0}%）`)
  }
  if (!reasons.length) reasons.push(`不合格数 ${unqualified} ≤ Ac 值，缺陷率未超阈值`)
  return { result, reasons, unqualified, cr, maj, min, unentered: null }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/utils/qc.js
git commit -m "feat(qc-app): 质检常量与客户端预判工具"
```

---

## Task 4: 检测项卡片组件 qc-line-card

**Files:**
- Create: `app/pages/mes/qc/components/qc-line-card.vue`

**Interfaces:**
- Props: `line`（Object，QcOrderLine，含 qcResultType/standerVal/thresholdMin/thresholdMax/checkValText/crQuantity/majQuantity/minQuantity/lineResult 等，双向引用）、`readonly`（Boolean）、`required`（Boolean，是否必检）
- 组件直接 mutate `line.checkValText` / `line.crQuantity` / `line.majQuantity` / `line.minQuantity`；图片上传通过 emit `upload-image` 由父组件处理（因为上传有 loading/统一错误处理），或内部直接调 `uploadQcImage`。本组件内部直接调上传 API，保持自包含。
- 不单独判定行结果；NUMBER 失焦时用 `judgeLine` 显示"正常/超差"标签（仅提示）。

- [ ] **Step 1: 创建 `app/pages/mes/qc/components/qc-line-card.vue`**

```vue
<template>
  <view class="line-card" :class="{ 'is-error': required && !line.checkValText && showError }">
    <view class="line-header">
      <text class="line-name">{{ line.indexName }}</text>
      <text v-if="required" class="required-tag">必检</text>
      <text v-if="previewTag" class="preview-tag" :class="previewTag === 'PASS' ? 'ok' : 'bad'">
        {{ previewTag === 'PASS' ? '正常' : '超差' }}
      </text>
    </view>
    <view class="line-meta" v-if="line.qcTool || line.checkMethod">
      <text v-if="line.qcTool">工具：{{ line.qcTool }}</text>
      <text v-if="line.checkMethod" class="meta-method">方法：{{ line.checkMethod }}</text>
    </view>

    <!-- NUMBER / COUNT -->
    <view v-if="line.qcResultType === 'NUMBER' || line.qcResultType === 'COUNT'" class="field-row">
      <uni-easyinput
        v-model="line.checkValText"
        type="number"
        :inputBorder="true"
        :disabled="readonly"
        :placeholder="rangeHint"
        @blur="onPreview"
      />
      <text v-if="line.unitOfMeasure" class="unit">{{ line.unitOfMeasure }}</text>
    </view>

    <!-- DICT：PASS/FAIL 按钮组 -->
    <view v-else-if="line.qcResultType === 'DICT'" class="dict-row">
      <view
        v-for="opt in dictOptions"
        :key="opt.value"
        class="dict-btn"
        :class="{ active: line.checkValText === opt.value, pass: opt.value === 'PASS', fail: opt.value === 'FAIL' }"
        @click="!readonly && (line.checkValText = opt.value)"
      >{{ opt.label }}</view>
    </view>

    <!-- TEXT -->
    <view v-else-if="line.qcResultType === 'TEXT'">
      <uni-easyinput v-model="line.checkValText" type="textarea" :maxlength="500"
        :inputBorder="true" :disabled="readonly" placeholder="请输入检验描述" />
    </view>

    <!-- FILE：图片 -->
    <view v-else-if="line.qcResultType === 'FILE'" class="photo-row">
      <view v-for="(img, idx) in imageList" :key="idx" class="photo-item">
        <image :src="img" mode="aspectFill" class="photo-img" @click="previewImg(idx)" />
        <uni-icons v-if="!readonly" type="closeempty" size="18" class="photo-del" @click="removeImg(idx)" />
      </view>
      <view v-if="!readonly && imageList.length < 9" class="photo-add" @click="takePhoto">
        <uni-icons type="camera-filled" size="28" color="#999" />
        <text class="text-grey">{{ uploading ? '上传中…' : '拍照' }}</text>
      </view>
    </view>

    <!-- 三档缺陷数 -->
    <view v-if="!readonly" class="defect-row">
      <view class="defect-cell">
        <text class="defect-label cr">致命</text>
        <uni-number-box v-model="line.crQuantity" :min="0" :step="1" :disabled="readonly" />
      </view>
      <view class="defect-cell">
        <text class="defect-label maj">严重</text>
        <uni-number-box v-model="line.majQuantity" :min="0" :step="1" :disabled="readonly" />
      </view>
      <view class="defect-cell">
        <text class="defect-label min">轻微</text>
        <uni-number-box v-model="line.minQuantity" :min="0" :step="1" :disabled="readonly" />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniEasyinput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import uniNumberBox from '@/uni_modules/uni-number-box/components/uni-number-box/uni-number-box.vue'
import { uploadQcImage } from '@/api/mes/qc/upload'
import { judgeLine } from '@/utils/qc'

const props = defineProps({
  line: { type: Object, required: true },
  readonly: { type: Boolean, default: false },
  required: { type: Boolean, default: false },
  showError: { type: Boolean, default: false }
})

const dictOptions = [
  { label: '合格', value: 'PASS' },
  { label: '不合格', value: 'FAIL' }
]
const uploading = ref(false)

const imageList = computed(() => {
  const v = props.line.checkValText
  return v ? v.split(',').filter(Boolean) : []
})

const rangeHint = computed(() => {
  const std = props.line.standerVal
  if (std != null) {
    const lo = props.line.thresholdMin != null ? Number(std) + Number(props.line.thresholdMin) : null
    const hi = props.line.thresholdMax != null ? Number(std) + Number(props.line.thresholdMax) : null
    if (lo != null && hi != null) return `标准 ${std}（${lo}~${hi}）`
    if (lo != null) return `≥ ${lo}`
    if (hi != null) return `≤ ${hi}`
    return `标准 ${std}`
  }
  return '0'
})

const previewTag = computed(() => {
  if (props.line.qcResultType !== 'NUMBER') return null
  const r = judgeLine(props.line)
  return r
})

function onPreview() { /* trigger 重算 previewTag */ }

function takePhoto() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    sourceType: ['camera', 'album'],
    success: (res) => {
      const path = res.tempFilePaths[0]
      uploading.value = true
      uploadQcImage(path).then((r) => {
        const url = r.url
        const cur = props.line.checkValText ? props.line.checkValText + ',' : ''
        props.line.checkValText = cur + url
      }).catch(() => {
        uni.showToast({ title: '图片上传失败', icon: 'none' })
      }).finally(() => { uploading.value = false })
    }
  })
}
function removeImg(idx) {
  const arr = imageList.value.slice()
  arr.splice(idx, 1)
  props.line.checkValText = arr.join(',')
}
function previewImg(idx) {
  uni.previewImage({ current: idx, urls: imageList.value })
}
</script>

<style lang="scss" scoped>
.line-card {
  background: #fff;
  border-radius: 12rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  border-left: 6rpx solid #409eff;
  &.is-error { border-left-color: #f56c6c; }
}
.line-header { display: flex; align-items: center; gap: 12rpx; margin-bottom: 12rpx; }
.line-name { font-size: 30rpx; font-weight: 600; color: #303133; }
.required-tag { font-size: 22rpx; color: #f56c6c; }
.preview-tag { font-size: 22rpx; padding: 2rpx 12rpx; border-radius: 6rpx; margin-left: auto;
  &.ok { color: #67c23a; background: #f0f9eb; }
  &.bad { color: #f56c6c; background: #fef0f0; } }
.line-meta { font-size: 24rpx; color: #909399; margin-bottom: 12rpx; display: flex; gap: 20rpx; }
.field-row { display: flex; align-items: center; gap: 12rpx; }
.unit { font-size: 26rpx; color: #606266; min-width: 60rpx; }
.dict-row { display: flex; gap: 20rpx; }
.dict-btn { flex: 1; text-align: center; padding: 18rpx 0; border-radius: 8rpx; border: 2rpx solid #dcdfe6;
  font-size: 28rpx; color: #606266;
  &.active.pass { background: #f0f9eb; border-color: #67c23a; color: #67c23a; }
  &.active.fail { background: #fef0f0; border-color: #f56c6c; color: #f56c6c; } }
.photo-row { display: flex; flex-wrap: wrap; gap: 16rpx; }
.photo-item { position: relative; width: 160rpx; height: 160rpx; }
.photo-img { width: 100%; height: 100%; border-radius: 8rpx; }
.photo-del { position: absolute; top: -10rpx; right: -10rpx; background: #fff; border-radius: 50%; }
.photo-add { width: 160rpx; height: 160rpx; border: 2rpx dashed #dcdfe6; border-radius: 8rpx;
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6rpx; }
.text-grey { font-size: 22rpx; color: #999; }
.defect-row { display: flex; gap: 16rpx; margin-top: 20rpx; }
.defect-cell { flex: 1; display: flex; align-items: center; justify-content: space-between; }
.defect-label { font-size: 24rpx; color: #606266;
  &.cr { color: #f56c6c; } &.maj { color: #e6a23c; } &.min { color: #909399; } }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add app/pages/mes/qc/components/qc-line-card.vue
git commit -m "feat(qc-app): 检测项卡片组件（5 种值类型）"
```

---

## Task 5: 缺陷记录编辑器组件 qc-defect-editor

**Files:**
- Create: `app/pages/mes/qc/components/qc-defect-editor.vue`

**Interfaces:**
- Props: `modelValue`（Array<QcDefectRecord>，v-model）、`readonly`（Boolean）、`defectOptions`（Array<QcDefect>，从父组件加载后传入）
- 内部增删改；缺陷名从 defectOptions 选，等级/处置方式也默认带出缺陷字典里的值，可改（等级用 showActionSheet）。
- 图片字段为 `defectImage`（逗号分隔多 URL）。

- [ ] **Step 1: 创建 `app/pages/mes/qc/components/qc-defect-editor.vue`**

```vue
<template>
  <view class="defect-editor">
    <view class="de-header">
      <text class="de-title">缺陷记录</text>
      <text v-if="!readonly" class="de-add" @click="addRecord">+ 添加缺陷</text>
    </view>
    <view v-if="!list.length" class="de-empty">暂无缺陷记录</view>
    <view v-for="(rec, idx) in list" :key="idx" class="de-card">
      <view class="de-row">
        <text class="de-label">缺陷</text>
        <picker :range="defectNames" :value="defectIndex(rec)" @change="(e) => pickDefect(rec, e.detail.value)">
          <view class="de-pick">{{ rec.defectName || '请选择缺陷' }}</view>
        </picker>
      </view>
      <view class="de-row">
        <text class="de-label">等级</text>
        <view class="de-tag" :class="rec.defectLevel" @click="!readonly && pickLevel(rec)">
          {{ levelText(rec.defectLevel) || '请选择' }}
        </view>
        <text class="de-label" style="margin-left:24rpx">数量</text>
        <uni-number-box v-model="rec.defectQuantity" :min="1" :step="1" :disabled="readonly" />
      </view>
      <view class="de-row">
        <text class="de-label">处置</text>
        <view class="de-pick" @click="!readonly && pickProcess(rec)">{{ rec.processMethod || '请选择处置方式' }}</view>
      </view>
      <view class="photo-row">
        <view v-for="(img, i) in imagesOf(rec)" :key="i" class="photo-item">
          <image :src="img" mode="aspectFill" class="photo-img" @click="previewImg(rec, i)" />
          <uni-icons v-if="!readonly" type="closeempty" size="18" class="photo-del" @click="removeImg(rec, i)" />
        </view>
        <view v-if="!readonly && imagesOf(rec).length < 9" class="photo-add" @click="takePhoto(rec)">
          <uni-icons type="camera-filled" size="24" color="#999" />
        </view>
      </view>
      <view v-if="!readonly" class="de-del" @click="removeRecord(idx)">删除</view>
    </view>
  </view>
</template>

<script setup>
import { computed } from 'vue'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniNumberBox from '@/uni_modules/uni-number-box/components/uni-number-box/uni-number-box.vue'
import { uploadQcImage } from '@/api/mes/qc/upload'
import { DEFECT_LEVEL_ORDER, defectLevelText } from '@/utils/qc'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false },
  defectOptions: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])

const list = computed(() => props.modelValue || [])
const defectNames = computed(() => props.defectOptions.map(d => d.defectName))

function defectIndex(rec) {
  return props.defectOptions.findIndex(d => d.defectId === rec.defectId)
}
function levelText(l) { return defectLevelText(l) }

function pickDefect(rec, i) {
  const d = props.defectOptions[i]
  if (!d) return
  rec.defectId = d.defectId
  rec.defectCode = d.defectCode
  rec.defectName = d.defectName
  if (!rec.defectLevel) rec.defectLevel = d.defectLevel
  if (!rec.processMethod) rec.processMethod = d.processMethod
}
function pickLevel(rec) {
  uni.showActionSheet({
    itemList: DEFECT_LEVEL_ORDER.map(defectLevelText),
    success: (res) => { rec.defectLevel = DEFECT_LEVEL_ORDER[res.tapIndex] }
  })
}
function pickProcess(rec) {
  const items = ['返工', '返修', '报废', '让步接收', '退货']
  uni.showActionSheet({
    itemList: items,
    success: (res) => { rec.processMethod = items[res.tapIndex] }
  })
}
function addRecord() {
  const arr = list.value.slice()
  arr.push({ defectId: null, defectCode: '', defectName: '', defectLevel: 'MAJOR', defectQuantity: 1, processMethod: '', defectImage: '' })
  emit('update:modelValue', arr)
}
function removeRecord(idx) {
  const arr = list.value.slice()
  arr.splice(idx, 1)
  emit('update:modelValue', arr)
}
function imagesOf(rec) {
  return rec.defectImage ? rec.defectImage.split(',').filter(Boolean) : []
}
function takePhoto(rec) {
  uni.chooseImage({
    count: 1, sizeType: ['compressed'], sourceType: ['camera', 'album'],
    success: (res) => {
      uploadQcImage(res.tempFilePaths[0]).then((r) => {
        rec.defectImage = rec.defectImage ? rec.defectImage + ',' + r.url : r.url
      }).catch(() => uni.showToast({ title: '图片上传失败', icon: 'none' }))
    }
  })
}
function removeImg(rec, i) {
  const arr = imagesOf(rec)
  arr.splice(i, 1)
  rec.defectImage = arr.join(',')
}
function previewImg(rec, i) {
  uni.previewImage({ current: i, urls: imagesOf(rec) })
}
</script>

<style lang="scss" scoped>
.de-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.de-title { font-size: 30rpx; font-weight: 600; }
.de-add { font-size: 26rpx; color: #409eff; }
.de-empty { font-size: 26rpx; color: #999; padding: 20rpx 0; }
.de-card { background: #fff; border-radius: 12rpx; padding: 20rpx; margin-bottom: 16rpx; }
.de-row { display: flex; align-items: center; gap: 12rpx; margin-bottom: 16rpx; flex-wrap: wrap; }
.de-label { font-size: 26rpx; color: #606266; min-width: 60rpx; }
.de-pick { flex: 1; font-size: 28rpx; color: #303133; border: 2rpx solid #dcdfe6; border-radius: 8rpx; padding: 12rpx 16rpx; min-width: 200rpx; }
.de-tag { padding: 8rpx 20rpx; border-radius: 8rpx; font-size: 26rpx; border: 2rpx solid #dcdfe6;
  &.CRITICAL { color: #f56c6c; border-color: #f56c6c; }
  &.MAJOR { color: #e6a23c; border-color: #e6a23c; }
  &.MINOR { color: #909399; border-color: #909399; } }
.de-del { text-align: right; font-size: 26rpx; color: #f56c6c; margin-top: 8rpx; }
.photo-row { display: flex; flex-wrap: wrap; gap: 12rpx; }
.photo-item { position: relative; width: 130rpx; height: 130rpx; }
.photo-img { width: 100%; height: 100%; border-radius: 8rpx; }
.photo-del { position: absolute; top: -10rpx; right: -10rpx; background: #fff; border-radius: 50%; }
.photo-add { width: 130rpx; height: 130rpx; border: 2rpx dashed #dcdfe6; border-radius: 8rpx; display: flex; align-items: center; justify-content: center; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add app/pages/mes/qc/components/qc-defect-editor.vue
git commit -m "feat(qc-app): 缺陷记录编辑器组件"
```

---

## Task 6: 注册页面 + 工作台入口

**Files:**
- Modify: `app/pages.json`
- Modify: `app/pages/work/index.vue`

- [ ] **Step 1: 在 pages.json 注册 4 个页面**

在 `pages` 数组中最后一个 mes 条目（`pages/mes/wm/outsource-receive`）之后追加：

```json
,{
  "path": "pages/mes/qc/list",
  "style": { "navigationBarTitleText": "质检", "enablePullDownRefresh": true }
}, {
  "path": "pages/mes/qc/inspect",
  "style": { "navigationBarTitleText": "检验录入" }
}, {
  "path": "pages/mes/qc/ipqc-create",
  "style": { "navigationBarTitleText": "新建过程检验" }
}, {
  "path": "pages/mes/qc/history",
  "style": { "navigationBarTitleText": "检验历史", "enablePullDownRefresh": true }
}
```

- [ ] **Step 2: 工作台加"质检"入口**

在 `app/pages/work/index.vue` 中，找到生产管理分组（`<uni-section title="生产管理">`）的 `<uni-grid>` 内，加一个 grid-item（放在"生产报工"附近）：

```html
<uni-grid-item @click="goQc">
  <view class="grid-item-box">
    <uni-icons type="checkmarkempty" size="30" color="#409eff"></uni-icons>
    <text class="text">质检</text>
  </view>
</uni-grid-item>
```

在 `<script setup>` 中已有 `goReport` 等函数附近加：

```javascript
function goQc() {
  proxy.$tab.navigateTo('/pages/mes/qc/list')
}
```

- [ ] **Step 3: 验证页面能打开**

启动 App（H5 即可），工作台点"质检"应能跳到空白 list 页（Task 7 才实现）；点返回正常。

- [ ] **Step 4: Commit**

```bash
git add app/pages.json app/pages/work/index.vue
git commit -m "feat(qc-app): 注册质检页面并添加工位入口"
```

---

## Task 7: 待检列表首页 list.vue

**Files:**
- Create: `app/pages/mes/qc/list.vue`

**Interfaces:**
- Consumes: `listIqc`, `listIpqc`, `scanIqc` from `@/api/mes/qc`；`getCardScanResult` from `@/api/mes/pro/procard`
- 两个 Tab（IPQC / IQC），下拉刷新 + 上拉加载；状态过滤 PENDING,INSPECTING。
- 右上角扫码：扫码后识别——收货单号走 `scanIqc`（命中 IQC 跳 inspect）；流转卡/工单号走 `getCardScanResult`（已有 IPQC 跳 inspect，否则跳 ipqc-create 带 cardCode）；检验单号直接跳 inspect。
- IPQC Tab 底部"手工建单"按钮跳 ipqc-create。
- 右上角"历史"入口跳 history。
- 卡片展示：单号、来源、物料/工序（IPQC）或供应商/物料（IQC）、状态标签、生成时间。

- [ ] **Step 1: 创建 `app/pages/mes/qc/list.vue`**

```vue
<template>
  <view class="qc-page">
    <view class="top-bar">
      <view class="tabs">
        <view class="tab" :class="{ active: tab === 'IPQC' }" @click="switchTab('IPQC')">过程检 IPQC</view>
        <view class="tab" :class="{ active: tab === 'IQC' }" @click="switchTab('IQC')">来料检 IQC</view>
      </view>
      <view class="top-actions">
        <uni-icons type="scan" size="26" color="#409eff" @click="goScan" />
        <uni-icons type="clock" size="26" color="#909399" @click="goHistory" />
      </view>
    </view>

    <scroll-view scroll-y class="list-wrap" @scrolltolower="loadMore" refresher-enabled @refresherrefresh="onRefresh">
      <view v-if="!list.length && !loading" class="empty">暂无待检单</view>
      <view v-for="item in list" :key="item.iqcId || item.ipqcId" class="card" @click="openItem(item)">
        <view class="card-head">
          <text class="code">{{ item.iqcCode || item.ipqcCode }}</text>
          <uni-tag :type="statusTagType(item.status)" :text="statusText(item.status)" size="small" />
        </view>
        <view class="card-line" v-if="tab === 'IPQC'">
          <text>{{ item.itemName }}</text>
          <text class="muted"> · {{ item.processName }}</text>
        </view>
        <view class="card-line" v-else>
          <text>{{ item.itemName }}</text>
          <text class="muted"> · {{ item.vendorName }}</text>
        </view>
        <view class="card-line sub">
          <text class="muted">来源：{{ item.sourceDocCode || '—' }}</text>
          <text class="muted time">{{ fmtTime(item.createTime) }}</text>
        </view>
      </view>
      <view v-if="loading" class="loading-tip">加载中…</view>
      <view v-else-if="noMore && list.length" class="loading-tip">没有更多了</view>
    </scroll-view>

    <view v-if="tab === 'IPQC'" class="fab" @click="goCreate">
      <uni-icons type="plusempty" size="28" color="#fff" />
      <text>手工建单</text>
    </view>
  </view>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import { listIqc, listIpqc, scanIqc } from '@/api/mes/qc'
import { getCardScanResult } from '@/api/mes/pro/procard'
import { parseQrPayload } from '@/utils/qrPayload'
import { qcStatusText, qcStatusTagType, ipqcTypeText } from '@/utils/qc'

const { proxy } = getCurrentInstance()

const tab = ref('IPQC')
const list = ref([])
const loading = ref(false)
const noMore = ref(false)
const pageNum = ref(1)
const pageSize = 10

function statusText(s) { return qcStatusText(s) }
function statusTagType(s) { return qcStatusTagType(s) }

function switchTab(t) {
  if (tab.value === t) return
  tab.value = t
  reset()
  load()
}
function reset() { list.value = []; pageNum.value = 1; noMore.value = false }

function load() {
  loading.value = true
  // list 端点 status 仅支持精确匹配，分别查 PENDING/INSPECTING 后合并
  const api = tab.value === 'IPQC' ? listIpqc : listIqc
  const baseQ = { pageNum: pageNum.value, pageSize }
  Promise.all([
    api({ ...baseQ, status: 'PENDING' }),
    api({ ...baseQ, status: 'INSPECTING' })
  ]).then(([r1, r2]) => {
    const rows = [...(r1.rows || []), ...(r2.rows || [])]
    list.value = pageNum.value === 1 ? rows : list.value.concat(rows)
    noMore.value = rows.length < pageSize
  }).finally(() => {
    loading.value = false
    uni.stopPullDownRefresh()
  })
}
function loadMore() {
  if (noMore.value || loading.value) return
  pageNum.value++
  load()
}
function onRefresh() { reset(); load() }
onShow(() => { reset(); load() })
onPullDownRefresh(onRefresh)

function openItem(item) {
  const id = item.iqcId || item.ipqcId
  proxy.$tab.navigateTo(`/pages/mes/qc/inspect?type=${tab.value}&id=${id}`)
}
function goHistory() { proxy.$tab.navigateTo('/pages/mes/qc/history') }
function goCreate() { proxy.$tab.navigateTo('/pages/mes/qc/ipqc-create') }

function fmtTime(t) { return t ? String(t).replace('T', ' ').substring(5, 16) : '' }

// ===== 扫码路由 =====
function goScan() {
  // #ifdef H5
  uni.navigateTo({ url: '/pages/mes/pro/scan?callback=1', events: { scanResult: (code) => resolveCode(code) } })
  // #endif
  // #ifndef H5
  uni.scanCode({
    onlyFromCamera: false, scanType: ['barCode', 'qrCode'],
    success: (res) => resolveCode(res.result),
    fail: () => {}
  })
  // #endif
}

function resolveCode(raw) {
  const code = (raw || '').trim()
  if (!code) return
  // 解析 QXX|<TYPE>|<CODE> 结构，按类型分流避免无谓的错误提示
  const payload = parseQrPayload(code)
  const realCode = payload ? payload.code : code
  const qrType = payload ? payload.type : null

  uni.showLoading({ title: '查单中…' })
  if (qrType === 'CARD' || qrType === 'WO') {
    resolveCard(realCode)
  } else {
    // 未知类型或 MAT/PKG/ROLL：先当收货单试，失败再试流转卡
    scanIqc(realCode).then(res => {
      uni.hideLoading()
      const iqcList = res.data?.iqcList || []
      if (iqcList.length) {
        const target = iqcList.find(i => i.status !== 'CLOSED') || iqcList[0]
        proxy.$tab.navigateTo(`/pages/mes/qc/inspect?type=IQC&id=${target.iqcId}`)
      } else {
        proxy.$modal.msgWarning('该收货单未生成检验单或已免检')
      }
    }).catch(() => resolveCard(realCode))
  }
}

function resolveCard(realCode) {
  getCardScanResult(realCode).then(res => {
    const d = res.data
    if (!d || !d.card) {
      uni.hideLoading()
      proxy.$modal.msgError('未找到对应检验单或流转卡：' + realCode); return
    }
    const card = d.card
    // 查该卡是否已有进行中（PENDING/INSPECTING）IPQC
    return listIpqc({ cardCode: card.cardCode, pageNum: 1, pageSize: 20 })
      .then(r => {
        uni.hideLoading()
        const rows = (r.rows || []).filter(x => x.status === 'PENDING' || x.status === 'INSPECTING')
        if (rows.length) {
          proxy.$tab.navigateTo(`/pages/mes/qc/inspect?type=IPQC&id=${rows[0].ipqcId}`)
        } else {
          proxy.$tab.navigateTo(`/pages/mes/qc/ipqc-create?cardCode=${encodeURIComponent(card.cardCode)}`)
        }
      })
  }).catch(() => { uni.hideLoading(); proxy.$modal.msgError('查单失败') })
}
</script>

<style lang="scss" scoped>
page { background: #f5f6f7; min-height: 100%; }
.qc-page { display: flex; flex-direction: column; height: 100vh; }
.top-bar { display: flex; align-items: center; justify-content: space-between; padding: 16rpx 24rpx; background: #fff; }
.tabs { display: flex; gap: 24rpx; }
.tab { font-size: 28rpx; color: #606266; padding: 12rpx 0; position: relative;
  &.active { color: #409eff; font-weight: 600; &::after { content:''; position:absolute; bottom:0; left:20%; right:20%; height:4rpx; background:#409eff; border-radius:2rpx; } } }
.top-actions { display: flex; gap: 28rpx; }
.list-wrap { flex: 1; padding: 20rpx 24rpx; }
.empty { text-align: center; color: #999; padding: 120rpx 0; font-size: 28rpx; }
.card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 20rpx; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.code { font-size: 30rpx; font-weight: 600; color: #303133; }
.card-line { font-size: 28rpx; color: #303133; margin-bottom: 8rpx; display: flex; justify-content: space-between; }
.muted { color: #909399; font-size: 26rpx; }
.time { font-size: 24rpx; }
.loading-tip { text-align: center; color: #999; padding: 20rpx; font-size: 26rpx; }
.fab { position: fixed; right: 32rpx; bottom: 60rpx; background: #409eff; color: #fff;
  display: flex; align-items: center; gap: 8rpx; padding: 20rpx 28rpx; border-radius: 44rpx;
  font-size: 28rpx; box-shadow: 0 6rpx 20rpx rgba(64,158,255,.4); }
</style>
```

- [ ] **Step 2: 在浏览器/H5 验证**

启动 App（`npm run dev:h5` 或项目既定方式），进入质检页：两个 Tab 能切换；下拉刷新；卡片渲染（需后端有 PENDING 数据，没有可先 PC 端做一笔到货登记生成 IQC）；扫码按钮在 H5 跳手动输入页。

- [ ] **Step 3: Commit**

```bash
git add app/pages/mes/qc/list.vue
git commit -m "feat(qc-app): 待检列表首页（IPQC/IQC Tab + 扫码）"
```

---

## Task 8: 检验录入页 inspect.vue（核心）

**Files:**
- Create: `app/pages/mes/qc/inspect.vue`

**Interfaces:**
- Consumes: `getIqc/getIpqc`, `updateIqc/updateIpqc`, `judgeIqc/judgeIpqc`, `listDefect`
- query 参数：`type`（IQC/IPQC）、`id`
- 流程：
  1. onLoad 拉 getInfo，渲染头 + 检测项卡片 + 缺陷编辑器。
  2. 录入 `quantityCheck`（单据头）；逐行录 checkValText/三档缺陷数；加缺陷记录。
  3. "暂存"：拼整单 body（头 + lines + defectRecords）调 edit；PENDING 单首次 edit 自动变 INSPECTING。
  4. "提交判定"：先调 `predictOrder` 客户端预判；未录入项拦截并滚动；PASS 直接确认 → edit 后 judge；FAIL 弹出让步理由输入 → 选择"按不合格提交"（judge 无理由）或"让步接收"（judge 带理由，理由非空才可用）。
  5. 判定后显示结果横幅，全部只读。

- [ ] **Step 1: 创建 `app/pages/mes/qc/inspect.vue`**

```vue
<template>
  <view class="inspect-page">
    <!-- 单据头 -->
    <view class="header-card">
      <view class="h-row">
        <text class="h-code">{{ form.iqcCode || form.ipqcCode }}</text>
        <uni-tag :type="qcStatusTagType(form.status)" :text="qcStatusText(form.status)" size="small" />
      </view>
      <view class="h-row sub" v-if="type === 'IPQC'">
        <text>{{ form.itemName }}</text>
        <text class="muted">{{ form.processName }} · {{ ipqcTypeText(form.ipqcType) }}</text>
      </view>
      <view class="h-row sub" v-else>
        <text>{{ form.itemName }}</text>
        <text class="muted">{{ form.vendorName }}</text>
      </view>
      <view class="h-row qty-row">
        <text class="muted">本次检测数量</text>
        <uni-number-box v-if="!readonly" v-model="form.quantityCheck" :min="1" :step="1" />
        <text v-else>{{ form.quantityCheck }}</text>
      </view>
    </view>

    <!-- 判定结果横幅 -->
    <view v-if="form.checkResult" class="result-banner" :class="form.checkResult">
      <uni-icons :type="form.checkResult === 'PASS' ? 'checkmarkempty' : 'closeempty'" size="22" color="#fff" />
      <text>{{ qcResultText(form.checkResult) }}</text>
      <text v-if="form.checkResult === 'FAIL'" class="rb-sub">下游业务将被拦截</text>
      <text v-if="form.checkResult === 'CONCESSION'" class="rb-sub">让步接收</text>
    </view>

    <!-- 检测项 -->
    <view class="section-title">检测项（{{ lines.length }}）</view>
    <qc-line-card
      v-for="(line, idx) in lines" :key="line.lineId || idx"
      :line="line" :readonly="readonly" :required="true" :show-error="showLineError"
    />

    <!-- 缺陷记录 -->
    <qc-defect-editor v-model="defectRecords" :readonly="readonly" :defect-options="defectOptions" />

    <!-- 让步理由（COMPLETED 后回显） -->
    <view v-if="form.concessionReason" class="concession-box">
      <text class="muted">让步理由：</text>
      <text>{{ form.concessionReason }}</text>
    </view>

    <view style="height: 180rpx"></view>

    <!-- 底部操作栏 -->
    <view v-if="!readonly" class="footer-bar">
      <button class="btn-save" @click="save(false)" :disabled="submitting">暂存</button>
      <button class="btn-judge" type="primary" @click="onJudge" :disabled="submitting">
        {{ submitting ? '提交中…' : '提交判定' }}
      </button>
    </view>

    <!-- 判定确认弹窗（FAIL 时让步） -->
    <uni-popup ref="judgePopup" type="center" v-if="predictResult">
      <view class="judge-dialog">
        <view class="jd-title">预判结果</view>
        <uni-tag :type="predictResult === 'PASS' ? 'success' : 'error'"
          :text="predictResult === 'PASS' ? 'PASS 合格' : 'FAIL 不合格'" size="default" />
        <view class="jd-reason" v-for="(r, i) in predictReasons" :key="i">{{ r }}</view>
        <view v-if="predictResult === 'FAIL'" class="jd-concession">
          <text class="muted">让步理由（填了则升级让步接收）</text>
          <uni-easyinput v-model="concessionInput" type="textarea" :maxlength="200" placeholder="如选择让步接收则必填" />
        </view>
        <view class="jd-actions">
          <button v-if="predictResult === 'FAIL'" class="btn-fail" @click="doJudge(false)">按不合格提交</button>
          <button v-if="predictResult === 'FAIL'" class="btn-concession" :disabled="!concessionInput.trim()" @click="doJudge(true)">让步接收</button>
          <button v-else type="primary" @click="doJudge(false)">确认判定</button>
          <button class="btn-cancel" @click="judgePopup.close()">取消</button>
        </view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import uniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniNumberBox from '@/uni_modules/uni-number-box/components/uni-number-box/uni-number-box.vue'
import uniEasyinput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import uniPopup from '@/uni_modules/uni-popup/components/uni-popup/uni-popup.vue'
import qcLineCard from './components/qc-line-card.vue'
import qcDefectEditor from './components/qc-defect-editor.vue'
import { getIqc, updateIqc, judgeIqc, getIpqc, updateIpqc, judgeIpqc, listDefect } from '@/api/mes/qc'
import { qcStatusText, qcStatusTagType, qcResultText, ipqcTypeText, predictOrder } from '@/utils/qc'

const { proxy } = getCurrentInstance()
const type = ref('IPQC')
const form = ref({})
const lines = ref([])
const defectRecords = ref([])
const defectOptions = ref([])
const submitting = ref(false)
const showLineError = ref(false)
const judgePopup = ref(null)
const predictResult = ref(null)
const predictReasons = ref([])
const concessionInput = ref('')

const readonly = computed(() => form.value.status === 'COMPLETED' || form.value.status === 'CLOSED')

onLoad((opt) => {
  type.value = opt.type || 'IPQC'
  loadDetail(opt.id)
  loadDefects()
})

function loadDetail(id) {
  const api = type.value === 'IPQC' ? getIpqc : getIqc
  uni.showLoading({ title: '加载中…' })
  api(id).then(res => {
    const d = res.data || {}
    form.value = d
    lines.value = (d.lines || []).map(l => ({ ...l,
      crQuantity: l.crQuantity || 0, majQuantity: l.majQuantity || 0, minQuantity: l.minQuantity || 0 }))
    defectRecords.value = (d.defectRecords || []).map(r => ({ ...r }))
  }).finally(() => uni.hideLoading())
}
function loadDefects() {
  listDefect({ indexType: type.value, enableFlag: '1', pageNum: 1, pageSize: 500 })
    .then(res => { defectOptions.value = res.rows || [] })
}

function buildBody() {
  // 整单提交（lines/defectRecords 全量，edit 全删全插）
  const head = type.value === 'IPQC'
    ? { ipqcId: form.value.ipqcId, ipqcCode: form.value.ipqcCode, quantityCheck: form.value.quantityCheck,
        workorderId: form.value.workorderId, cardId: form.value.cardId, processId: form.value.processId,
        itemId: form.value.itemId, templateId: form.value.templateId, status: form.value.status }
    : { iqcId: form.value.iqcId, iqcCode: form.value.iqcCode, quantityCheck: form.value.quantityCheck,
        sourceDocId: form.value.sourceDocId, sourceDocType: form.value.sourceDocType,
        itemId: form.value.itemId, templateId: form.value.templateId, status: form.value.status }
  return { ...head, lines: lines.value, defectRecords: defectRecords.value }
}

function save(reloadAfter) {
  if (!form.value.quantityCheck || form.value.quantityCheck < 1) {
    proxy.$modal.msgWarning('请填写本次检测数量'); return Promise.reject()
  }
  submitting.value = true
  const api = type.value === 'IPQC' ? updateIpqc : updateIqc
  return api(buildBody()).then(() => {
    proxy.$modal.msgSuccess('已保存')
    if (reloadAfter) return loadDetail(form.value.iqcId || form.value.ipqcId)
  }).catch((e) => {
    proxy.$modal.msgError(e?.msg || '保存失败')
    throw e
  }).finally(() => { submitting.value = false })
}

function onJudge() {
  showLineError.value = false
  if (!form.value.quantityCheck || form.value.quantityCheck < 1) {
    proxy.$modal.msgWarning('请填写本次检测数量'); return
  }
  const pred = predictOrder({
    lines: lines.value,
    defects: defectRecords.value,
    quantityCheck: form.value.quantityCheck,
    acQuantity: form.value.quantityMaxUnqualified,
    crRateLimit: form.value.crRateLimit,
    majRateLimit: form.value.majRateLimit,
    minRateLimit: form.value.minRateLimit
  })
  if (pred.unentered) {
    showLineError.value = true
    proxy.$modal.msgError(`检测项[${pred.unentered.indexName}]未录入结果`)
    return
  }
  predictResult.value = pred.result
  predictReasons.value = pred.reasons
  concessionInput.value = ''
  judgePopup.value.open()
}

function doJudge(concession) {
  if (concession && !concessionInput.value.trim()) {
    proxy.$modal.msgWarning('请填写让步理由'); return
  }
  judgePopup.value.close()
  proxy.$modal.confirm('确认提交判定？判定后不可修改。').then(() => {
    submitting.value = true
    const api = type.value === 'IPQC' ? updateIpqc : updateIqc
    const judgeApi = type.value === 'IPQC' ? judgeIpqc : judgeIqc
    const id = form.value.ipqcId || form.value.iqcId
    // 先保存最新录入，再单次 judge（让步理由随本次提交）
    return api(buildBody()).then(() => judgeApi(id, concession ? concessionInput.value.trim() : null))
  }).then(() => {
    proxy.$modal.msgSuccess('判定完成')
    return loadDetail(form.value.iqcId || form.value.ipqcId)
  }).catch((e) => {
    if (e === 'cancel' || e === false || e?.cancel) return
    proxy.$modal.msgError(e?.msg || '判定失败')
  }).finally(() => { submitting.value = false })
}
</script>

<style lang="scss" scoped>
page { background: #f5f6f7; }
.inspect-page { padding: 20rpx 24rpx; }
.header-card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 20rpx; }
.h-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.h-code { font-size: 32rpx; font-weight: 600; }
.sub { font-size: 28rpx; color: #303133; }
.muted { color: #909399; font-size: 26rpx; }
.qty-row { border-top: 2rpx solid #f0f0f0; padding-top: 16rpx; margin-top: 8rpx; }
.result-banner { display: flex; align-items: center; gap: 12rpx; padding: 20rpx 24rpx; border-radius: 12rpx; color: #fff; margin-bottom: 20rpx; font-size: 30rpx; font-weight: 600;
  &.PASS { background: #67c23a; }
  &.FAIL { background: #f56c6c; }
  &.CONCESSION { background: #e6a23c; }
  .rb-sub { font-size: 24rpx; font-weight: normal; margin-left: auto; } }
.section-title { font-size: 28rpx; color: #606266; margin: 12rpx 0 16rpx; }
.concession-box { background: #fdf6ec; padding: 20rpx; border-radius: 8rpx; font-size: 26rpx; margin-top: 16rpx; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; background: #fff; padding: 16rpx 24rpx; display: flex; gap: 20rpx; box-shadow: 0 -2rpx 12rpx rgba(0,0,0,.06); }
.btn-save { flex: 1; background: #f4f4f5; color: #606266; font-size: 30rpx; border-radius: 44rpx; }
.btn-judge { flex: 2; font-size: 30rpx; border-radius: 44rpx; }
.judge-dialog { width: 620rpx; background: #fff; border-radius: 16rpx; padding: 32rpx; }
.jd-title { font-size: 32rpx; font-weight: 600; margin-bottom: 20rpx; }
.jd-reason { font-size: 26rpx; color: #606266; margin-top: 12rpx; }
.jd-concession { margin-top: 24rpx; }
.jd-actions { display: flex; flex-wrap: wrap; gap: 16rpx; margin-top: 28rpx; }
.jd-actions button { flex: 1; min-width: 240rpx; font-size: 28rpx; border-radius: 40rpx; }
.btn-fail { background: #fef0f0; color: #f56c6c; }
.btn-concession { background: #fdf6ec; color: #e6a23c; }
.btn-cancel { background: #f4f4f5; color: #909399; }
</style>
```

- [ ] **Step 2: 真机/浏览器全流程验证（IPQC）**

1. PC 端触发一笔 IPQC（报工确认自动生成），或用 Task 9 手工建单。
2. App 进入质检 → IPQC Tab → 点该单进入录入页。
3. 依次测试五种控件：NUMBER 输数字看到"正常/超差"标签、DICT 选合格/不合格、TEXT 输入、FILE 拍照上传 MinIO（确认图能预览）、COUNT 型。
4. 三档缺陷 stepper 增减；加一条缺陷记录（选缺陷、等级、数量、处置、拍照）。
5. 改"本次检测数量"。点"暂存"→ 退出重进确认数据回显、状态变"检验中"。
6. 点"提交判定"→ 弹窗显示预判 PASS/FAIL 及原因；FAIL 时填让步理由点"让步接收"。
7. 判定后页面只读，显示结果横幅；PC 端确认该单 COMPLETED、inspector/inspectDate 已回填。

- [ ] **Step 3: 真机/浏览器全流程验证（IQC + 让步/拦截）**

1. PC 做一笔到货登记生成 IQC。
2. App 扫收货单号（或从 IQC Tab 列表）进入。
3. 录入合格数据 → 判定 PASS → PC 端确认入库能放行。
4. 再造一笔：填超差 NUMBER 值 + 致命缺陷 → 预判 FAIL → 不填让步直接"按不合格提交"→ 验证 PC 端入库确认被 `QcGateService` 拦截。
5. 再造一笔 FAIL → 填让步理由 → 判定 CONCESSION → 验证入库可放行。

- [ ] **Step 4: Commit**

```bash
git add app/pages/mes/qc/inspect.vue
git commit -m "feat(qc-app): 检验录入页（录入+预判+判定+让步）"
```

---

## Task 9: IPQC 手工建单页 ipqc-create.vue

**Files:**
- Create: `app/pages/mes/qc/ipqc-create.vue`
- Modify: `backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/qc/QcIpqcController.java`（add 返回新单 ID，向后兼容 PC）

**Interfaces:**
- Consumes: `getCardScanResult`（流转卡扫码带工单/物料/工序）、`listTemplate`（IPQC 模板）、`addIpqc`
- Produces: `POST /mes/qc/ipqc` 响应的 `data` 改为新 `ipqcId`（PC 只判断 code，不受影响）
- query 可选 `cardCode`（从 list 页扫码带入）
- 字段：检验类型（FIRST_CHECK/TOUR_CHECK/SPOT_CHECK，showActionSheet）、流转卡（扫码或手输）、工单/物料/工序（扫码后自动带出只读）、检验模板（从启用的 IPQC 模板中选）。
- 提交：POST /mes/qc/ipqc 带 workorderId/itemId/templateId/ipqcType/cardId/processId 等，不传 lines（后端按模板自动生成检验行）。成功后跳 inspect 页。

- [ ] **Step 1: 修改 QcIpqcController.add 返回新单 ID**

将 `add` 方法的返回从 `toAjax(...)` 改为返回回填的主键（PC 端只判断 code，data 字段新增不影响）：

```java
    @PreAuthorize("@ss.hasPermi('mes:qc:ipqc:add')")
    @Log(title = "过程检验单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody QcIpqc qcipqc)
    {
        qcIpqcService.insertQcIpqc(qcipqc);
        return AjaxResult.success(qcipqc.getIpqcId());
    }
```

- [ ] **Step 2: 创建 `app/pages/mes/qc/ipqc-create.vue`**

```vue
<template>
  <view class="create-page">
    <view class="form-card">
      <view class="form-row">
        <text class="label">检验类型</text>
        <view class="pick" @click="pickType">{{ ipqcTypeText(form.ipqcType) || '请选择' }}</view>
      </view>
      <view class="form-row">
        <text class="label">流转卡</text>
        <uni-easyinput v-model="cardCodeInput" placeholder="扫码或输入流转卡号" :inputBorder="false" @confirm="onScanCard" />
        <uni-icons type="scan" size="24" color="#409eff" @click="scanCard" />
      </view>
      <view v-if="card" class="card-info">
        <view class="ci-row"><text class="muted">工单</text><text>{{ card.workorderCode }}</text></view>
        <view class="ci-row"><text class="muted">产品</text><text>{{ card.itemName }}</text></view>
        <view class="ci-row"><text class="muted">当前工序</text><text>{{ card.currentProcessName }}</text></view>
      </view>
      <view class="form-row" v-if="ipqcTemplates.length">
        <text class="label">检验模板</text>
        <picker :range="ipqcTemplateNames" :value="tplIndex" @change="(e) => form.templateId = ipqcTemplates[e.detail.value].templateId">
          <view class="pick">{{ tplName || '请选择模板' }}</view>
        </picker>
      </view>
    </view>
    <view style="height: 160rpx"></view>
    <view class="footer-bar">
      <button type="primary" class="btn-submit" :disabled="submitting" @click="submit">{{ submitting ? '提交中…' : '确认建单' }}</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import uniIcons from '@/uni_modules/uni-icons/components/uni-icons/uni-icons.vue'
import uniEasyinput from '@/uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.vue'
import { getCardScanResult } from '@/api/mes/pro/procard'
import { listTemplate, addIpqc } from '@/api/mes/qc'
import { ipqcTypeText, IPQC_TYPE_MAP } from '@/utils/qc'
import { parseQrPayload } from '@/utils/qrPayload'

const { proxy } = getCurrentInstance()
const cardCodeInput = ref('')
const card = ref(null)
const templates = ref([])
const form = ref({ ipqcType: 'TOUR_CHECK', templateId: null })
const submitting = ref(false)

const ipqcTypes = Object.keys(IPQC_TYPE_MAP)
const ipqcTypeNames = computed(() => ipqcTypes.map(ipqcTypeText))
function pickType() {
  uni.showActionSheet({ itemList: ipqcTypeNames.value, success: (res) => { form.value.ipqcType = ipqcTypes[res.tapIndex] } })
}

const ipqcTemplates = computed(() =>
  templates.value.filter(t => (t.qcTypes || '').split(',').map(s => s.trim()).includes('IPQC')))
const ipqcTemplateNames = computed(() => ipqcTemplates.value.map(t => t.templateName))
const tplIndex = computed(() => ipqcTemplates.value.findIndex(t => t.templateId === form.value.templateId))
const tplName = computed(() => { const t = ipqcTemplates.value[tplIndex.value]; return t ? t.templateName : '' })

onLoad((opt) => {
  listTemplate({ enableFlag: '1', pageNum: 1, pageSize: 500 }).then(r => { templates.value = r.rows || [] })
  if (opt.cardCode) { cardCodeInput.value = decodeURIComponent(opt.cardCode); onScanCard() }
})

function scanCard() {
  // #ifdef H5
  uni.navigateTo({ url: '/pages/mes/pro/scan?callback=1', events: { scanResult: (c) => resolveCard(c) } })
  // #endif
  // #ifndef H5
  uni.scanCode({ success: (res) => resolveCard(res.result), fail: () => {} })
  // #endif
}
function resolveCard(raw) {
  let code = (raw || '').trim()
  const p = parseQrPayload(code)
  if (p && p.code) code = p.code
  cardCodeInput.value = code
  onScanCard()
}
function onScanCard() {
  const code = cardCodeInput.value.trim()
  if (!code) return
  uni.showLoading({ title: '查卡中…' })
  getCardScanResult(code).then(res => {
    uni.hideLoading()
    const d = res.data
    if (!d || !d.card) { proxy.$modal.msgError('流转卡不存在或不可用：' + (d?.reason || code)); return }
    card.value = d.card
    // 自动选中第一个可报工任务的工序/工位
    const task = (d.reportableTasks || [])[0]
    form.value.workorderId = card.value.workorderId
    form.value.itemId = card.value.itemId
    form.value.cardId = card.value.cardId
    form.value.processId = task?.processId || card.value.currentProcessId
    form.value.workstationId = task?.workstationId || null
  }).catch(() => { uni.hideLoading(); proxy.$modal.msgError('查卡失败') })
}

function submit() {
  if (!card.value) { proxy.$modal.msgWarning('请先扫描流转卡'); return }
  if (!form.value.templateId) { proxy.$modal.msgWarning('请选择检验模板'); return }
  submitting.value = true
  const body = {
    ipqcType: form.value.ipqcType,
    workorderId: form.value.workorderId,
    workorderCode: card.value.workorderCode,
    workorderName: card.value.workorderName,
    cardId: form.value.cardId,
    cardCode: card.value.cardCode,
    processId: form.value.processId,
    processName: card.value.currentProcessName,
    workstationId: form.value.workstationId,
    itemId: form.value.itemId,
    itemCode: card.value.itemCode,
    itemName: card.value.itemName,
    specification: card.value.specification,
    unitOfMeasure: card.value.unitOfMeasure,
    templateId: form.value.templateId,
    quantityCheck: 1
  }
  addIpqc(body).then(res => {
    proxy.$modal.msgSuccess('建单成功')
    const newId = res.data
    setTimeout(() => proxy.$tab.redirectTo(`/pages/mes/qc/inspect?type=IPQC&id=${newId}`), 800)
  }).catch((e) => { proxy.$modal.msgError(e?.msg || '建单失败') })
    .finally(() => { submitting.value = false })
}
</script>

<style lang="scss" scoped>
page { background: #f5f6f7; }
.create-page { padding: 20rpx 24rpx; }
.form-card { background: #fff; border-radius: 12rpx; padding: 8rpx 24rpx; }
.form-row { display: flex; align-items: center; gap: 16rpx; padding: 24rpx 0; border-bottom: 2rpx solid #f5f5f5; }
.label { font-size: 28rpx; color: #606266; min-width: 140rpx; }
.pick { flex: 1; font-size: 28rpx; color: #303133; }
.card-info { background: #f8f9fa; border-radius: 8rpx; padding: 16rpx 20rpx; margin: 16rpx 0; }
.ci-row { display: flex; justify-content: space-between; font-size: 26rpx; padding: 6rpx 0; }
.muted { color: #909399; }
.footer-bar { position: fixed; left: 0; right: 0; bottom: 0; background: #fff; padding: 16rpx 24rpx; box-shadow: 0 -2rpx 12rpx rgba(0,0,0,.06); }
.btn-submit { border-radius: 44rpx; font-size: 30rpx; }
</style>
```

- [ ] **Step 3: 重新打包重启后端并验证建单**

Task 1 的重启流程同样适用：`mvn -pl ruoyi-admin -am package -DskipTests` → kill 旧进程 → `nohup java -jar` → 等 8081 起来。

1. App 质检页 → 点"手工建单"。
2. 扫一个进行中的流转卡（或手输卡号）→ 工单/产品/工序自动带出。
3. 选检验类型（首检/巡检/抽检）和检验模板（只显示 IPQC 模板）。
4. 点"确认建单"→ 成功后跳录入页，检测项已按模板自动生成。
5. PC 端 IPQC 列表确认新单存在、状态 PENDING、检验行完整。
6. 异常：未扫码提交、未选模板提交，均被前端拦截。

- [ ] **Step 4: Commit**

```bash
git add app/pages/mes/qc/ipqc-create.vue \
        backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/mes/qc/QcIpqcController.java
git commit -m "feat(qc-app): 扫码手工建 IPQC 页面（add 返回新单ID）"
```

---

## Task 10: 检验历史页 history.vue（只读）

**Files:**
- Create: `app/pages/mes/qc/history.vue`

**Interfaces:**
- Consumes: `listIqc`, `listIpqc`
- 两个 Tab，状态过滤 COMPLETED,CLOSED；点击进入 inspect 页（readonly 自动生效）。

- [ ] **Step 1: 创建 `app/pages/mes/qc/history.vue`**

复用 list.vue 的列表渲染逻辑，区别：状态 `COMPLETED,CLOSED`，卡片显示判定结果标签（PASS/FAIL/CONCESSION）与检验员，无扫码/建单 FAB，点击仍跳 inspect 页（判定后只读）。

```vue
<template>
  <view class="qc-page">
    <view class="top-bar">
      <view class="tabs">
        <view class="tab" :class="{ active: tab === 'IPQC' }" @click="switchTab('IPQC')">过程检</view>
        <view class="tab" :class="{ active: tab === 'IQC' }" @click="switchTab('IQC')">来料检</view>
      </view>
    </view>
    <scroll-view scroll-y class="list-wrap" @scrolltolower="loadMore" refresher-enabled @refresherrefresh="onRefresh">
      <view v-if="!list.length && !loading" class="empty">暂无历史记录</view>
      <view v-for="item in list" :key="item.iqcId || item.ipqcId" class="card" @click="openItem(item)">
        <view class="card-head">
          <text class="code">{{ item.iqcCode || item.ipqcCode }}</text>
          <uni-tag :type="qcResultTagType(item.checkResult)" :text="qcResultText(item.checkResult)" size="small" />
        </view>
        <view class="card-line"><text>{{ item.itemName }}</text><text class="muted">{{ item.inspector || '—' }}</text></view>
        <view class="card-line sub"><text class="muted">{{ item.sourceDocCode || '—' }}</text><text class="muted time">{{ fmtTime(item.inspectDate) }}</text></view>
      </view>
      <view v-if="loading" class="loading-tip">加载中…</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import uniTag from '@/uni_modules/uni-tag/components/uni-tag/uni-tag.vue'
import { listIqc, listIpqc } from '@/api/mes/qc'
import { qcResultText, qcResultTagType } from '@/utils/qc'

const { proxy } = getCurrentInstance()
const tab = ref('IPQC')
const list = ref([])
const loading = ref(false)
const noMore = ref(false)
const pageNum = ref(1)
const pageSize = 10

function switchTab(t) { if (tab.value === t) return; tab.value = t; reset(); load() }
function reset() { list.value = []; pageNum.value = 1; noMore.value = false }
function load() {
  loading.value = true
  // status 仅支持精确匹配，分别查 COMPLETED/CLOSED 合并
  const api = tab.value === 'IPQC' ? listIpqc : listIqc
  const baseQ = { pageNum: pageNum.value, pageSize }
  Promise.all([
    api({ ...baseQ, status: 'COMPLETED' }),
    api({ ...baseQ, status: 'CLOSED' })
  ]).then(([r1, r2]) => {
    const rows = [...(r1.rows || []), ...(r2.rows || [])]
    list.value = pageNum.value === 1 ? rows : list.value.concat(rows)
    noMore.value = rows.length < pageSize
  }).finally(() => { loading.value = false; uni.stopPullDownRefresh() })
}
function loadMore() { if (noMore.value || loading.value) return; pageNum.value++; load() }
function onRefresh() { reset(); load() }
onShow(() => { reset(); load() })
onPullDownRefresh(onRefresh)
function openItem(item) {
  proxy.$tab.navigateTo(`/pages/mes/qc/inspect?type=${tab.value}&id=${item.iqcId || item.ipqcId}`)
}
function fmtTime(t) { return t ? String(t).replace('T', ' ').substring(5, 16) : '' }
</script>

<style lang="scss" scoped>
page { background: #f5f6f7; min-height: 100%; }
.qc-page { display: flex; flex-direction: column; height: 100vh; }
.top-bar { background: #fff; padding: 16rpx 24rpx; }
.tabs { display: flex; gap: 24rpx; }
.tab { font-size: 28rpx; color: #606266; padding: 12rpx 0; position: relative;
  &.active { color: #409eff; font-weight: 600; &::after { content:''; position:absolute; bottom:0; left:20%; right:20%; height:4rpx; background:#409eff; border-radius:2rpx; } } }
.list-wrap { flex: 1; padding: 20rpx 24rpx; }
.empty { text-align: center; color: #999; padding: 120rpx 0; font-size: 28rpx; }
.card { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 20rpx; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12rpx; }
.code { font-size: 30rpx; font-weight: 600; }
.card-line { font-size: 28rpx; display: flex; justify-content: space-between; margin-bottom: 8rpx; }
.muted { color: #909399; font-size: 26rpx; }
.time { font-size: 24rpx; }
.loading-tip { text-align: center; color: #999; padding: 20rpx; font-size: 26rpx; }
</style>
```

- [ ] **Step 2: 验证历史页与只读详情**

已完成单出现在历史列表；点进详情所有控件置灰、无底部操作栏、显示判定结果与让步理由。

- [ ] **Step 3: Commit**

```bash
git add app/pages/mes/qc/history.vue
git commit -m "feat(qc-app): 检验历史与只读详情"
```

---

## Task 11: 并发与异常场景验证

**Files:** 无新增，验证 Task 1-10 的实现。

- [ ] **Step 1: 两人同检一单的并发保护**

用两个账号（或两台设备/两个浏览器）同时打开同一个 PENDING IQC：
1. A 暂存 → 状态变 INSPECTING（认领）。
2. B 暂存或提交 → 后端 edit 校验当前状态。**核实后端 edit 是否对"PENDING 单被另一人推进到 INSPECTING 后，B 仍能 edit"有防护**：阅读 `QcIqcServiceImpl.updateQcIqc`，它仅拦截 COMPLETED/CLOSED，不拦截 INSPECTING 的并发编辑。这意味着 B 的 edit 会覆盖 A 的录入（全删全插）。

若该风险需处理：在 `QcIqcServiceImpl.updateQcIqc`/`QcIpqcServiceImpl` 增加基于 `updateBy` 的乐观校验，或在 App 端提交前重新 GET 比对 status/inspector。**第一期接受的设计是"首次暂存认领 + 乐观锁"，但后端目前没有版本号字段。** 最小实现：App 在 buildBody 提交前不做特殊处理；后端 edit 时如果当前 status 已是 INSPECTING 且 updateBy 不是本人且本人未提交过，则拒绝（用 inspector 字段在判定前为空无法区分）。

鉴于第一期检验员通常按区域分工、同检概率低，**实现计划采用轻量方案**：App 进入录入页时记录当前 status；若提交 edit 时后端返回的单已不是自己进入时的版本（可在 edit 成功后比对返回），提示"单据已被他人更新，请刷新"。更严格的版本号乐观锁留待后续。此步只需人工验证：A、B 同时编辑，后提交者会覆盖先提交者的数据——**记录该已知限制**，在测试报告中注明，不在本期修。

- [ ] **Step 2: 网络异常**

Chrome DevTools 切 Offline，点暂存/判定 → 看到"网络异常/保存失败"提示，页面已填数据不丢；恢复网络后重试成功。

- [ ] **Step 3: 必检项与检测数量拦截**

清空某个 NUMBER 检测项直接提交判定 → 拦截并提示具体检测项名；检测数量留空提交 → 拦截。

- [ ] **Step 4: 图片上传中断**

提交判定时若仍有图片上传中（组件 uploading 状态），按钮应不可点；模拟上传失败 → 单张提示失败，可重拍。

- [ ] **Step 5: PC 回归**

PC 端 IQC/IPQC 的列表、录入、判定、让步流程全部正常（共用 edit/judge 接口）。

---

## Task 12: 提交前检查与收尾

- [ ] **Step 1: 代码质量检查**

```bash
# 后端编译
cd backend && mvn -pl ruoyi-admin -am compile -q
# 检查前端组件行数 ≤300
wc -l app/pages/mes/qc/*.vue app/pages/mes/qc/components/*.vue
```

`inspect.vue` 若超过 300 行，将判定弹窗抽成独立组件 `components/judge-result-dialog.vue`，父组件通过 ref 调用。检查所有 uni-ui 组件是否都显式 import（不能依赖 easycom）。

- [ ] **Step 2: 检查 uni-ui 显式 import**

grep 确认所有用到的 uni-ui 组件（uni-icons/uni-tag/uni-easyinput/uni-number-box/uni-popup/uni-section）都在 `<script setup>` 显式 import，不能依赖 easycom 自动注册（H5 发行会失效）。

- [ ] **Step 3: 全量 commit（如有未提交改动）**

```bash
git status
git add -A
git commit -m "feat(qc-app): 质检移动端录入（IPQC+IQC）" || echo "nothing to commit"
```

- [ ] **Step 4: 填写测试结论**

在 PR 描述中列出：
- IPQC 手工建单→录入→判定（PASS/FAIL/CONCESSION）实测结果截图；
- IQC 扫码→录入→判定→下游放行/拦截实测结果；
- 五种检测项控件 + 拍照上传 MinIO 验证；
- PC 回归结论；
- 已知限制：并发编辑的乐观锁为轻量方案（Task 11 Step 1）。

---

## Self-Review 结果

- **Spec 覆盖**：页面结构（Task 6-10）、五种控件录入（Task 4）、缺陷记录（Task 5）、数据流/接口（Task 1-2）、判定与让步（Task 8）、扫码路由（Task 7）、异常（Task 11）、历史只读（Task 10）均有对应任务。
- **后端增量**：仅一个扫码查询端点（Task 1），其余复用；spec 中标注的"待确认"已落实为新增 `QcScanController`。
- **字段名**：全部使用后端固化拼写 `standerVal`/`qcResultType`/`crQuantity`/`defectQuantity`/`processMethod`/`defectImage`。
- **判定模式**：采用 PC 一致的"客户端预判 + 单次 judge + 让步理由随本次提交"（Task 3 predictOrder + Task 8 doJudge），符合后端 COMPLETED 后不可重判的约束。
- **已知简化**：并发乐观锁采用轻量方案并在 Task 11 记录；离线不做（纯在线，符合决策）。
