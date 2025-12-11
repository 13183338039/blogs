package com.markerhub.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.markerhub.common.lang.Result;
import com.markerhub.entity.Blog;
import com.markerhub.service.BlogService;
import com.markerhub.service.SearchService;
import com.markerhub.util.ShiroUtil;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 关注公众号：MarkerHub
 * @since 2020-05-25
 */
@RestController
public class BlogController {

    @Autowired
    BlogService blogService;

    @Autowired
    SearchService searchService;

    @GetMapping("/blogs")
    public Result list(@RequestParam(defaultValue = "1") Integer currentPage) {

        Page page = new Page(currentPage, 5);
        IPage pageData = blogService.page(page, new QueryWrapper<Blog>().orderByDesc("created"));

        return Result.succ(pageData);
    }

    @GetMapping("/blog/{id}")
    public Result detail(@PathVariable(name = "id") Long id) {
        Blog blog = blogService.getById(id);
        Assert.notNull(blog, "该博客已被删除");

        return Result.succ(blog);
    }

    @RequiresAuthentication
    @PostMapping("/blog/edit")
    public Result edit(@Validated @RequestBody Blog blog) {

//        Assert.isTrue(false, "公开版不能任意编辑！");

        Blog temp = null;
        if(blog.getId() != null) {
            temp = blogService.getById(blog.getId());
            // 只能编辑自己的文章
            System.out.println(ShiroUtil.getProfile().getId());
            Assert.isTrue(temp.getUserId().longValue() == ShiroUtil.getProfile().getId().longValue(), "没有权限编辑");

        } else {

            temp = new Blog();
            temp.setUserId(ShiroUtil.getProfile().getId());
            temp.setCreated(LocalDateTime.now());
            temp.setStatus(0);
        }

        BeanUtil.copyProperties(blog, temp, "id", "userId", "created", "status");
        blogService.saveOrUpdate(temp);

        // 同步到 Elasticsearch
        searchService.syncBlogToEs(temp);

        return Result.succ(null);
    }

    @RequiresAuthentication
    @DeleteMapping("/blog/{id}")
    public Result delete(@PathVariable(name = "id") Long id) {
        Blog blog = blogService.getById(id);
        Assert.notNull(blog, "该博客不存在");

        // 只能删除自己的文章
        Assert.isTrue(blog.getUserId().longValue() == ShiroUtil.getProfile().getId().longValue(), "没有权限删除");

        blogService.removeById(id);
        // 从 Elasticsearch 删除
        searchService.deleteBlogFromEs(id);
        return Result.succ(null);
    }

    /**
     * 全文搜索
     */
    @GetMapping("/search")
    public Result search(@RequestParam String keyword,
                        @RequestParam(defaultValue = "1") Integer currentPage,
                        @RequestParam(defaultValue = "5") Integer pageSize) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.fail("搜索关键词不能为空");
        }
        return Result.succ(searchService.search(keyword.trim(), currentPage, pageSize));
    }

    /**
     * 搜索建议/自动补全
     */
    @GetMapping("/search/suggest")
    public Result suggest(@RequestParam String keyword) {
        return Result.succ(searchService.suggest(keyword));
    }

    /**
     * 搜索统计
     */
    @GetMapping("/search/stats")
    public Result searchStats() {
        return Result.succ(searchService.getSearchStats());
    }

    /**
     * 手动触发同步所有博客到 Elasticsearch（管理接口）
     */
    @RequiresAuthentication
    @PostMapping("/search/sync-all")
    public Result syncAllBlogs() {
        try {
            // 查询所有已发布的博客
            List<Blog> blogs = blogService.list(
                    new QueryWrapper<Blog>().eq("status", 0)
            );

            if (blogs.isEmpty()) {
                return Result.fail("没有需要同步的博客数据（status = 0）");
            }

            int successCount = 0;
            int failCount = 0;
            List<String> errors = new ArrayList<>();

            for (Blog blog : blogs) {
                try {
                    searchService.syncBlog(blog);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("博客 ID " + blog.getId() + ": " + e.getMessage());
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", blogs.size());
            result.put("success", successCount);
            result.put("fail", failCount);
            if (!errors.isEmpty()) {
                result.put("errors", errors.subList(0, Math.min(10, errors.size())));
            }

            return Result.succ(result);
        } catch (Exception e) {
            return Result.fail("同步失败: " + e.getMessage());
        }
    }

}
