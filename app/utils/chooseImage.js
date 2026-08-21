// 跨平台图片选择：H5 用原生 <input type="file">（解决 uni.chooseImage
// 在部分桌面浏览器/设备模拟模式下不弹文件选择器的问题），其他平台用 uni.chooseImage。
// 返回与 uni.chooseImage 一致的 { tempFilePaths, tempFiles }。
export function chooseImageAsync(options = {}) {
  const count = options.count || 1

  // #ifdef H5
  return new Promise((resolve, reject) => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = 'image/*'
    if (count > 1) input.multiple = true
    // 须挂到 DOM 且在用户手势同步调用栈内 click，浏览器才放行文件选择器
    input.style.position = 'fixed'
    input.style.left = '-9999px'
    input.style.top = '-9999px'
    input.onchange = () => {
      const files = Array.from(input.files || [])
      if (!files.length) { cleanup(); reject(new Error('未选择文件')); return }
      const picked = files.slice(0, count)
      const tempFilePaths = picked.map(f => URL.createObjectURL(f))
      resolve({ tempFilePaths, tempFiles: picked })
      cleanup()
    }
    input.oncancel = () => { cleanup(); reject(new Error('取消选择')) }
    function cleanup() {
      // 延迟移除 input：部分浏览器 change 后同步移除会中断取文件
      setTimeout(() => input.remove(), 0)
    }
    document.body.appendChild(input)
    input.click()
  })
  // #endif

  // #ifndef H5
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count,
      sizeType: options.sizeType || ['compressed'],
      sourceType: options.sourceType || ['camera', 'album'],
      success: resolve,
      fail: reject
    })
  })
  // #endif
}
