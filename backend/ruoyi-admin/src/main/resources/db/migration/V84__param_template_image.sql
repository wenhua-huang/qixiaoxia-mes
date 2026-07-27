-- V84: 工序参数模板增加"标准图样"字段
-- 检验/报工的标准从数值扩展到可视化图样（外观样、测量点图、缺陷示例等）
-- 存储约定：逗号分隔的相对路径串，值为 /common/upload 返回的 fileName
ALTER TABLE qxx_pro_param_template
  ADD COLUMN image_url VARCHAR(1000) NULL COMMENT '标准图样，多个逗号分隔，存/common/upload返回的相对路径';
