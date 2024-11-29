package liuyuyang.net.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import liuyuyang.net.execption.CustomException;
import liuyuyang.net.mapper.*;
import liuyuyang.net.model.Article;
import liuyuyang.net.model.ArticleCate;
import liuyuyang.net.model.ArticleConfig;
import liuyuyang.net.model.Cate;
import liuyuyang.net.service.ArticleCateService;
import liuyuyang.net.service.ArticleService;
import liuyuyang.net.service.CateService;
import liuyuyang.net.utils.YuYangUtils;
import liuyuyang.net.vo.PageVo;
import liuyuyang.net.vo.article.ArticleFillterVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private ArticleCateMapper articleCateMapper;
    @Resource
    private ArticleCateService articleCateService;
    @Resource
    private ArticleConfigMapper articleConfigMapper;
    @Resource
    private CateMapper cateMapper;
    @Resource
    private CateService cateService;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private YuYangUtils yuYangUtils;

    @Override
    public void add(Article article) {
        articleMapper.insert(article);

        // 新增分类
        List<Integer> cateIdList = article.getCateIds();
        if (!cateIdList.isEmpty()) {
            ArrayList<ArticleCate> cateArrayList = new ArrayList<>(cateIdList.size());
            for (Integer id : cateIdList) {
                ArticleCate articleCate = new ArticleCate();
                articleCate.setArticleId(article.getId());
                articleCate.setCateId(id);
                cateArrayList.add(articleCate);
            }
            articleCateService.saveBatch(cateArrayList);
        }

        // 新增文章配置
        ArticleConfig config = article.getConfig();
        ArticleConfig articleConfig = new ArticleConfig();
        articleConfig.setArticleId(article.getId());
        articleConfig.setStatus(config.getStatus());

        // 如果密码不等于空则加密
        if (!config.getPassword().isEmpty())
            articleConfig.setPassword(DigestUtils.md5DigestAsHex(config.getPassword().getBytes()));

        articleConfigMapper.insert(articleConfig);
    }

    @Override
    public void del(Integer id, Integer is_del) {
        Article article = articleMapper.selectById(id);

        if (is_del == 0) {
            // 严格删除：直接从数据库删除
            // 删除之前绑定的分类
            QueryWrapper<ArticleCate> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("article_id", id);
            articleCateMapper.delete(queryWrapper);

            // 删除文章配置
            QueryWrapper<ArticleConfig> queryArticleConfigWrapper = new QueryWrapper<>();
            queryArticleConfigWrapper.in("article_id", article.getId());
            articleConfigMapper.delete(queryArticleConfigWrapper);

            // 删除当前文章
            articleMapper.deleteById(id);
        }

        if (is_del == 1) {
            // 普通删除：更改 is_del 字段，到时候可以通过更改字段恢复
            article.setIsDel(1);
            articleMapper.updateById(article);
        }

        if (is_del != 0 && is_del != 1) {
            throw new CustomException(400, "参数有误：请选择是否严格删除");
        }
    }

    @Override
    public void reduction(Integer id) {
        Article article = articleMapper.selectById(id);
        article.setIsDel(0);
        articleMapper.updateById(article);
    }

    @Override
    public void delBatch(List<Integer> ids) {
        // 先删除之前绑定的分类
        for (Integer id : ids) {
            QueryWrapper<ArticleCate> queryWrapperArticleCate = new QueryWrapper<>();
            queryWrapperArticleCate.in("article_id", id);
            articleCateMapper.delete(queryWrapperArticleCate);
        }

        // 再批量删除文章
        QueryWrapper<Article> queryWrapperArticle = new QueryWrapper<>();
        queryWrapperArticle.in("id", ids);
        int res = articleMapper.delete(queryWrapperArticle);
        if (res == 0) throw new CustomException(400, "批量删除文章失败");
    }

    @Override
    public void edit(Article article) {
        if (article.getCateIds() == null) throw new CustomException(400, "编辑失败：请绑定分类");

        // 删除之前绑定的分类
        QueryWrapper<ArticleCate> queryArticleCateWrapper = new QueryWrapper<>();
        queryArticleCateWrapper.in("article_id", article.getId());
        articleCateMapper.delete(queryArticleCateWrapper);

        // 删除之前绑定的文章配置
        QueryWrapper<ArticleConfig> queryArticleConfigWrapper = new QueryWrapper<>();
        queryArticleConfigWrapper.in("article_id", article.getId());
        articleConfigMapper.delete(queryArticleConfigWrapper);

        // 重新绑定分类
        for (Integer id : article.getCateIds()) {
            ArticleCate articleCate = new ArticleCate();
            articleCate.setArticleId(article.getId());
            articleCate.setCateId(id);
            articleCateMapper.insert(articleCate);
        }

        // 重新绑定文章配置
        ArticleConfig config = article.getConfig();
        ArticleConfig articleConfig = new ArticleConfig();
        articleConfig.setArticleId(article.getId());
        articleConfig.setStatus(config.getStatus());

        // 如果密码不等于空则加密
        if (!config.getPassword().isEmpty())
            articleConfig.setPassword(DigestUtils.md5DigestAsHex(config.getPassword().getBytes()));
        articleConfigMapper.insert(articleConfig);

        // 修改文章
        articleMapper.updateById(article);
    }

    @Override
    public Article get(Integer id, String password, String token) {
        Article data = bindingData(id);

        String description = data.getDescription();
        String content = data.getContent();

        Boolean isAdmin = !"".equals(token) && yuYangUtils.isAdmin(token);

        ArticleConfig config = data.getConfig();

        if (config.getPassword().isEmpty() && !password.isEmpty()) {
            throw new CustomException(610, "该文章不需要访问密码");
        }

        // 管理员可以查看任何权限的文章
        if (!isAdmin) {
            if ("hide".equals(config.getStatus())) {
                throw new CustomException(611, "该文章已被隐藏");
            }

            // 如果有密码就必须通过密码才能查看
            if (!config.getPassword().isEmpty()) {
                // 如果需要访问密码且没有传递密码参数
                if (password.isEmpty()) {
                    throw new CustomException(612, "请输入文章访问密码");
                }

                data.setDescription("该文章需要密码才能查看");
                data.setContent("该文章需要密码才能查看");

                // 验证密码是否正确
                if (config.getPassword().equals(DigestUtils.md5DigestAsHex(password.getBytes()))) {
                    data.setDescription(description);
                    data.setContent(content);
                } else {
                    throw new CustomException(613, "文章访问密码错误");
                }
            }
        }

        // 获取当前文章的创建时间
        String createTime = data.getCreateTime();

        // 查询上一篇文章
        QueryWrapper<Article> prevQueryWrapper = new QueryWrapper<>();
        prevQueryWrapper.lt("create_time", createTime).orderByDesc("create_time").last("LIMIT 1");
        Article prevArticle = articleMapper.selectOne(prevQueryWrapper);
        if (prevArticle != null) {
            Map<String, Object> resultPrev = new HashMap<>();
            resultPrev.put("id", prevArticle.getId());
            resultPrev.put("title", prevArticle.getTitle());
            data.setPrev(resultPrev);
        }

        // 查询下一篇文章
        QueryWrapper<Article> nextQueryWrapper = new QueryWrapper<>();
        nextQueryWrapper.gt("create_time", createTime).orderByAsc("create_time").last("LIMIT 1");
        Article nextArticle = articleMapper.selectOne(nextQueryWrapper);
        if (nextArticle != null) {
            Map<String, Object> resultNext = new HashMap<>();
            resultNext.put("id", nextArticle.getId());
            resultNext.put("title", nextArticle.getTitle());
            data.setNext(resultNext);
        }

        return data;
    }

    @Override
    public List<Article> list(ArticleFillterVo filterVo, String token) {
        QueryWrapper<Article> queryWrapper = queryWrapperArticle(filterVo);
        queryWrapper.eq("is_draft", filterVo.getIsDraft());
        queryWrapper.eq("is_del", filterVo.getIsDel());
        List<Article> list = articleMapper.selectList(queryWrapper);

        Boolean isAdmin = yuYangUtils.isAdmin(token);
        list = list.stream()
                .map(article -> bindingData(article.getId()))
                // 如果是普通用户则不显示隐藏的文章，如果是管理员则显示
                .filter(article -> isAdmin || !Objects.equals(article.getConfig().getStatus(), "hide"))
                .collect(Collectors.toList());

        for (Article article : list) {
            ArticleConfig config = article.getConfig();
            // 如果有密码就必须通过密码才能查看
            if (!config.getPassword().isEmpty()) {
                article.setDescription("该文章是加密的");
                article.setContent("该文章是加密的");
            }
        }

        return list;
    }

    @Override
    public Page<Article> paging(ArticleFillterVo filterVo, PageVo pageVo, String token) {
        List<Article> list = list(filterVo, token);
        Boolean isAdmin = yuYangUtils.isAdmin(token);
        if (!isAdmin) {
            list = list.stream().filter(k -> !Objects.equals(k.getConfig().getStatus(), "no_home")).collect(Collectors.toList());
        }
        Page<Article> result = yuYangUtils.getPageData(pageVo, list);
        return result;
    }

    @Override
    public Page<Article> getCateArticleList(Integer id, PageVo pageVo) {
        // 先通过分类id查询出所有文章id
        QueryWrapper<ArticleCate> queryWrapperArticleCate = new QueryWrapper<>();
        queryWrapperArticleCate.in("cate_id", id);
        List<Integer> articleIds = articleCateMapper.selectList(queryWrapperArticleCate).stream().map(ArticleCate::getArticleId).collect(Collectors.toList());

        // 然后通过文章的id查询出对应的文章数据
        QueryWrapper<Article> queryWrapperArticle = new QueryWrapper<>();
        queryWrapperArticle.orderByDesc("create_time");

        // 有数据就查询，没有就返回空数组
        if (!articleIds.isEmpty()) {
            queryWrapperArticle.in("id", articleIds);
        } else {
            queryWrapperArticle.in("id", -1);
        }

        Page<Article> page = new Page<>(pageVo.getPage(), pageVo.getSize());
        articleMapper.selectPage(page, queryWrapperArticle);

        for (Article article : page.getRecords()) {
            QueryWrapper<ArticleConfig> articleConfigQueryWrapper = new QueryWrapper<>();
            articleConfigQueryWrapper.eq("article_id", article.getId());
            ArticleConfig config = articleConfigMapper.selectOne(articleConfigQueryWrapper);

            // 如果有密码就必须通过密码才能查看
            if (!config.getPassword().isEmpty()) {
                article.setDescription("该文章是加密的");
                article.setContent("该文章是加密的");
            }
        }

        page.setRecords(page.getRecords().stream().map(article -> bindingData(article.getId())).collect(Collectors.toList()));

        return page;
    }

    @Override
    public List<Article> getRandomArticles(Integer count) {
        List<Integer> ids = articleMapper.selectList(null).stream()
                .filter(k -> {
                    QueryWrapper<ArticleConfig> articleConfigQueryWrapper = new QueryWrapper<>();
                    articleConfigQueryWrapper.eq("article_id", k.getId());
                    ArticleConfig config = articleConfigMapper.selectOne(articleConfigQueryWrapper);
                    return "".equals(config.getPassword()) && Objects.equals(config.getStatus(), "default");
                })
                .map(Article::getId)
                .collect(Collectors.toList());

        if (ids.size() <= count) {
            // 如果文章数量少于或等于需要的数量，直接返回所有文章
            return ids.stream()
                    .map(id -> get(id, "", ""))
                    .collect(Collectors.toList());
        }

        // 随机打乱文章ID列表
        Collections.shuffle(ids, new Random());

        // 选择前 count 个文章ID
        List<Integer> randomArticleIds = ids.subList(0, count);

        // 根据随机选择的文章ID获取文章
        return randomArticleIds.stream()
                .map(this::bindingData)
                .collect(Collectors.toList());
    }

    @Override
    public List<Article> getRecommendedArticles(Integer count) {
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("view").last("LIMIT " + count);
        return list(queryWrapper);
    }

    @Override
    public void recordView(Integer id) {
        Article data = articleMapper.selectById(id);
        if (data == null) throw new CustomException(400, "获取失败：该文章不存在");
        data.setView(data.getView() + 1);
        articleMapper.updateById(data);
    }

    // 关联文章数据
    @Override
    public Article bindingData(Integer id) {
        Article data = articleMapper.selectById(id);

        if (data == null) throw new CustomException(400, "获取文章失败：该文章不存在");

        // 查询当前文章的分类ID
        QueryWrapper<ArticleCate> queryWrapperCateIds = new QueryWrapper<>();
        queryWrapperCateIds.eq("article_id", id);
        List<Integer> cids = articleCateMapper.selectList(queryWrapperCateIds).stream().map(ArticleCate::getCateId).collect(Collectors.toList());

        // 如果有分类，则绑定分类信息
        if (!cids.isEmpty()) {
            QueryWrapper<Cate> queryWrapperCateList = new QueryWrapper<>();
            queryWrapperCateList.in("id", cids);
            List<Cate> cates = cateService.buildCateTree(cateMapper.selectList(queryWrapperCateList), 0);
            data.setCateList(cates);
        }

        data.setTagList(articleMapper.getTagList(id));
        data.setComment(commentMapper.getCommentList(id).size());

        // 查找文章配置
        QueryWrapper<ArticleConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("article_id", id);
        ArticleConfig articleConfig = articleConfigMapper.selectOne(queryWrapper);
        data.setConfig(articleConfig);

        return data;
    }

    // 过滤文章数据
    @Override
    public QueryWrapper<Article> queryWrapperArticle(ArticleFillterVo filterVo) {
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");

        // 根据关键字通过标题过滤出对应文章数据
        if (filterVo.getKey() != null && !filterVo.getKey().isEmpty()) {
            queryWrapper.like("title", "%" + filterVo.getKey() + "%");
        }

        // 根据开始与结束时间过滤
        if (filterVo.getStartDate() != null && filterVo.getEndDate() != null) {
            queryWrapper.between("create_time", filterVo.getStartDate(), filterVo.getEndDate());
        } else if (filterVo.getStartDate() != null) {
            queryWrapper.ge("create_time", filterVo.getStartDate());
        } else if (filterVo.getEndDate() != null) {
            queryWrapper.le("create_time", filterVo.getEndDate());
        }

        // 根据分类id过滤
        if (filterVo.getCateIds() != null && !filterVo.getCateIds().isEmpty()) {
            QueryWrapper<ArticleCate> queryWrapperArticleIds = new QueryWrapper<>();
            queryWrapperArticleIds.in("cate_id", filterVo.getCateIds());
            List<Integer> articleIds = articleCateMapper.selectList(queryWrapperArticleIds).stream().map(ArticleCate::getArticleId).collect(Collectors.toList());

            if (!articleIds.isEmpty()) {
                queryWrapper.in("id", articleIds);
            } else {
                // 添加一个始终为假的条件
                queryWrapper.in("id", -1); // -1 假设为不存在的ID
            }
        }

        // 根据标签id过滤
        if (filterVo.getTagId() != null) {
            queryWrapper.like("tag_ids", "%" + filterVo.getTagId() + "%");
        }

        return queryWrapper;
    }
}