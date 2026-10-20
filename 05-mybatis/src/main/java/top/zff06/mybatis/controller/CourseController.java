package top.zff06.mybatis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.zff06.mybatis.entity.Course;
import top.zff06.mybatis.service.CourseService;

import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/11
 * @description CourseController
 **/
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor

public class CourseController {
    private final CourseService courseService;

    @PostMapping("/batch")
    public boolean saveBatch(@RequestBody List<Course> courses) {
        return courseService.saveBatch(courses);
    }

    @DeleteMapping("/batch")
    public boolean removeByIds(@RequestParam List<Long> ids) {
        return courseService.removeByIds(ids);
    }

    @GetMapping
    public List<Course> list() {
        return courseService.list();
    }

}
