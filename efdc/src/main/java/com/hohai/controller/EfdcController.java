package com.hohai.controller;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public String httpUpload(@RequestParam("files") MultipartFile files[]) {
        System.out.println("fileSize: " + files.length);
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i].getOriginalFilename();
            System.out.println("after parsed :" + fileName);
            File dest = new File("/Users/cui/Desktop/efdc/modle-1" + '/' + fileName);
            if (!dest.getParentFile().exists()) {
                dest.getParentFile().mkdirs();
            }
            try {
                files[i].transferTo(dest);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "test-ok";
    }

}
