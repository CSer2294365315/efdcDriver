package com.hohai.schedule;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.hohai.utils.GlobalConstant.*;
import static com.hohai.utils.Utils.getConnection;
import static com.hohai.utils.Utils.replaceFileString;

public class EfdcScheduleTask {
    //TODO 总任务数量，一般是总天数
    public static Double totalTaskNums = 0.0;
    //TODO 任务实时执行进度
    public static Double taskPercent = 0.0;

    public static void main(String[] args) {
        EfdcScheduleTask.scheduleTask();
    }

    public static void scheduleTask() {
        while (true) {
            try {
                Connection connection = null;
                Statement statement = null;
                try {
                    // 连接数据库,查询出需要执行的任务
                    connection = getConnection(DB_URL, USER_NAME, PASSWORD);
                    statement = connection.createStatement();
                    //获取到数据库中，待执行的任务，限定一个，按照时间升序排序
                    ResultSet resultSet = statement.executeQuery(
                            "select\n" +
                                    "    id,\n" +
                                    "    task_name,\n" +
                                    "    run_status,\n" +
                                    "    run_percent,\n" +
                                    "    run_err_msg,\n" +
                                    "    task_exe_file_path,\n" +
                                    "    task_rst_file_path\n" +
                                    "from efdc_task_info\n" +
                                    "where run_status = 0\n" +
                                    "order by id asc \n" +
                                    "limit 1");
                    while (resultSet.next()) {
                        //获取到待执行的任务的任务包的绝对路径
                        String taskExeFilePath = resultSet.getString("task_exe_file_path");
                        String taskId = resultSet.getString("id");
                        //执行任务包中的exe文件
                        invokeEfdcTask(taskExeFilePath, taskId, connection);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        statement.close();
                        connection.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void invokeEfdcTask(String filePath, String TaskId, Connection connection) throws Exception {
        // 执行exe文件并获取日志
        try {
            String command = filePath + "/EFDCPlus_085_OMP_190819_DPx64.exe";
            //执行EFDC的exe文件
            Process process = Runtime.getRuntime().exec(command);

            Statement statement = connection.createStatement();

            //TODO 开启一个线程，后台定时执行，读取任务实时进度，定时的更新到数据库中
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            String insertTaskInfoSQL = "update efdc.efdc_task_info set run_status = 1 and run_percent = " + taskPercent + " where id = " + TaskId;
                            statement.execute(insertTaskInfoSQL);
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

            //TODO 执行BAT批处理文件，将十进制改为二进制
            String binaryToDecimalExeFile = filePath + "/GetEFDC8.5PAN.exe";
            String getefdcInpFilePath = filePath + "/getefdc.inp";

            //将getefdc.inp文件中的efdc.inp文件路径进行替换
            String efdcInpFilePath = filePath + "/efdc.inp";
            replaceFileString(getefdcInpFilePath, "@@@EfdcInpFilePath@@@", efdcInpFilePath);

            try {
                Runtime.getRuntime().exec(binaryToDecimalExeFile + " " + getefdcInpFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

//            //实时获取到exe文件的执行日志
//            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//            StringBuilder log = new StringBuilder();
//            while ((line = reader.readLine()) != null) {
//                //此步完成进度的实时更新功能
//                taskPercent = ExtractTaskExecutionProgress(line);
//                log.append(line).append("\n");
//
//            }
//            reader.close();

            //TODO 此步执行完毕，将任务状态更新为2
            String insertTaskInfoSQL = "update efdc.efdc_task_info set run_status = 2 and run_percent = " + 1.0 + " where id = " + TaskId;
            statement.execute(insertTaskInfoSQL);

            statement.close();

        } catch (Exception e) {
            System.out.println("执行exe文件失败");
            e.printStackTrace();
        }
    }

    /**
     * 提取任务执行进度，得到百分比
     */
    public static Double ExtractTaskExecutionProgress(String logLine) {
        return 0.0;
    }


}
