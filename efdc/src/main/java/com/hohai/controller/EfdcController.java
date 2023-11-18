package com.hohai.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hohai.utils.GlobalConstant;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.zip.ZipOutputStream;

import static com.hohai.utils.GlobalConstant.*;
import static com.hohai.utils.Utils.*;

@RestController
@RequestMapping("/api")
public class EfdcController {

    /**
     * 将任务文件上传到EFDC服务器
     *
     * 测试通过
     */
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
            Class.forName("com.mysql.cj.jdbc.Driver");
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

            preparedStatement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO 返回id，根据这个id拿结果数据
        return getUploadStatusJsonStr(200,"任务上传成功",id);
    }


    /**
     * 下载文件接口，根据传入的任务Id，下载到任务结果文件
     * 文件存在，则为200
     * 文件不存在，则为404 not Found
     *
     * 测试通过
     */
    @PostMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("taskId") String taskId) throws IOException {
        // 从数据库，根据任务Id, 获取到文件路径
        Path filePath = Paths.get("/Users/cui/Desktop/efdc/modle-1/" + taskId + ".txt");
        // 从文件路径创建Resource对象
        Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());

        // 检查文件是否存在并可读
        if (resource.exists() && resource.isReadable()) {
            // 设置HTTP头部
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + taskId);
            // 设置响应类型为文件流
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            // 创建ResponseEntity对象并返回
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } else {
            // 文件不存在或不可读，返回错误响应
            return ResponseEntity.notFound().build();
        }
    }


    /**
     * 任务执行情况查询接口，输入任务Id，返回任务状态:
     * 成功或者失败，以及执行进度，
     * 如果失败的话，返回报错信息
     *
     * 测试通过
     */
    @PostMapping("/checkTaskStatus")
    public JSONObject checkTaskStatus(@RequestParam("taskId") String taskId) throws IOException {
        Connection connection = null;
        Statement statement = null;
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = getConnection(DB_URL, USER_NAME, PASSWORD);
            statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery(
                    "select \n" +
                            "id,\n" +
                            "task_name,\n" +
                            "run_status,\n" +
                            "run_percent,\n" +
                            "run_err_msg\n" +
                            "from efdc.efdc_task_info\n" +
                            "where id = '" + taskId + "' limit 1");
            while(resultSet.next()){
                String id = resultSet.getString("id");
                String taskName = resultSet.getString("task_name");
                Integer runStatus = resultSet.getInt("run_status");
                Double runPercent = resultSet.getDouble("run_percent");
                String runErrMsg = resultSet.getString("run_err_msg");

                //查询到对应的任务，通过JSON返回任务状态，200表示查询到任务状态
                JSONObject taskStatusJSON = new JSONObject();
                taskStatusJSON.put("statusCode",200);
                taskStatusJSON.put("status", "任务查找成功");

                JSONObject statusDetail = new JSONObject();
                statusDetail.put("id", taskId);
                statusDetail.put("taskName", taskName);
                statusDetail.put("runStatus", runStatus);
                statusDetail.put("runPercent", runPercent);
                statusDetail.put("runErrMsg", runErrMsg);

                taskStatusJSON.put("statusDetail" , statusDetail);
                return taskStatusJSON;
            }

            statement.close();
            connection.close();

            //未查询到对应的任务，通过JSON返回任务状态，501表示未查询到任务状态
            JSONObject taskNotFoundJSON = new JSONObject();
            taskNotFoundJSON.put("statusCode",501);
            taskNotFoundJSON.put("status", "任务id不存在");
            JSONObject statusDetail = new JSONObject();
            taskNotFoundJSON.put("statusDetail" , statusDetail);
            return taskNotFoundJSON;

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                statement.close();
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        //511 服务器内部错误
        JSONObject taskNotFoundJSON = new JSONObject();
        taskNotFoundJSON.put("statusCode",501);
        taskNotFoundJSON.put("status", "服务器内部错误");
        JSONObject statusDetail = new JSONObject();
        taskNotFoundJSON.put("statusDetail" , statusDetail);
        return taskNotFoundJSON;
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
