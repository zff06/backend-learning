package top.zff06.toolkit.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.zff06.toolkit.common.Result;
import top.zff06.toolkit.dto.InspectRequest;
import top.zff06.toolkit.dto.InspectSummary;
import top.zff06.toolkit.service.InspectService;

/**
 * 案例 11：接口巡检
 *
 * @auther zff06
 * @data 2026/10/22
 **/
@RestController
@RequestMapping("/toolkit/inspect")
@RequiredArgsConstructor
public class InspectController {

    private final InspectService inspectService;

    @PostMapping
    public Result<InspectSummary> inspect(@RequestBody InspectRequest request) {
        return Result.ok(inspectService.inspect(request.urls()));
    }
}
