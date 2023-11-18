package com.hohai.utils;

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

public class Utils {
    /**
     * 将文件夹递归的复制到另一个文件夹，复制成功返回true，复制失败返回false
     */
    public static boolean copyFolderInvoker(String sourceFolderPath, String destinationFolderPath) {
        try {
            // 创建目标文件夹
            File destinationFolder = new File(destinationFolderPath);
            if (!destinationFolder.exists()) {
                destinationFolder.mkdirs();
            }

            // 复制文件夹及其下面的所有文件
            // 获取源文件夹下的所有文件和子文件夹
            File[] files = new File(sourceFolderPath).listFiles();
            if (files != null) {
                // 遍历复制每个文件或子文件夹
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 如果是子文件夹，则递归复制子文件夹
                        File newDestinationFolder = new File(destinationFolder, file.getName());
                        newDestinationFolder.mkdirs();
                        copyFolder(file, newDestinationFolder);
                    } else {
                        // 如果是文件，则复制文件到目标文件夹
                        File newFile = new File(destinationFolder, file.getName());
                        Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            System.out.println("文件夹复制完成！路径为:" + destinationFolderPath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFolder(File sourceFolder, File destinationFolder) throws IOException {
        // 获取源文件夹下的所有文件和子文件夹
        File[] files = sourceFolder.listFiles();
        if (files != null) {
            // 遍历复制每个文件或子文件夹
            for (File file : files) {
                if (file.isDirectory()) {
                    // 如果是子文件夹，则递归复制子文件夹
                    File newDestinationFolder = new File(destinationFolder, file.getName());
                    newDestinationFolder.mkdirs();
                    copyFolder(file, newDestinationFolder);
                } else {
                    // 如果是文件，则复制文件到目标文件夹
                    File newFile = new File(destinationFolder, file.getName());
                    Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * 获取到当前时间，格式如202311181500
     */
    public static String getCurrentTimeStampStr() {
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        String timestampStr = currentTime.format(formatter);
        return timestampStr;
    }

    //TODO 获取到JDBC连接
    public static Connection getConnection(String url, String userName, String password) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(
                    url,
                    userName,
                    password);
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 生成返回JSON
     */
    public static String getUploadStatusJsonStr(Integer statusCode, String msg,String id) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("status", statusCode);
        map.put("msg", msg);
        map.put("taskId",id);
        String jsonStr = JSON.toJSONString(map);
        return jsonStr;
    }

    /**
     * 将磁盘中的某个文件中的某个字符串，替换为指定的字符串
     */
    public static boolean replaceFileString(String filePath , String searchStr , String replaceStr) {
        try {
            // 读取文件内容
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            reader.close();

            // 执行字符串替换
            String updatedContent = content.toString().replace(searchStr, replaceStr);

            // 覆盖源文件
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(updatedContent);
            writer.close();

            System.out.println("字符串替换完成。");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        replaceFileString("/Users/cui/Desktop/efdc/秦淮河_202311181510/getefdc.inp",
                "@@@EfdcInpFilePath@@@",
                "/Users/cui/Desktop/efdc/秦淮河_202311181510/test"
                );
    }
}
