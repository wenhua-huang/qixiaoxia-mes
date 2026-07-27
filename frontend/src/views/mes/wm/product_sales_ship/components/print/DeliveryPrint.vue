<template>
  <div class="delivery-print">
    <h2 class="title">{{ companyName }}送货单</h2>
    <div class="header-info">
      <span>出库单号：{{ header.salesCode }}</span>
      <span>客户：{{ header.clientName }}</span>
      <span>日期：{{ parseTime(header.salesDate, '{y}-{m}-{d}') }}</span>
    </div>
    <table class="doc-table">
      <thead>
        <tr>
          <th style="width:40px">序号</th>
          <th>物料编码</th>
          <th>物料名称</th>
          <th>规格</th>
          <th style="width:80px">数量</th>
          <th style="width:60px">单位</th>
          <th style="width:120px">备注</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(l, i) in (header.lines || [])" :key="i">
          <td>{{ i + 1 }}</td>
          <td>{{ l.itemCode }}</td>
          <td>{{ l.itemName }}</td>
          <td>{{ l.specification }}</td>
          <td class="num">{{ l.quantitySales }}</td>
          <td>{{ l.unitName }}</td>
          <td></td>
        </tr>
        <tr class="total-row">
          <td colspan="4">合计</td>
          <td class="num">{{ header.totalQuantity }}</td>
          <td colspan="2"></td>
        </tr>
      </tbody>
    </table>
    <div class="footer-sign">
      <div class="sign-block">发货人签字：____________</div>
      <div class="sign-block">收货人签字：____________</div>
      <div class="sign-block">日期：____________</div>
    </div>
    <div class="footer-note">本送货单一式两联，发货方与收货方各执一联。客户签收后请盖章确认。</div>
  </div>
</template>

<script setup lang="ts">
import { parseTime } from '@/utils/ruoyi'
import type { WmProductSales } from '@/types'

defineProps<{ header: WmProductSales }>()
const companyName = '奇晓晓科技'
</script>

<style scoped>
.delivery-print { padding: 20px; color: #000; font-size: 14px; }
.title { text-align: center; font-size: 22px; margin: 0 0 12px; }
.header-info { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 13px; }
.doc-table { width: 100%; border-collapse: collapse; }
.doc-table th, .doc-table td { border: 1px solid #000; padding: 6px 8px; text-align: center; font-size: 13px; }
.doc-table th { background: #f5f5f5; }
.num { text-align: right; }
.total-row td { font-weight: bold; }
.footer-sign { display: flex; justify-content: space-around; margin-top: 40px; }
.sign-block { min-width: 200px; }
.footer-note { margin-top: 24px; font-size: 12px; color: #666; text-align: center; }
</style>
