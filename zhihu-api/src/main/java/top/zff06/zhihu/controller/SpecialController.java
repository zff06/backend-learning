package top.zff06.zhihu.controller;

import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.zhihu.common.ResponseResult;
import top.zff06.zhihu.entity.Special;
import top.zff06.zhihu.service.SpecialService;

import java.util.List;

/**
 * @auther zff06
 * @data 2026/10/20
 * @description SpecialController
 **/
@RestController
@RequestMapping("/api/v1/special")
public class SpecialController {
    @Resource
    private SpecialService specialService;

    @GetMapping("all")
    public ResponseResult getAll() {
        List<Special> all = specialService.getAll();
        return ResponseResult.builder()
                .code(200)
                .msg("数据获取成功")
                .data(all)
                .build();
    }
}
