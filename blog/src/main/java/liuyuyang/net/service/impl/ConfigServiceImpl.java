package liuyuyang.net.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import liuyuyang.net.dto.project.SystemDTO;
import liuyuyang.net.execption.CustomException;
import liuyuyang.net.mapper.ConfigMapper;
import liuyuyang.net.model.Config;
import liuyuyang.net.service.ConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OperatingSystem;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements ConfigService {
    @Resource
    private ConfigMapper configMapper;

    @Override
    public Map<String, Object> list(String group) {
        List<Config> list;
        Map<String, Object> result;
        LambdaQueryWrapper<Config> wrapper = new LambdaQueryWrapper<>();

        switch (group) {
            case "all":
                list = configMapper.selectList(null);
                result = list.stream().collect(Collectors.toMap(Config::getName, Config::getValue));
                return result;
            case "web":
                wrapper.eq(Config::getGroup, "web");
                break;
            case "layout":
                wrapper.eq(Config::getGroup, "layout");
                break;
            case "system":
                SystemDTO systemInfo = getSystemInfo();
                result = new HashMap<>();
                result.put("osName", systemInfo.getOsName());
                result.put("osVersion", systemInfo.getOsVersion());
                result.put("totalMemory", systemInfo.getTotalMemory());
                result.put("availableMemory", systemInfo.getAvailableMemory());
                result.put("memoryUsage", systemInfo.getMemoryUsage());
                return result;
            default:
                throw new CustomException(400, "未知的分组");
        }

        // 不等于 "all" or "system" 触发
        list = configMapper.selectList(wrapper);
        result = list.stream().collect(Collectors.toMap(Config::getName, Config::getValue));
        return result;
    }

    @Override
    public void edit(String group, Map<String, String> data) {
        System.out.println(data);

        // switch (group) {
        //     case "layout":
        //         for (Map.Entry<String, String> entry : data.entrySet()) {
        //             String name = entry.getKey();
        //             String value = entry.getValue();
        //
        //             LambdaQueryWrapper<Config> wrapper = new LambdaQueryWrapper<>();
        //             wrapper.eq(Config::getName, name);
        //             Config config = new Config();
        //             config.setValue(value);
        //             configMapper.update(config, wrapper);
        //         }
        //
        //         break;
        // }

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            LambdaQueryWrapper<Config> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Config::getName, name);
            Config config = new Config();
            config.setValue(value);
            configMapper.update(config, wrapper);
        }
    }

    @Override
    public SystemDTO getSystemInfo() {
        SystemInfo systemInfo = new SystemInfo();

        // 获取操作系统信息
        OperatingSystem os = systemInfo.getOperatingSystem();
        // 获取系统名称
        String osName = os.getFamily();
        // 获取系统版本
        String osVersion = os.getVersionInfo().getVersion();

        // 获取操作系统内存信息
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        // 总内存量
        long totalMemory = memory.getTotal();
        // 可用内存量
        long availableMemory = memory.getAvailable();
        // 内存使用率
        double memoryUsage = (double) (totalMemory - availableMemory) / totalMemory * 100;

        SystemDTO data = new SystemDTO();
        data.setOsName(osName);
        data.setOsVersion(osVersion);
        data.setTotalMemory((int) (totalMemory / (1024.0 * 1024 * 1024)));
        data.setAvailableMemory((int) (availableMemory / (1024.0 * 1024 * 1024)));
        data.setMemoryUsage(Float.valueOf(String.format("%.2f", memoryUsage)));

        return data;
    }
}