// Calendar utility - simplified version
export default {
  getMonthStart(date: Date) { const d = new Date(date); d.setDate(1); return d; },
  getMonthEnd(date: Date) { const d = new Date(date); d.setMonth(d.getMonth()+1); d.setDate(0); return d; },
  getDays(date: Date) {
    const start = this.getMonthStart(date)
    const end = this.getMonthEnd(date)
    const days = []
    const sdf = (d: Date) => d.toISOString().split('T')[0]
    for (let d = new Date(start); d <= end; d.setDate(d.getDate()+1)) {
      days.push({ theDay: sdf(new Date(d)), shiftType: '', teamShifts: [] })
    }
    return days
  }
}
