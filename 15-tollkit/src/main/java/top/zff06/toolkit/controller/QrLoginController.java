package top.zff06.toolkit.controller;

import cn.hutool.core.lang.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.zff06.toolkit.common.BusinessException;
import top.zff06.toolkit.common.ErrorCode;
import top.zff06.toolkit.common.Result;
import top.zff06.toolkit.dto.QrStatusResponse;
import top.zff06.toolkit.dto.QrTicketResponse;
import top.zff06.toolkit.service.QrLoginService;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description QrLoginController
 **/
@RestController
@RequestMapping("/toolkit/qr-login")
@RequiredArgsConstructor
public class QrLoginController {

    private final QrLoginService qrLoginService;

    /**
     * 生成二维码
     */
    @PostMapping("/create")
    public Result<QrTicketResponse> create() {
        return Result.ok(qrLoginService.create());
    }

    /**
     * 前端轮询，建议 1 到 2 秒一次
     */
    @GetMapping("/status")
    public Result<QrStatusResponse> status(@RequestParam String ticket) {
        return Result.ok(qrLoginService.status(ticket));
    }

    /**
     * 模拟手机端扫码
     */
    @PostMapping("/scan")
    public Result<QrStatusResponse> scan(@RequestParam String ticket) {
        return Result.ok("扫码成功", qrLoginService.scan(ticket));
    }

    /**
     * 模拟手机端确认登录
     */
    @PostMapping("/confirm")
    public Result<QrStatusResponse> confirm(@RequestParam String ticket,
                                            @RequestParam String phone) {
        if (!Validator.isMobile(phone)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号格式不正确");
        }
        return Result.ok("登录成功", qrLoginService.confirm(ticket, phone));
    }

    /**
     * 模拟手机端取消
     */
    @PostMapping("/cancel")
    public Result<QrStatusResponse> cancel(@RequestParam String ticket) {
        return Result.ok("已取消", qrLoginService.cancel(ticket));
    }

    /**
     * 终端里查看二维码，纯文本
     */
    @GetMapping(value = "/ascii", produces = "text/plain;charset=UTF-8")
    public String ascii(@RequestParam String ticket) {
        return qrLoginService.ascii(ticket);
    }
}
