import type { GanttRow } from './GanttBar.vue'
import type { WorkstationLane } from '@/types/api/mes/pro/gantt'

type ParseDate = (s: string) => Date

const HOUR_MS = 3600000

/**
 * 机台泳道视图：把后端 lanes（每个工作站一行 + 行内任务）拍平成甘特行。
 * 每行可含多条任务条（bars），任务条仅点击派工/查看、不支持拖拽（noDrag）。
 */
export function buildLaneRows(lanes: WorkstationLane[], parseDate: ParseDate): GanttRow[] {
  const rows: GanttRow[] = []
  for (const lane of lanes) {
    const bars: GanttRow[] = []
    for (const t of lane.tasks || []) {
      if (!t.start) continue
      const s = parseDate(t.start)
      // 无结束时间（如待指派的历史脏数据）也渲染最小条，保证能点开派工
      let e = t.end ? parseDate(t.end) : new Date(s.getTime() + HOUR_MS)
      if (isNaN(e.getTime()) || e.getTime() <= s.getTime()) e = new Date(s.getTime() + HOUR_MS)
      bars.push({
        id: 'bar-' + t.id,
        text: t.processName || t.text || '工序',
        t: 't',
        s,
        e,
        c: t.colorCode || '#409eff',
        raw: t,
        noDrag: true,
        aS: t.actualStartTime ? parseDate(t.actualStartTime) : null,
        aE: t.actualEndTime ? parseDate(t.actualEndTime) : null,
        progress: typeof t.progressPercent === 'number' ? Math.min(100, Math.max(0, t.progressPercent)) : 0,
        delayLevel: t.delayLevel || 'NORMAL'
      })
    }
    rows.push({
      id: 'lane-' + lane.workstationId,
      text: lane.workstationName,
      t: 't',
      s: null,
      e: null,
      c: '',
      raw: { __lane: true, workstationId: lane.workstationId, workstationName: lane.workstationName },
      bars,
      laneType: lane.workstationCode
    })
  }
  return rows
}
