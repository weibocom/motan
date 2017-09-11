SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for operation_record
-- ----------------------------
DROP TABLE IF EXISTS `operation_record`;
CREATE TABLE `operation_record` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `operator` varchar(255) DEFAULT NULL COMMENT '操作人',
  `type` VARCHAR(32) DEFAULT NULL COMMENT '操作类型',
  `group_name` varchar(255) DEFAULT NULL COMMENT 'group分组',
  `command` varchar(255) DEFAULT NULL COMMENT '指令内容或指令序号',
  `status` tinyint(2) DEFAULT NULL COMMENT '执行状态：1成功 0失败',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
