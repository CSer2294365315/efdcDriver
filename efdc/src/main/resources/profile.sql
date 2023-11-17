create database efdc;

use efdc;

-- 在数据库中生成task（id=流域名+当前时间戳，任务名，状态/是否在运行，百分比，报错信息，任务文件路径，结果文件路径），等待后续异步执行
create table efdc_task_info(
	id varchar(256) primary key comment '主键:流域名+当前时间戳',
	task_name varchar(256) comment '任务名,如:预测_秦淮河流域_2023-11-01日_2023-11-12日_未来5天的数据',
	run_status int comment '运行状态，0表示未运行过，1表示正在运行，2表示已经运行结束，3表示运行失败',
	run_percent double comment '运行进度百分比',
	run_err_msg text comment '运行报错信息，如果未报错，则为空',
	task_exe_file_path varchar(512) comment '任务可执行文件夹绝对路径',
	task_rst_file_path varchar(512) comment '任务十进制执行结果文件夹绝对路径'
)