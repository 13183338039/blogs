# Elasticsearch 学习笔记

## docker运行
参考文章
https://cloud.tencent.com/developer/article/2123550  
docker启动Elasticsearch后仍无法访问9200，需要关闭鉴权  
https://blog.csdn.net/weixin_44554142/article/details/143994641  

## java调用原理
### 1.环境配置
        <!-- Elasticsearch Java Client (支持 ES 9.x) -->
        <dependency>
            <groupId>co.elastic.clients</groupId>
            <artifactId>elasticsearch-java</artifactId>
            <version>9.2.1</version>
        </dependency>
    
        <!-- Elasticsearch REST Client (底层客户端) -->
        <dependency>
            <groupId>org.elasticsearch.client</groupId>
            <artifactId>elasticsearch-rest-client</artifactId>
            <version>9.2.1</version>
        </dependency>
    
        <!-- Jakarta JSON API (Elasticsearch Java Client 9.x 需要) -->
        <dependency>
            <groupId>jakarta.json</groupId>
            <artifactId>jakarta.json-api</artifactId>
            <version>2.0.1</version>
        </dependency>
        <dependency>
            <groupId>org.glassfish</groupId>
            <artifactId>jakarta.json</artifactId>
            <version>2.0.1</version>
        </dependency>
    
        <!-- JSON 序列化 & HTTP 客户端，供 ES 客户端使用 -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents.client5</groupId>
            <artifactId>httpclient5</artifactId>
            <version>5.2.1</version>
        </dependency>
### 2.初始化客户端
   客户端可以分为3层结构  
   1.低级客户端：处理HTTP请求  
   2.传输层：处理序列化/反序列化  
   3.API客户端：提供类型安全的API  
### 3.基础操作

​	搜索的核心是构建搜索请求
```
SearchRequest.of(s -> s
   .index(INDEX_NAME)
   .query(multiMatchQuery)
   .highlight(highlight)
   .from((page - 1) * size)
   .size(size)
);
INDEX_NAME是索引的名称
multiMatchQuery是查询规则
highlight是高亮规则
```
#### 分页设置
   其中from，size表示  
   从搜索结果的第from条开始，输出size条  
#### 高亮规则  
Highlight.of(...) 使用 函数式构建器，h -> h ... 里的 h 就是一个 Highlight.Builder。  
在 h 上连续调用  
  
.fields(...)，为不同字段配置高亮规则 .fields(NamedValue.of("title", HighlightField.of(...)))  
这一行是给 title 字段配置高亮： 
  
HighlightField.of(f -> f.preTags(...).postTags(...))  
这里在创建一个 HighlightField 对象，用来定义“高亮片段如何包装”  
preTags:标签前插入的字符串
postTags：标签后插入的字符串
```
Highlight highlight = Highlight.of(h -> h
                    .fields(
                           NamedValue.of("title", 
                                                HighlightField.of(f -> f.preTags("<em style='color:red'>").postTags("</em>"))))
                    );
```
#### 查询规则
.query(): 设置查询的关键字  
.fields(): 设置搜索的字段，其中title^2表示标题字段的权重是 2 倍  
.fuzziness("AUTO")：开启模糊匹配，用户输错一个字母、少一个字母，都有机会被匹配到。  
.type(TextQueryType.BestFields)：多个字段的得分叠加作为排名依据  
```
               Query multiMatchQuery = Query.of(q -> q
                       .multiMatch(MultiMatchQuery.of(m -> m
                               .query(keyword)
                               .fields("title^2", "content", "description", "tags")
                               .fuzziness("AUTO")
                               .type(co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType.BestFields)
                       ))
               );
```
#### 聚合操作
size(0):表示不返回文档，只返回聚合结果  
.aggregations("hot_tags", a -> a ...) —— 定义一个名为 hot_tags 的聚合  
.terms(t -> t.field("tags").size(5))： Terms 聚合，用来统计某个字段中各个值出现的次数  
```
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
```
#### 搜索建议/自动补全
1.用所给关键词先做一个快速且简单的模糊查询，只针对标题    
2.对查询结果进行切割，将包含关键词的一整块输出，作为搜索建议/自动补全
## 完整流程概览

### 1. 应用启动阶段

