import { describe, it, expect, vi, beforeEach } from 'vitest'

const { request } = vi.hoisted(() => ({
  request: vi.fn(() => Promise.resolve({ code: 200, rows: [], total: 0, data: {} })),
}))
vi.mock('@/utils/request', () => ({ default: request }))

import {
  listWmProductRecpt,
  getWmProductRecpt,
  addWmProductRecpt,
  updateWmProductRecpt,
  delWmProductRecpt,
} from '@/api/mes/wm/product_recpt'

describe('产品入库单 API - 端点契约', () => {
  beforeEach(() => { request.mockClear() })

  it('listWmProductRecpt -> GET /mes/wm/product_recpt/list', async () => {
    await listWmProductRecpt({ pageNum: 1, pageSize: 10 })
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_recpt/list',
      method: 'get',
    }))
  })

  it('getWmProductRecpt -> GET /mes/wm/product_recpt/{id}', async () => {
    await getWmProductRecpt(1)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_recpt/1',
      method: 'get',
    }))
  })

  it('addWmProductRecpt -> POST /mes/wm/product_recpt', async () => {
    await addWmProductRecpt({ recptCode: 'PR001' } as any)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_recpt',
      method: 'post',
    }))
  })

  it('updateWmProductRecpt -> PUT /mes/wm/product_recpt', async () => {
    await updateWmProductRecpt({ recptId: 1, recptCode: 'PR001' } as any)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_recpt',
      method: 'put',
    }))
  })

  it('delWmProductRecpt -> DELETE /mes/wm/product_recpt/{id}', async () => {
    await delWmProductRecpt(1)
    expect(request).toHaveBeenCalledWith(expect.objectContaining({
      url: '/mes/wm/product_recpt/1',
      method: 'delete',
    }))
  })
})
