package com.hohai.controller;

import com.alibaba.fastjson.JSON;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import static com.hohai.utils.Utils.*;

@RestController
@RequestMapping("/api")
public class EfdcController {

    /**
     * TODO 模板文件地址
     */
    String EFDC_TEMPLATE_FILE_PATH = "/Users/cui/Desktop/efdc/modle-1";

    String BASIC_PATH = "/Users/cui/Desktop/efdc/";

    String DB_URL = "jdbc:mysql://localhost:3306/efdc";
    String USER_NAME = "root";
    String PASSWORD = "q771411";


    @PostMapping("/upload")
    public String uploadTask(@RequestParam(value = "files") MultipartFile[] files,
                           @RequestParam("drainage") String drainage,
                           @RequestParam("taskName") String taskName) {

        //TODO 生成文件夹id，id = 流域名称_时间戳
        String currentTimeStampStr = getCurrentTimeStampStr();
        String id = drainage + "_" + currentTimeStampStr;
        String NewEfdcExeFilePath = BASIC_PATH + drainage + "_" + currentTimeStampStr;

        //TODO 复制模板文件到一个新的文件夹
        copyFolderInvoker(EFDC_TEMPLATE_FILE_PATH, NewEfdcExeFilePath);

        //TODO 上传文件(先复制模板，再覆盖文件，生成可执行的文件包)
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getOriginalFilename();
            File dest = new File(NewEfdcExeFilePath + "/" + fileName);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            try {
                files[i].transferTo(dest);
            } catch (Exception e) {
                e.printStackTrace();
                return getUploadStatusJsonStr(400,"任务上传失败,请重新上传,如果再次失败,请联系管理员",null);
            }
        }


        /*
        create table efdc_task_info(
            id varchar(256) primary key comment '主键:流域名+当前时间戳',
            task_name varchar(256) comment '任务名,如:预测_秦淮河流域_2023-11-01日_2023-11-12日_未来5天的数据',
            run_status int comment '运行状态，0表示未运行过，1表示正在运行，2表示已经运行结束，3表示运行失败',
            run_percent double comment '运行进度百分比',
            run_err_msg text comment '运行报错信息，如果未报错，则为空',
            task_exe_file_path varchar(512) comment '任务可执行文件夹绝对路径',
            task_rst_file_path varchar(512) comment '任务十进制执行结果文件夹绝对路径'
        )
         */
        //TODO 在数据库中生成task（id=流域名+当前时间戳，任务名，状态/是否在运行，百分比，报错信息，任务文件路径，结果文件路径），等待后续异步执行
        try {
            Connection connection = getConnection(DB_URL, USER_NAME, PASSWORD);
            String insertTaskInfoSQL = "insert into efdc.efdc_task_info(id,task_name,run_status,run_percent,run_err_msg,task_exe_file_path,task_rst_file_path) " +
                    "values (?,?,?,?,?,?,?)";
            PreparedStatement preparedStatement = connection.prepareStatement(insertTaskInfoSQL);
            preparedStatement.setString(1, id);
            preparedStatement.setString(2, taskName);
            preparedStatement.setInt(3, 0);
            preparedStatement.setDouble(4, 0.0);
            preparedStatement.setString(5, "");
            preparedStatement.setString(6, NewEfdcExeFilePath);
            //TODO 待定
            preparedStatement.setString(7, NewEfdcExeFilePath);

            preparedStatement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO 返回id，根据这个id拿结果数据
        return getUploadStatusJsonStr(200,"任务上传成功",id);
    }

    public static void main(String[] args) {
//        copyFolderInvoker("/Users/cui/Desktop/efdc/modle-1","/Users/cui/Desktop/efdc/modle-2");
//        LocalDateTime currentTime = LocalDateTime.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
//        String timestamp = currentTime.format(formatter);
//
//        System.out.println("当前时间戳: " + timestamp);
        //{"status":200, "msg":""}
        HashMap<String, String> map = new HashMap<>();
        map.put("key1","value1");
        map.put("key2","value2");
        String jsonStr= JSON.toJSONString(map);
        System.out.println(jsonStr);

    }


}
