package com.markerhub.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 配置类 - 使用 Elasticsearch Java Client (支持 ES 9.x)
 * 目的是测试是否连通，并返回Elasticsearch 客户端
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${spring.data.elasticsearch.uris:http://localhost:9200}")
    private String uris;

    @Bean
    public RestClient restClient() {//结构1:低级客户端：处理HTTP请求
        try {
            // 解析 URI
            String uri = uris.split(",")[0].trim();
            String url = uri.replace("http://", "").replace("https://", "");
            String[] hostPort = url.split(":");
            String host = hostPort[0];
            int port =  Integer.parseInt(hostPort[1]) ;

            System.out.println("正在连接到 Elasticsearch: " + host + ":" + port);

            //创建 Elasticsearch 的 REST 客户端实例
            RestClient restClient = RestClient.builder(new HttpHost(host, port, "http")).build();

            // 测试连接
            try {
                //curl http://localhost:9200/
                org.elasticsearch.client.Request request = new org.elasticsearch.client.Request("GET", "/");
                org.elasticsearch.client.Response response = restClient.performRequest(request);
                if (response.getStatusLine().getStatusCode() == 200) {
                    System.out.println("✓ Elasticsearch 连接成功！");
                }
            } catch (Exception e) {
                System.err.println("警告: Elasticsearch 连接测试失败，但将继续启动。");
            }

            return restClient;
        } catch (Exception e) {
            System.err.println("创建 Elasticsearch RestClient 失败: " + e.getMessage());
            System.err.println("应用将继续启动，但搜索功能将不可用。");
            throw new RuntimeException("无法连接到 Elasticsearch", e);
        }
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        // 结构2:传输层：处理序列化/反序列化

        // 创建 JSON 映射器，配置日期格式
        ObjectMapper objectMapper = new ObjectMapper();
        // 自动发现并注册模块（如 JavaTimeModule）JavaTimeModule 支持 LocalDateTime、LocalDate 等 Java 8 时间类型的序列化
        // 注册 JavaTimeModule 以支持 LocalDateTime 序列化
        objectMapper.findAndRegisterModules();
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);

        // 创建传输层
        ElasticsearchTransport transport = new RestClientTransport(restClient, jsonpMapper);

        // 结构3:API客户端：提供类型安全的API
        // 创建 Elasticsearch 客户端
        return new ElasticsearchClient(transport);
    }
}
