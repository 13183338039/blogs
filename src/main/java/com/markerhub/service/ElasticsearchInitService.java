package com.markerhub.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.markerhub.entity.Blog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Elasticsearch 初始化服务
 * 在应用启动后同步所有历史博客数据到 Elasticsearch
 */
@Service
public class ElasticsearchInitService {

    @Autowired
    private BlogService blogService;

    @Autowired
    private SearchService searchService;

    /**
     * 应用启动完成后，同步所有已发布的博客到 Elasticsearch
     */
    @EventListener(ApplicationReadyEvent.class)
    public void syncAllBlogsToElasticsearch() {
        // 检查 Elasticsearch 是否可用
        if (!searchService.isElasticsearchAvailable()) {
            System.err.println("⚠ 警告: Elasticsearch 客户端不可用，跳过数据同步。");
            return;
        }
        System.out.println("========================================");
        System.out.println("ElasticsearchInitService: 开始初始化...");
        System.out.println("========================================");
        
        try {
            // 查询所有已发布的博客（status = 0 表示已发布）
            List<Blog> blogs = blogService.list(
                    new QueryWrapper<Blog>()
                            .eq("status", 0)
            );

            System.out.println("查询到 " + blogs.size() + " 篇已发布的博客");

            if (blogs.isEmpty()) {
                System.out.println("⚠ 警告: 没有需要同步的博客数据。");
                System.out.println("请检查数据库中是否有 status = 0 的博客记录。");
                return;
            }

            System.out.println("========================================");
            System.out.println("开始同步 " + blogs.size() + " 篇博客到 Elasticsearch...");
            System.out.println("========================================");

            int successCount = 0;
            int failCount = 0;

            for (Blog blog : blogs) {
                try {
                    searchService.syncBlogToEs(blog);
                    successCount++;
                    if (successCount % 10 == 0) {
                        System.out.println("已同步 " + successCount + " 篇博客...");
                    }
                } catch (Exception e) {
                    failCount++;
                    System.err.println("同步博客失败 [ID: " + blog.getId() + ", 标题: " + blog.getTitle() + "]: " + e.getMessage());
                }
            }

            System.out.println("========================================");
            System.out.println("✓ 博客同步完成！");
            System.out.println("成功: " + successCount + " 篇");
            System.out.println("失败: " + failCount + " 篇");
            System.out.println("========================================");
        } catch (Exception e) {
            System.err.println("初始化 Elasticsearch 数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

