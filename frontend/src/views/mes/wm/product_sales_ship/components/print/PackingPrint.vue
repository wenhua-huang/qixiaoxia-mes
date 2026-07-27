<template>
  <div class="packing-print">
    <h2 class="title">装箱单</h2>
    <div class="header-info">
      <span>出库单号：{{ header.salesCode }}</span>
      <span>客户：{{ header.clientName }}</span>
      <span>箱数合计：{{ boxes.length }}</span>
      <span>体积合计：{{ totalVolume }} m³</span>
      <span>重量合计：{{ totalWeight }} kg</span>
    </div>
    <table class="doc-table">
      <thead>
        <tr>
          <th style="width:50px">序号</th>
          <th style="width:100px">箱号</th>
          <th>物料编码</th>
          <th>物料名称</th>
          <th style="width:70px">数量</th>
          <th style="width:50px">单位</th>
          <th style="width:130px">箱规</th>
          <th style="width:90px">体积(m³)</th>
          <th style="width:90px">重量(kg)</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(b, i) in boxes" :key="b.boxId || i">
          <td>{{ i + 1 }}</td>
          <td>{{ b.boxNo }}</td>
          <td>{{ b.itemCode }}</td>
          <td>{{ b.itemName }}</td>
          <td class="num">{{ b.quantity }}</td>
          <td>{{ b.unitName }}</td>
          <td>{{ b.boxSpec }}</td>
          <td class="num">{{ b.volume }}</td>
          <td class="num">{{ b.weight }}</td>
        </tr>
        <tr class="total-row">
          <td colspan="4">合计</td>
          <td class="num">{{ totalQty }}</td>
          <td colspan="2"></td>
          <td class="num">{{ totalVolume }}</td>
          <td class="num">{{ totalWeight }}</td>
        </tr>
      </tbody>
    </table>
    <div class="footer-sign">
      <div>装箱员：____________</div>
      <div>复核员：____________</div>
      <div>日期：____________</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { WmProductSales, WmProductSalesBox } from '@/types'

const props = defineProps<{ header: WmProductSales; boxes: WmProductSalesBox[] }>()

const totalQty = computed(() => props.boxes.reduce((s: number, b: WmProductSalesBox) => s + Number(b.quantity || 0), 0).toFixed(2))
const totalVolume = computed(() => props.boxes.reduce((s: number, b: WmProductSalesBox) => s + Number(b.volume || 0), 0).toFixed(3))
const totalWeight = computed(() => props.boxes.reduce((s: number, b: WmProductSalesBox) => s + Number(b.weight || 0), 0).toFixed(2))
</script>

<style scoped>
.packing-print { padding: 20px; color: #000; font-size: 14px; }
.title { text-align: center; font-size: 22px; margin: 0 0 12px; }
.header-info { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 13px; flex-wrap: wrap; gap: 8px; }
.doc-table { width: 100%; border-collapse: collapse; }
.doc-table th, .doc-table td { border: 1px solid #000; padding: 5px 6px; text-align: center; font-size: 12px; }
.doc-table th { background: #f5f5f5; }
.num { text-align: right; }
.total-row td { font-weight: bold; }
.footer-sign { display: flex; justify-content: space-around; margin-top: 40px; }
</style>
