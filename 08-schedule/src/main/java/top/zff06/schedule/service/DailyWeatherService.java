package top.zff06.schedule.service;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @auther zff06
 * @data 2026/10/21
 * @description DailyWeatherService
 **/
@Slf4j
@Service
public class DailyWeatherService {

    /** 和风天气的免费接口 1 分钟内有次数限制，复用同一个 client 走连接池，别每次 new */
    private final OkHttpClient okHttpClient = new OkHttpClient();

    @Resource
    private JavaMailSender mailSender;

    @Value("${weather.api.key}")
    private String weatherApiKey;

    @Value("${weather.api.city}")
    private String cityId;

    @Value("${weather.api.url}")
    private String weatherApiUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${email.recipients}")
    private String[] toEmails;

    /** 每天 11:22 发一次天气早报 */
    @Scheduled(cron = "0 20 13 * * ?")
    public void sendDailyWeather() {
        try {
            String weatherResponse = getWeatherData();
            if (weatherResponse == null) {
                log.error("【天气早报】 获取天气数据失败");
                return;
            }
            JSONObject weatherJson = JSONObject.parseObject(weatherResponse);
            // 和风天气返回结构：{"code":"200","daily":[{...},...]}，"daily" 单词别拼错
            JSONObject todayWeather = weatherJson.getJSONArray("daily").getJSONObject(0);
            String date = todayWeather.getString("fxDate");
            String tempMax = todayWeather.getString("tempMax");
            String tempMin = todayWeather.getString("tempMin");
            String textDay = todayWeather.getString("textDay");
            String winDirDay = todayWeather.getString("windDirDay");
            String windScaleDay = todayWeather.getString("windScaleDay");

            String subject = String.format("【每日天气早报】 %s 南京天气", LocalDate.parse(date).format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
            String content = String.format("""
                    📅 日期：%s
                    ☀️ 天气：%s
                    🌡️ 温度：%s°C ~ %s°C
                    🌬️ 风向：%s
                    💨 风力：%s级
                    💡提示：出门请根据天气预报增减衣物，注意交通安全！""",date,textDay,tempMin,tempMax,winDirDay,windScaleDay);
            sendEmail(subject,content);
            log.info("【天气早报】邮件发送成功，收件人：{}",String.join(",",toEmails));
        }catch (Exception e){
            log.error("【天气早报】执行失败",e);
        }
    }

    private String getWeatherData() throws IOException{
        String requestUrl = String.format("%s?location=%s&key=%s",weatherApiUrl,cityId,weatherApiKey);
        Request request = new Request.Builder().url(requestUrl).build();
        try(Response response = okHttpClient.newCall(request).execute()){
            if(response.isSuccessful() && response.body() != null){
                return response.body().string();
            }
            return null;
        }
    }

    private void sendEmail(String subject,String content){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmails);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }
}