```
Spring Boot 启动
    ↓
ElasticsearchConfig.restClient() 
    → 创建 RestClient 连接到 localhost:9200
    → 测试连接（GET /）
    → 返回RestClient
    ↓
ElasticsearchConfig.elasticsearchClient()
    → 捕获 RestClient
    → 创建 JSON 映射器，配置日期格式,支持 LocalDateTime、LocalDate 等 Java 8 时间类型的序列
    → 创建 Elasticsearch 客户端——ElasticsearchClient
    ↓
SearchService.initIndex() (@PostConstruct)
    → 检查索引 blog_index 是否存在
    → 如果不存在，创建索引（带字段映射）
    → 输出：✓ Elasticsearch 索引 [blog_index] 创建成功！
```
确保索引是否存在
```
boolean exists = elasticsearchClient.indices().exists(e -> e.index(INDEX_NAME)).value();
```
### 1.5 Elasticsearch 字段类型对比


| 字段类型 | 是否分词 | 用途 | 示例字段 |
|---------|----|------|---------|
| `text` |  是 | 全文搜索 | title, description, content |
| `keyword` |  否 | 精确匹配、聚合 | tags |
| `long` |  否 | 数值精确匹配 | id, userId |
| `integer` |  否 | 数值精确匹配 | status |
| `date` |  否 | 日期范围查询 | created |
```
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
```
### 2. 数据同步阶段

```
用户创建/编辑博客
    ↓
BlogController.edit()
    → blogService.saveOrUpdate(temp)  // 保存到 MySQL
    → searchService.syncBlogToEs(temp)  // 同步到 ES
        ↓
    SearchService.syncBlogToEs()
        → 检查索引是否存在（不存在则创建）
        → 将 Blog 转换为 BlogDocument
        → elasticsearchClient.index()  // 索引文档
```
更新索引
```
elasticsearchClient.index(i -> i
   .index(INDEX_NAME)
   .id(document.getId().toString())
   .document(document)//BlogDocument 通过 Jackson 自动序列化为 JSON
);
```
后续：
需要把BlogDocument和Blog只保留一个
### 3. 搜索阶段

```
前端搜索请求
    ↓
BlogController.search()
    → searchService.search(keyword, page, size)
        ↓
    SearchService.search()
        → 检查 elasticsearchClient 是否为 null
        → 构建 MultiMatchQuery（title^2, content, description, tags）
        → 构建 Highlight（高亮配置）
        → 执行 elasticsearchClient.search()
        → 处理结果和高亮
        → 返回搜索结果
```

---

## 🔍 搜索失败原因分析

### 问题 1: 索引为空（最可能的原因）

**现象**：
- 索引创建成功
- 搜索返回空结果（total: 0）

**原因**：
1. **历史数据未同步**：应用启动时只创建了索引，但 MySQL 中已有的博客数据没有同步到 Elasticsearch
2. **新数据未同步**：虽然 `BlogController.edit()` 中调用了 `syncBlogToEs()`，但：
   - 如果是在添加新功能之前创建的博客，这些数据不会自动同步
   - 只有通过编辑接口修改的博客才会同步

**验证方法**：
```bash
# 检查索引中的文档数量
curl http://localhost:9200/blog_index/_count

# 查看索引中的所有文档
curl http://localhost:9200/blog_index/_search?pretty
```

**解决方案**：
需要实现一个**数据迁移/初始化脚本**，将 MySQL 中所有已发布的博客同步到 Elasticsearch。

---

### 问题 2: 索引创建失败（被静默处理）

**现象**：
- 启动时看到警告：`⚠ 警告: 创建 Elasticsearch 索引失败`
- 搜索时返回空结果

**可能原因**：
1. Elasticsearch 未启动
2. 连接配置错误
3. 权限问题
4. 字段映射错误（如日期格式）

**验证方法**：
查看应用启动日志，确认是否有索引创建成功的消息。

---

### 问题 3: 数据同步失败（被静默处理）

**现象**：
- 编辑博客后，搜索仍然找不到

**可能原因**：
1. `syncBlogToEs()` 中的异常被捕获，只打印错误日志
2. 字段映射不匹配（如日期格式）
3. 文档 ID 冲突

**验证方法**：
查看应用日志，确认是否有 `同步博客到 Elasticsearch 失败` 的错误信息。

---

### 问题 4: 查询字段不存在

**现象**：
- 搜索时报字段不存在错误

**原因**：
- 查询中使用了 `tags` 字段，但同步时 `document.setTags(new ArrayList<>())` 设置为空列表
- 如果索引映射中没有 `tags` 字段，查询会失败

