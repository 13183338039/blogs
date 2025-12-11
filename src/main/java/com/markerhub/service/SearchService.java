package com.markerhub.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.NamedValue;
import com.markerhub.entity.Blog;
import com.markerhub.entity.BlogDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 搜索服务 - 使用 Elasticsearch Java Client (支持 ES 9.x)
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    @Autowired(required = false)
    private ElasticsearchClient elasticsearchClient;//config文件导入的Elasticsearch 客户端

    private static final String INDEX_NAME = "blog_index";//索引名称

    /**
     * 检查 Elasticsearch 客户端是否可用
     */
    public boolean isElasticsearchAvailable() {
        return elasticsearchClient != null;
    }

    /**
     * 初始化：创建索引（如果不存在）
     */
    @PostConstruct
    public void initIndex() {
        if (elasticsearchClient == null) {
            return;
        }
        try {
            // 检查索引是否存在
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(INDEX_NAME)).value();
            if (!exists) {
                // 创建索引（使用标准分词器，如果安装了 IK 分词器可以后续更新映射）
                elasticsearchClient.indices().create(c -> c
                        .index(INDEX_NAME)
                        .mappings(m -> m
                                .properties("id", Property.of(p -> p.long_(l -> l)))
                                .properties("userId", Property.of(p -> p.long_(l -> l)))
                                .properties("title", Property.of(p -> p.text(t -> t)))
                                .properties("description", Property.of(p -> p.text(t -> t)))
                                .properties("content", Property.of(p -> p.text(t -> t)))
                                .properties("tags", Property.of(p -> p.keyword(k -> k)))
                                .properties("created", Property.of(p -> p.date(d -> d.format("yyyy-MM-dd HH:mm:ss"))))
                                .properties("status", Property.of(p -> p.integer(i -> i)))
                        )
                );
                log.info("Elasticsearch 索引 [" + INDEX_NAME + "] 创建成功！");
            } else {
                log.info("Elasticsearch 索引 [" + INDEX_NAME + "] 已存在。");
            }
        } catch (Exception e) {
            log.error("警告: 创建 Elasticsearch 索引失败: " + e.getMessage());
            log.error("索引将在首次同步博客时自动创建。");
        }
    }

    /**
     * 全文搜索（支持高亮）
     */
    public Map<String, Object> search(String keyword, int page, int size) {
        // 检查 Elasticsearch 是否可用
        if (elasticsearchClient == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("blogs", new ArrayList<>());
            result.put("total", 0);
            result.put("current", page);
            result.put("size", size);
            result.put("totalPages", 0);
            return result;
        }

        try {
            // 构建查询
            //标题匹配更重要，提升标题匹配的排名
            // fuzziness("AUTO") 表示自动模糊匹配
            // type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields) 表示最佳匹配
            Query multiMatchQuery = Query.of(q -> q
                    .multiMatch(MultiMatchQuery.of(m -> m
                            .query(keyword)
                            .fields("title^2", "content", "description", "tags")
                            .fuzziness("AUTO")
                            .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                    ))
            );

            // 构建高亮
            // 高亮显示匹配的词
            Highlight highlight = Highlight.of(h -> h
                    .fields(NamedValue.of("title", HighlightField.of(f -> f.preTags("<em style='color:red'>").postTags("</em>"))))
                    .fields(NamedValue.of("description", HighlightField.of(f -> f.preTags("<em style='color:red'>").postTags("</em>"))))
                    .fields(NamedValue.of("content", HighlightField.of(f -> f.preTags("<em style='color:red'>").postTags("</em>"))))
            );

            // 构建搜索请求
            // 实现分页：通过 from 和 size 控制返回结果
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(multiMatchQuery)
                    .highlight(highlight)
                    .from((page - 1) * size)
                    .size(size)
            );

            // 执行搜索
            SearchResponse<BlogDocument> searchResponse = elasticsearchClient.search(searchRequest, BlogDocument.class);

            // 处理结果
            List<BlogDocument> blogs = new ArrayList<>();
            for (Hit<BlogDocument> hit : searchResponse.hits().hits()) {
                BlogDocument blogDocument = hit.source();
                if (blogDocument == null) {
                    continue;
                }

                // 处理高亮
                if (hit.highlight() != null) {
                    Map<String, List<String>> highlights = hit.highlight();
                    
                    if (highlights.containsKey("title") && !highlights.get("title").isEmpty()) {
                        blogDocument.setTitle(highlights.get("title").get(0));
                    }
                    
                    if (highlights.containsKey("description") && !highlights.get("description").isEmpty()) {
                        blogDocument.setDescription(highlights.get("description").get(0));
                    }
                    //将所有匹配的片段组合起来
                    //并在最后做截断
                    if (highlights.containsKey("content") && !highlights.get("content").isEmpty()) {
                        StringBuilder contentBuilder = new StringBuilder();
                        for (String fragment : highlights.get("content")) {
                            contentBuilder.append(fragment);
                        }
                        String content = contentBuilder.toString();
                        if (content.length() > 500) {
                            content = content.substring(0, 500) + "...";
                        }
                        blogDocument.setContent(content);
                    }
                } else {
                    // 如果没有高亮，简单处理内容长度
                    String content = blogDocument.getContent();
                    if (content != null && content.length() > 500) {
                        blogDocument.setContent(content.substring(0, 500) + "...");
                    }
                }

                blogs.add(blogDocument);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("blogs", blogs);
            result.put("total", searchResponse.hits().total().value());
            result.put("current", page);
            result.put("size", size);
            result.put("totalPages", (int) Math.ceil((double) searchResponse.hits().total().value() / size));

            // // 打印搜索结果信息
            // log.info("========== 搜索结果 ==========");
            // log.info("关键词: {}", keyword);
            // log.info("总记录数: {}", searchResponse.hits().total().value());
            // log.info("当前页码: {}", page);
            // log.info("每页大小: {}", size);
            // log.info("总页数: {}", (int) Math.ceil((double) searchResponse.hits().total().value() / size));
            // log.info("返回博客数量: {}", blogs.size());
            // log.info("==============================");

            return result;
        } catch (ElasticsearchException e) {
            // 如果是索引不存在，返回空结果而不是抛出异常
            if (e.response().error().type().equals("index_not_found_exception")) {
                log.error("警告: Elasticsearch 索引 [" + INDEX_NAME + "] 不存在，返回空结果。");
                // 尝试创建索引
                try {
                    initIndex();
                } catch (Exception ex) {
                    log.error("创建索引失败: " + ex.getMessage());
                }
                Map<String, Object> result = new HashMap<>();
                result.put("blogs", new ArrayList<>());
                result.put("total", 0);
                result.put("current", page);
                result.put("size", size);
                result.put("totalPages", 0);
                return result;
            }
            // 其他 Elasticsearch 异常
            log.error("Elasticsearch 搜索失败: " + e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("blogs", new ArrayList<>());
            result.put("total", 0);
            result.put("current", page);
            result.put("size", size);
            result.put("totalPages", 0);
            return result;
        } catch (Exception e) {
            log.error("Elasticsearch 搜索失败: " + e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("blogs", new ArrayList<>());
            result.put("total", 0);
            result.put("current", page);
            result.put("size", size);
            result.put("totalPages", 0);
            return result;
        }
    }

    /**
     * 搜索建议/自动补全
     */
    public List<String> suggest(String keyword) {
        List<String> suggestions = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty() || elasticsearchClient == null) {
            return suggestions;
        }

        try {
            // 构建查询
            Query query = Query.of(q -> q
                    .multiMatch(MultiMatchQuery.of(m -> m
                            .query(keyword)
                            .fields("title", "content", "tags")
                    ))
            );

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_NAME)
                    .query(query)
                    .size(10)
            );

            SearchResponse<BlogDocument> searchResponse = elasticsearchClient.search(searchRequest, BlogDocument.class);

            Set<String> titleWords = new HashSet<>();//用 Set 去重
            for (Hit<BlogDocument> hit : searchResponse.hits().hits()) {//遍历每个命中的博客
                BlogDocument doc = hit.source();
                if (doc != null && doc.getTitle() != null) {//只处理有标题的文档：
                    String title = doc.getTitle();
                    if (title.toLowerCase().contains(keyword.toLowerCase())) {//标题里包含关键字（忽略大小写）
                        String[] words = title.split(" ");//按空格拆分标题：
                        for (String word : words) {
                            if (word.toLowerCase().contains(keyword.toLowerCase()) && word.length() > keyword.length()) {
                                titleWords.add(word);
                            }
                        }
                    }
                }
            }

            suggestions.addAll(titleWords);
            return suggestions.subList(0, Math.min(5, suggestions.size()));
        } catch (Exception e) {
            return suggestions;
        }
    }

    /**
     * 搜索统计
     */
    public Map<String, Object> getSearchStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            if (elasticsearchClient != null) {
                SearchRequest searchRequest = SearchRequest.of(s -> s
                        .index(INDEX_NAME)
                        .size(0)
                        // 添加聚合：统计最热门的标签
                        .aggregations("hot_tags", a -> a
                                .terms(t -> t
                                        .field("tags")
                                        .size(5)  // 获取前 5 个最热门的标签
                                )
                        )
                );
                SearchResponse<BlogDocument> searchResponse = elasticsearchClient.search(searchRequest, BlogDocument.class);
                stats.put("totalBlogs", searchResponse.hits().total().value());

                // 从聚合结果中提取热门标签
                List<String> hotKeywords = new ArrayList<>();
                if (searchResponse.aggregations() != null && searchResponse.aggregations().containsKey("hot_tags")) {
                    StringTermsAggregate tagsAgg = searchResponse.aggregations().get("hot_tags").sterms();
                    if (tagsAgg != null && tagsAgg.buckets() != null && tagsAgg.buckets().array() != null) {
                        for (StringTermsBucket bucket : tagsAgg.buckets().array()) {
                            if (bucket.key() != null && !bucket.key().stringValue().isEmpty()) {
                                hotKeywords.add(bucket.key().stringValue());
                            }
                        }
                    }
                }

                // 如果聚合结果为空，使用默认值
                if (hotKeywords.isEmpty()) {
                    hotKeywords = Arrays.asList("Spring Boot3", "Java", "Vue", "Elasticsearch", "博客");
                }

                stats.put("hotKeywords", hotKeywords);
            } else {
                stats.put("totalBlogs", 0);
                stats.put("hotKeywords", Arrays.asList("Spring Boot1", "Java", "Vue", "Elasticsearch", "博客"));
            }
        } catch (Exception e) {
            log.error("获取搜索统计失败: " + e.getMessage(), e);
            stats.put("totalBlogs", 0);
            stats.put("hotKeywords", Arrays.asList("Spring Boot2", "Java", "Vue", "Elasticsearch", "博客"));
        }

        return stats;
    }

    /**
     * 同步博客到 Elasticsearch
     */
    public void syncBlogToEs(Blog blog) {
        if (elasticsearchClient == null) {
            return;
        }
        try {
            BlogDocument document = new BlogDocument();
            document.setId(blog.getId());
            document.setUserId(blog.getUserId());
            document.setTitle(blog.getTitle());
            document.setDescription(blog.getDescription());
            document.setContent(blog.getContent());
            document.setCreated(blog.getCreated());
            document.setStatus(blog.getStatus());
            document.setTags(new ArrayList<>());

            // 确保索引存在
            try {
                boolean exists = elasticsearchClient.indices().exists(e -> e.index(INDEX_NAME)).value();
                if (!exists) {
                    initIndex();
                }
            } catch (Exception ex) {
                log.error("检查索引失败: " + ex.getMessage());
            }

            elasticsearchClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(document.getId().toString())
                    .document(document)
            );
            
            // 验证文档是否成功索引
            log.info("博客已同步到 ES [ID: " + document.getId() + ", 标题: " + document.getTitle() + "]");
        } catch (Exception e) {
            log.error("同步博客到 Elasticsearch 失败 [ID: " + blog.getId() + "]: " + e.getMessage());
            e.printStackTrace(); // 打印完整堆栈，便于调试
        }
    }

    /**
     * 同步单个博客到 Elasticsearch（公共方法，供外部调用）
     */
    public void syncBlog(Blog blog) {
        syncBlogToEs(blog);
    }

    /**
     * 从 Elasticsearch 删除博客
     */
    public void deleteBlogFromEs(Long blogId) {
        if (elasticsearchClient == null) {
            return;
        }
        try {
            elasticsearchClient.delete(d -> d
                    .index(INDEX_NAME)
                    .id(blogId.toString())
            );
        } catch (Exception e) {
            log.error("从 Elasticsearch 删除博客失败: " + e.getMessage());
        }
    }
}
