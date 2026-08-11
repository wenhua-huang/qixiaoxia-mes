# 帮助文档维护说明（给文档作者）

本目录 `frontend/public/help/` 下存放用户帮助中心的所有 Markdown 文档。
这些文档会被 Vite 原样拷贝到 `dist/help/`，前端帮助中心通过 `fetch('/help/xxx.md')` 加载并渲染。

## 目录位置

```
frontend/public/help/
├── README.md          ← 本文件（维护说明，不显示给用户）
├── quick-start.md     ← 快速上手
└── pro-workflow.md    ← 生产主流程
```

## 如何新增一篇文档

只需 3 步，**无需改路由、菜单、构建配置**：

1. 在本目录新建 `.md` 文件，例如 `quality-guide.md`
2. 打开 `frontend/src/views/help/index.vue`
3. 在 `docTree` 数组对应分组加一条：

   ```ts
   {
     title: '质量管理',   // 分组标题
     items: [
       { file: 'quality-guide.md', title: '质检操作指南' }
     ]
   }
   ```

刷新帮助中心即可看到。

## 文档编写规范

- **文件名**：全小写、中划线分隔、`.md` 结尾（如 `stock-taking.md`）
- **标题层级**：第一行用 `# 标题`（一级），正文从 `##` 开始
- **语言**：面向最终用户（工厂操作员、管理员），**不要**出现代码、SQL、API 等技术词汇
- **步骤**：用有序列表（1. 2. 3.），每步一句话
- **截图**：放 `frontend/public/help/images/`，用 `![描述](images/xxx.png)` 引用

## 加截图

1. 把图片放到 `frontend/public/help/images/`
2. 文档中写：`![登录页](images/login.png)`
3. 图片会随 `dist/` 自动发布

## Markdown 支持的语法

常用都支持：标题、粗体、列表、表格、代码块、引用 `>`、链接、图片。
渲染器是 `marked`，样式是 `github-markdown-css`。

## 不应该放这里的

- ❌ 开发者文档（架构、设计决策）→ 放 `docs/设计文档/`
- ❌ API 文档、SQL、部署说明 → 放对应 docs 子目录
- ✅ 只放**给最终用户看**的操作指南
