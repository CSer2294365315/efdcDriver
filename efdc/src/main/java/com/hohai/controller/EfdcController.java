package com.hohai.controller;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api")
public class EfdcController {


    /**
     * 接受平台端的传参，根据传参生成一个EFDC计算任务，插入到数据库中
     *
     * @return
     */
    @PostMapping("/efdcTaskBuilder")
    public String hello() {
        return "Hello,Spring Boot 3!";
    }


    @PostMapping("/upload")
    public String upload(@RequestParam(value = "files") MultipartFile[] files,
                           @RequestParam("name") String name) {
        //1,上传文件(先复制模板，然后覆盖文件，生成可执行的文件包)
        //2,上传taskName。文件名:绝对路径+数字id ， id=流域名+当前时间戳
        //在数据库中生成task（id=流域名+当前时间戳，任务名，状态/是否在运行，百分比，报错信息，任务文件路径，结果文件路径），等待后续异步执行

        //3,返回id，根据这个id拿结果数据
        //4,

        System.out.println("fileSize: " + files.length);
        System.out.println("name：" + name);
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getOriginalFilename();
            System.out.println("after parsed :" + fileName);
            File dest = new File("/Users/cui/Desktop/test/modle-1" + '/' + fileName);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            try {
                files[i].transferTo(dest);
            } catch (Exception e) {
                e.printStackTrace();
                return "文件上传失败";
            }
        }
        return "文件上传成功";
    }

}
