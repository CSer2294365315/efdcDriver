package com.hohai;

import com.hohai.schedule.EfdcScheduleTask;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.hohai.schedule.EfdcScheduleTask.invokeEfdcTask;
import static com.hohai.utils.GlobalConstant.*;
import static com.hohai.utils.Utils.getConnection;

@SpringBootApplication
public class MainApplication {
    public static void main(String[] args) {


        SpringApplication.run(MainApplication.class,args);
    }




}
