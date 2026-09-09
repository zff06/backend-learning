package top.zff06.quickstart.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.quickstart.entity.User;

import java.time.LocalDate;

/**
 * @auther zff06
 * @data 2026/9/9
 * @description UserController
 **/
@RestController
@RequestMapping("/user")
public class UserController {
    @GetMapping("/info")
    private User getUserInfo(){
        return new User(1001L,"李四", LocalDate.of(2004,3,15));
    }
}
