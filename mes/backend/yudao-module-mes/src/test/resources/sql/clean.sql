-- 文件路径: backend/yudao-module-mes/src/test/resources/sql/clean.sql
-- 此文件用于 BaseDbUnitTest 默认的数据清理阶段
-- 由于我们使用 TestTraceRegistry 进行精准清理，此处保留一条无害语句以避免 "Script must not be empty" 报错

SELECT 1;
