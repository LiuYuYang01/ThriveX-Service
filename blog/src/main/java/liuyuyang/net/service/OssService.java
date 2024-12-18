package liuyuyang.net.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import liuyuyang.net.model.Oss;
import liuyuyang.net.vo.PageVo;

public interface OssService extends IService<Oss> {
    /**
     * 分页
     */
    Page<Oss> ossPage(Oss oss, PageVo pageVo);

    void enable(Integer id);

    /**
     * 获取当前启用的oss
     */
    Oss getEnableOss();

    void disable(Integer id);

    /**
     * 删除
     */
    void delOss(Integer id);

    /**
     * 新增
     * @param oss
     */
    void saveOss(Oss oss);
}
