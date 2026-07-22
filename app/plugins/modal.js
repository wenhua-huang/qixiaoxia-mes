export default {
  // 消息提示
  msg(content) {
    uni.showToast({
      title: content,
      icon: 'none'
    })
  },
  // 错误消息
  // 注意：uni.showToast 在 icon 为 success/error 时，标题最多显示 7 个汉字，
  // 超出会被截断（例如"该订单状态为"E→只显示半字）。这里对长文本回退到 icon:'none'
  // 并延长显示时间，避免业务提示看不全。
  msgError(content) {
    const text = content == null ? '' : String(content)
    if (text.length > 7) {
      uni.showToast({ title: text, icon: 'none', duration: 3000 })
    } else {
      uni.showToast({ title: text, icon: 'error' })
    }
  },
  // 成功消息（同样规避 7 字截断）
  msgSuccess(content) {
    const text = content == null ? '' : String(content)
    if (text.length > 7) {
      uni.showToast({ title: text, icon: 'none', duration: 2500 })
    } else {
      uni.showToast({ title: text, icon: 'success' })
    }
  },
  // 隐藏消息
  hideMsg(content) {
    uni.hideToast()
  },
  // 弹出提示
  alert(content, title) {
    uni.showModal({
      title: title || '系统提示',
      content: content,
      showCancel: false
    })
  },
  // 确认窗体
  confirm(content, title) {
    return new Promise((resolve, reject) => {
      uni.showModal({
        title: title || '系统提示',
        content: content,
        cancelText: '取消',
        confirmText: '确定',
        success: function(res) {
          if (res.confirm) {
            resolve(res.confirm)
          }
        }
      })
    })
  },
  // 提示信息
  showToast(option) {
    if (typeof option === "object") {
      uni.showToast(option)
    } else {
      uni.showToast({
        title: option,
        icon: "none",
        duration: 2500
      })
    }
  },
  // 打开遮罩层
  loading(content) {
    uni.showLoading({
      title: content,
      icon: 'none'
    })
  },
  // 关闭遮罩层
  closeLoading() {
    try {
        uni.hideLoading()
    } catch (e) {
        console.log(e)
    }
  }
}
