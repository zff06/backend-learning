package top.zff06.config.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.config.properties.StudentProperties;

/**
 * @author zff06
 * @date 2026/9/16
 * @description StudentController
 **/
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {
    private final StudentProperties studentProperties;

    @GetMapping("/info")
    public StudentProperties getStudent(){
        return studentProperties;
    }
}
