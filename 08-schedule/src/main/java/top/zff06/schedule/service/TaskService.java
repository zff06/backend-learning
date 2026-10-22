package top.zff06.schedule.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @auther zff06
 * @data 2026/10/21
 * @description TestService
 **/
@Slf4j
@Service
public class TaskService {
    /*@Scheduled(cron = "0/3 * * * * ?")
    public void cronTask() {
        log.info("【Cron任务】每隔3秒触发一次,执行时间：{} ,执行线程：{}", LocalDateTime.now(), Thread.currentThread().getName());
    }*/
    @Scheduled(cron ="0 44 9 * * ?")
    public void cronTask(){
        log.info("【Cron任务】 指定时间出发,执行时间：{} ,执行线程：{}", LocalDateTime.now(),Thread.currentThread().getName());
    }
}
