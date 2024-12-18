package liuyuyang.net.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import liuyuyang.net.annotation.CheckRole;
import liuyuyang.net.model.Oss;
import liuyuyang.net.result.Result;
import liuyuyang.net.service.OssService;
import liuyuyang.net.utils.Paging;
import liuyuyang.net.vo.PageVo;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "对象存储管理")
@RestController
@CheckRole
@RequestMapping("/oss")
@AllArgsConstructor
public class OssController {
    private final OssService ossService;

    @PostMapping
    @ApiOperation("新增oss配置")
    @ApiOperationSupport(author = "laifeng", order = 1)
    public Result<String> add(@RequestBody Oss oss) {
        ossService.saveOss(oss);
        return Result.success();
    }

    @PatchMapping
    @ApiOperation("更新oss配置")
    @ApiOperationSupport(author = "laifeng", order = 2)
    public Result<String> update(@RequestBody Oss oss) {
        if (oss.getIsEnable() == 1) return Result.error("删除oss配置失败：该配置正在使用中");
        // 不允许更改平台
        oss.setPlatform(null);
        ossService.updateById(oss);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除oss配置")
    @ApiOperationSupport(author = "laifeng", order = 3)
    public Result<String> del(@PathVariable Integer id) {
        Oss oss = ossService.getById(id);
        if (oss == null) return Result.error("删除oss配置失败：该配置不存在");
        if (oss.getIsEnable() == 1) return Result.error("删除oss配置失败：该配置正在使用中");
        ossService.delOss(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("获取oss配置")
    @ApiOperationSupport(author = "laifeng", order = 4)
    public Result<Oss> get(@PathVariable Integer id) {
        return Result.success(ossService.getById(id));
    }

    @PostMapping("/page")
    @ApiOperation("获取oss配置分页")
    @ApiOperationSupport(author = "laifeng", order = 5)
    public Result<Object> page(@RequestBody Oss oss, PageVo pageVo) {
        Page<Oss> list = ossService.ossPage(oss, pageVo);
        Map<String, Object> result = Paging.filter(list);
        return Result.success(result);
    }

    @PatchMapping("/enable/{id}")
    @ApiOperation("启用oss配置")
    @ApiOperationSupport(author = "laifeng", order = 6)
    public Result enable(@PathVariable Integer id) {
        ossService.enable(id);
        return Result.success();
    }

    @PatchMapping("/disable/{id}")
    @ApiOperation("禁用oss配置")
    @ApiOperationSupport(author = "laifeng", order = 7)
    public Result disable(@PathVariable Integer id) {
        ossService.disable(id);
        return Result.success();
    }

    /**
     * 获取启用的oss配置
     *
     * @return
     */
    @GetMapping("/getEnableOss")
    @ApiOperation("获取启用的oss配置")
    @ApiOperationSupport(author = "laifeng", order = 8)
    public Result<Oss> getEnableOss() {
        return Result.success(ossService.getEnableOss());
    }
}