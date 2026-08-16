import { describe, it, expect, vi, beforeEach } from 'vitest'

const { request } = vi.hoisted(() => ({
  request: vi.fn(() => Promise.resolve({ code: 200, rows: [], total: 0, data: {} })),
}))
vi.mock('@/utils/request', () => ({ default: request }))

import {
  listShipment,
  listShipmentBySales,
  getShipment,
  addShipment,
  updateShipment,
  delShipment,
  receiveShipment,
} from '@/api/mes/wm/product_sales_shipment'
import { listBoxBySales, addBox, updateBox, delBox } from '@/api/mes/wm/product_sales_box'

describe('销售发运单 API - 端点契约', () => {
  beforeEach(() => { request.mockClear() })

  it('listShipment -> GET /mes/wm/product_sales_shipment/list', async () => {
    await listShipment({ pageNum: 1, pageSize: 10 })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_sales_shipment/list',
      method: 'get',
    }))
  })

  it('listShipmentBySales -> GET /mes/wm/product_sales_shipment/bySales/{id}', async () => {
    await listShipmentBySales(215)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_sales_shipment/bySales/215',
      method: 'get',
    }))
  })

  it('getShipment -> GET /mes/wm/product_sales_shipment/{id}', async () => {
    await getShipment(500)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_sales_shipment/500',
      method: 'get',
    }))
  })

  it('addShipment -> POST /mes/wm/product_sales_shipment', async () => {
    await addShipment({ salesId: 215, shipMethod: 'LOGISTICS' } as any)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_sales_shipment',
      method: 'post',
    }))
  })

  it('updateShipment -> PUT /mes/wm/product_sales_shipment', async () => {
    await updateShipment({ shipmentId: 500, trackingNo: 'SF123' } as any)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_sales_shipment',
      method: 'put',
    }))
  })

  it('delShipment -> DELETE /mes/wm/product_sales_shipment/{id}', async () => {
    await delShipment(500)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_sales_shipment/500',
      method: 'delete',
    }))
  })

  it('receiveShipment -> PUT /mes/wm/product_sales_shipment/receive/{id}', async () => {
    await receiveShipment(500, { receivedBy: '张三' } as any)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_sales_shipment/receive/500',
      method: 'put',
    }))
  })
})

describe('销售装箱 API - 端点契约', () => {
  beforeEach(() => { request.mockClear() })

  it('listBoxBySales -> GET /mes/wm/product_sales_box/bySales/{id}', async () => {
    await listBoxBySales(215)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_sales_box/bySales/215',
      method: 'get',
    }))
  })

  it('addBox -> POST /mes/wm/product_sales_box', async () => {
    await addBox({ salesId: 215, quantity: 5 } as any)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_sales_box',
      method: 'post',
    }))
  })

  it('updateBox -> PUT /mes/wm/product_sales_box', async () => {
    await updateBox({ boxId: 201, quantity: 6 } as any)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_sales_box',
      method: 'put',
    }))
  })

  it('delBox -> DELETE /mes/wm/product_sales_box/{id}', async () => {
    await delBox(201)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_sales_box/201',
      method: 'delete',
    }))
  })
})
