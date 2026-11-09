package top.zff06.zhihu.service.impl;

/**
 * @auther zff06
 * @data 2026/10/20
 * @description SpecialServiceImpl
 **/


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import top.zff06.zhihu.entity.Special;
import top.zff06.zhihu.mapper.SpecialMapper;
import top.zff06.zhihu.service.SpecialService;

import java.util.List;

/**
 * @author mqxu
 * @date 2026/10/19
 * @description SpecialServiceImpl
 **/
@Service
public class SpecialServiceImpl implements SpecialService {
    @Resource
    private SpecialMapper specialMapper;

    @Override
    public List<Special> getAll() {
        LambdaQueryWrapper<Special> wrapper = new LambdaQueryWrapper<>();
        //根据更新时间降序排列
        wrapper.orderByDesc(Special::getUpdated);
        return specialMapper.selectList(wrapper);
    }
}