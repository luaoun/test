-- 为 spc_point_metadata 表添加 tags 字段
-- 执行日期：2026-01-22

ALTER TABLE `spc_point_metadata`
ADD COLUMN `tags` varchar(500) DEFAULT NULL COMMENT '标签（多个标签用逗号分隔）'
AFTER `deleted_id`;