**当前代码问题**：
```java
// SearchService.java:293
document.setTags(new ArrayList<>());  // 总是设置为空，没有从 Blog 中获取
```

---

## 🐛 发现的问题

### 1. **缺少历史数据同步机制**

**问题**：
- 只有通过 `edit` 接口创建的博客才会同步到 ES
- 历史数据需要手动同步

**影响**：
- 搜索功能不完整
- 用户体验差

**建议**：
实现一个初始化脚本或定时任务，同步所有已发布的博客。

---

### 2. **Tags 字段未正确同步**

**问题**：
```java
// SearchService.java:293
document.setTags(new ArrayList<>());  // 硬编码为空列表
```

**影响**：
- Tags 搜索功能无效
- 丢失了标签信息

**建议**：
从 `Blog` 实体中获取真实的 tags 数据（如果 Blog 有 tags 字段）。

---

### 3. **错误处理过于宽泛**

**问题**：
```java
catch (Exception e) {
    System.err.println("同步博客到 Elasticsearch 失败: " + e.getMessage());
    // 静默失败，不抛出异常
}
```

**影响**：
- 数据同步失败时，用户无感知
- 难以排查问题

**建议**：
- 记录详细的错误日志（包括堆栈跟踪）
- 考虑使用消息队列异步处理，避免阻塞主流程

---

### 4. **索引创建时机问题**

**问题**：
- `@PostConstruct` 在 Spring Bean 初始化时执行
- 如果 ElasticsearchClient 还未完全初始化，可能失败

**建议**：
- 使用 `@EventListener(ApplicationReadyEvent.class)` 在应用完全启动后执行
- 或者添加重试机制

---

## ✅ 建议的改进方案

### 1. 实现数据初始化脚本

```java
@Service
public class ElasticsearchInitService {
    
    @Autowired
    private BlogService blogService;
    
    @Autowired
    private SearchService searchService;
    
    /**
     * 初始化：同步所有已发布的博客到 Elasticsearch
     */
    @PostConstruct
    public void initSyncAllBlogs() {
        // 查询所有已发布的博客（status = 0）
        List<Blog> blogs = blogService.list(
            new QueryWrapper<Blog>()
                .eq("status", 0)
        );
        
        System.out.println("开始同步 " + blogs.size() + " 篇博客到 Elasticsearch...");
        
        for (Blog blog : blogs) {
            try {
                searchService.syncBlogToEs(blog);
            } catch (Exception e) {
                System.err.println("同步博客失败 [ID: " + blog.getId() + "]: " + e.getMessage());
            }
        }
        
        System.out.println("✓ 博客同步完成！");
    }
}
```

### 2. 修复 Tags 字段同步

```java
// 如果 Blog 实体有 tags 字段
document.setTags(blog.getTags() != null ? blog.getTags() : new ArrayList<>());
```

### 3. 改进错误处理

```java
catch (Exception e) {
    log.error("同步博客到 Elasticsearch 失败 [Blog ID: {}]: {}", 
        blog.getId(), e.getMessage(), e);  // 记录完整堆栈
    // 可以考虑发送到消息队列重试
}
```

### 4. 添加健康检查

```java
@GetMapping("/search/health")
public Result checkElasticsearchHealth() {
    // 检查 ES 连接和索引状态
    // 返回健康状态
}
```

---

## 📊 排查步骤

1. **检查索引是否存在**：
   ```bash
   curl http://localhost:9200/_cat/indices?v
   ```

2. **检查索引中的文档数量**：
   ```bash
   curl http://localhost:9200/blog_index/_count
   ```

3. **查看索引映射**：
   ```bash
   curl http://localhost:9200/blog_index/_mapping?pretty
   ```

4. **测试搜索**：
   ```bash
   curl "http://localhost:9200/blog_index/_search?q=*&pretty"
   ```

5. **查看应用日志**：
   - 查找 "Elasticsearch 索引创建成功" 消息
   - 查找 "同步博客到 Elasticsearch 失败" 错误

---

## 🎯 总结

**最可能的搜索失败原因**：
1. ✅ **索引为空**：历史数据未同步（90% 可能性）
2. ⚠️ **Tags 字段未正确同步**：导致 tags 搜索无效
3. ⚠️ **数据同步失败被静默处理**：需要查看日志确认

**立即行动**：
1. 实现数据初始化脚本，同步所有历史博客
2. 修复 Tags 字段同步问题
3. 添加详细的日志记录和健康检查

