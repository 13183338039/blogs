<template>
  <div class="blogs-container">
    <Header></Header>

    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 搜索区域 -->
      <div class="search-section">
        <div class="search-wrapper">
          <div class="search-box">
            <el-autocomplete
              v-model="searchKeyword"
              :fetch-suggestions="querySearch"
              placeholder="搜索博客、文章、标签..."
              :trigger-on-focus="false"
              @select="handleSelect"
              @keyup.enter.native="handleSearch"
              class="search-input"
              clearable
              prefix-icon="el-icon-search"
            >
              <template slot-scope="{ item }">
                <div class="suggestion-item">
                  <i class="el-icon-search suggestion-icon"></i>
                  <span>{{ item.value }}</span>
                </div>
              </template>
            </el-autocomplete>
            <el-button 
              type="primary" 
              icon="el-icon-search" 
              @click="handleSearch" 
              class="search-btn"
              :loading="searchLoading"
            >
              搜索
            </el-button>
            <el-button 
              v-if="isSearchMode" 
              @click="handleReset" 
              class="reset-btn"
              icon="el-icon-refresh-left"
            >
              清除搜索
            </el-button>
          </div>
        </div>

        <!-- 搜索统计 -->
        <div v-if="searchStats && isSearchMode" class="search-stats-card">
          <div class="stats-content">
            <div class="stats-item">
              <i class="el-icon-document stats-icon"></i>
              <span class="stats-label">找到</span>
              <!-- <span class="stats-value">{{ searchStats.totalBlogs  }}</span> -->
              <span class="stats-value">{{  total  }}</span>
              <span class="stats-label">篇相关博客</span>
            </div>
            <div v-if="searchStats.hotKeywords && searchStats.hotKeywords.length > 0" class="hot-keywords">
              <i class="el-icon-star-on hot-icon"></i>
              <span class="hot-label">热门搜索：</span>
              <el-tag 
                v-for="(keyword, index) in searchStats.hotKeywords" 
                :key="index" 
                size="small" 
                type="info"
                class="hot-tag"
                @click="searchByKeyword(keyword)"
              >
                {{ keyword }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 博客列表区域 -->
      <div class="blogs-section">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <i class="el-icon-loading loading-icon"></i>
          <p>加载中...</p>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!blogs || blogs.length === 0" class="empty-container">
          <i class="el-icon-document-delete empty-icon"></i>
          <p class="empty-text">{{ isSearchMode ? '没有找到相关博客' : '暂无博客' }}</p>
          <el-button v-if="isSearchMode" @click="handleReset" type="primary" plain>
            返回博客列表
          </el-button>
        </div>

        <!-- 博客卡片列表 -->
        <div v-else class="blogs-list">
          <div 
            v-for="blog in blogs" 
            :key="blog.id" 
            class="blog-card"
          >
            <el-card class="blog-card-inner">
                <div class="blog-header">
                  <h3 class="blog-title">
                    <router-link :to="{name: 'BlogDetail', params: {blogId: blog.id}}" class="title-link">
                      <span v-html="blog.title"></span>
                    </router-link>
                  </h3>
                  <div class="blog-meta">
                    <i class="el-icon-time meta-icon"></i>
                    <span class="meta-text">{{ formatDate(blog.created) }}</span>
                  </div>
                </div>
                
                <div class="blog-content">
                  <p class="blog-description" v-html="blog.description"></p>
                  <div v-if="blog.content" class="blog-excerpt" v-html="blog.content"></div>
                </div>

                <div class="blog-footer">
                  <div v-if="blog.tags && blog.tags.length > 0" class="blog-tags">
                    <i class="el-icon-collection-tag tag-icon"></i>
                    <el-tag 
                      v-for="(tag, index) in blog.tags" 
                      :key="index" 
                      size="small" 
                      type="success"
                      class="blog-tag"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                  <router-link 
                    :to="{name: 'BlogDetail', params: {blogId: blog.id}}" 
                    class="read-more"
                  >
                    阅读全文
                    <i class="el-icon-arrow-right"></i>
                  </router-link>
                </div>
              </el-card>
            </div>
        </div>

        <!-- 分页 -->
        <div v-if="total > 0" class="pagination-container">
          <el-pagination
            background
            layout="prev, pager, next, jumper, total"
            :current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            @current-change="page"
            class="pagination"
          >
          </el-pagination>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  import Header from "../components/Header";

  export default {
    name: "Blogs.vue",
    components: {Header},
    data() {
      return {
        blogs: [],
        currentPage: 1,
        total: 0,
        pageSize: 5,
        searchKeyword: '',
        isSearchMode: false,
        searchStats: null,
        suggestions: [],
        loading: false,
        searchLoading: false
      }
    },
    methods: {
      page(currentPage) {
        const _this = this
        _this.loading = true
        if (_this.isSearchMode && _this.searchKeyword) {
          _this.search(_this.searchKeyword, currentPage)
        } else {
          _this.$axios.get("/blogs?currentPage=" + currentPage).then(res => {
            console.log(res)
            _this.blogs = res.data.data.records || []
            _this.currentPage = res.data.data.current || 1
            _this.total = res.data.data.total || 0
            _this.pageSize = res.data.data.size || 5
            _this.loading = false
          }).catch(error => {
            console.error('加载失败:', error)
            _this.$message.error('加载失败，请稍后重试')
            _this.loading = false
          })
        }
      },
      handleSearch() {
        if (!this.searchKeyword || this.searchKeyword.trim() === '') {
          this.$message.warning('请输入搜索关键词')
          return
        }
        this.isSearchMode = true
        this.currentPage = 1
        this.searchLoading = true
        this.search(this.searchKeyword.trim(), 1)
        this.loadSearchStats()
      },
      search(keyword, page) {
        const _this = this
        _this.$axios.get("/search?keyword=" + encodeURIComponent(keyword) + "&currentPage=" + page + "&pageSize=" + _this.pageSize).then(res => {
          console.log(res)
          const data = res.data.data
          _this.blogs = data.blogs || []
          _this.currentPage = data.current || 1
          _this.total = data.total || 0
          _this.pageSize = data.size || 5
          _this.loading = false
          _this.searchLoading = false
        }).catch(error => {
          console.error('搜索失败:', error)
          _this.$message.error('搜索失败，请稍后重试')
          _this.loading = false
          _this.searchLoading = false
        })
      },
      handleReset() {
        this.searchKeyword = ''
        this.isSearchMode = false
        this.searchStats = null
        this.currentPage = 1
        this.loading = true
        this.page(1)
      },
      querySearch(queryString, cb) {
        if (!queryString || queryString.trim() === '') {
          cb([])
          return
        }
        const _this = this
        _this.$axios.get("/search/suggest?keyword=" + encodeURIComponent(queryString)).then(res => {
          const suggestions = (res.data.data || []).map(item => ({
            value: item
          }))
          cb(suggestions)
        }).catch(error => {
          console.error('获取搜索建议失败:', error)
          cb([])
        })
      },
      handleSelect(item) {
        this.searchKeyword = item.value
        this.handleSearch()
      },
      loadSearchStats() {
        const _this = this
        _this.$axios.get("/search/stats").then(res => {
          _this.searchStats = res.data.data
        }).catch(error => {
          console.error('加载搜索统计失败:', error)
        })
      },
      searchByKeyword(keyword) {
        this.searchKeyword = keyword
        this.handleSearch()
      },
      formatDate(dateStr) {
        if (!dateStr) return ''
        const date = new Date(dateStr)
        const year = date.getFullYear()
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0')
        return `${year}-${month}-${day}`
      }
    },
    created() {
      this.page(1)
    }
  }
</script>

<style scoped>
  .blogs-container {
    min-height: 100vh;
    background: #34495e;
    padding-bottom: 40px;
  }

  .main-content {
    width: 100%;
    margin: 0 auto;
    padding: 30px 20px;
  }

  /* 搜索区域 */
  .search-section {
    margin-bottom: 30px;
  }

  .search-wrapper {
    background: #2c3e50;
    border: 1px solid #34495e;
    padding: 30px;
    margin-bottom: 20px;
  }

  .search-box {
    display: flex;
    gap: 12px;
    align-items: center;
  }

  .search-input {
    flex: 1;
    max-width: 600px;
  }

  .search-input >>> .el-input__inner {
    height: 48px;
    border: 1px solid #34495e;
    background: #34495e;
    color: #ecf0f1;
    font-size: 16px;
    padding-left: 50px;
  }

  .search-input >>> .el-input__inner:focus {
    border-color: #3498db;
    background: #3d566e;
  }

  .search-input >>> .el-input__inner::placeholder {
    color: #7f8c8d;
  }

  .search-input >>> .el-input__prefix {
    left: 18px;
  }

  .search-input >>> .el-input__prefix .el-input__icon {
    color: #95a5a6;
  }

  .search-btn {
    height: 48px;
    padding: 0 30px;
    font-size: 16px;
    font-weight: 500;
    background: #3498db;
    border: 1px solid #2980b9;
    color: #ecf0f1;
  }

  .search-btn:hover {
    background: #2980b9;
  }

  .reset-btn {
    height: 48px;
    padding: 0 20px;
    background: #95a5a6;
    border: 1px solid #7f8c8d;
    color: #ecf0f1;
  }

  .reset-btn:hover {
    background: #7f8c8d;
  }

  /* 搜索统计卡片 */
  .search-stats-card {
    background: #2c3e50;
    border: 1px solid #34495e;
    padding: 20px 30px;
    color: #ecf0f1;
  }

  .stats-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 20px;
  }

  .stats-item {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .stats-icon {
    font-size: 20px;
    color: #3498db;
  }

  .stats-label {
    font-size: 14px;
    color: #bdc3c7;
  }

  .stats-value {
    font-size: 24px;
    font-weight: bold;
    margin: 0 4px;
    color: #3498db;
  }

  .hot-keywords {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }

  .hot-icon {
    font-size: 18px;
    color: #3498db;
  }

  .hot-label {
    font-size: 14px;
    color: #bdc3c7;
  }

  .hot-tag {
    cursor: pointer;
    background: #34495e;
    border: 1px solid #2c3e50;
    color: #ecf0f1;
  }

  .hot-tag:hover {
    background: #3d566e;
    color: #3498db;
  }

  /* 博客列表区域 */
  .blogs-section {
    background: #2c3e50;
    border: 1px solid #34495e;
    padding: 30px;
    min-height: 400px;
  }

  /* 加载状态 */
  .loading-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 20px;
  }

  .loading-icon {
    font-size: 48px;
    color: #3498db;
  }

  .loading-container p {
    margin-top: 20px;
    color: #bdc3c7;
    font-size: 16px;
  }

  /* 空状态 */
  .empty-container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 80px 20px;
  }

  .empty-icon {
    font-size: 64px;
    color: #7f8c8d;
    margin-bottom: 20px;
  }

  .empty-text {
    font-size: 18px;
    color: #bdc3c7;
    margin-bottom: 20px;
  }

  /* 博客列表 */
  .blogs-list {
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  /* 博客卡片 */
  .blog-card {
    border-bottom: 1px solid #34495e;
  }

  .blog-card:last-child {
    border-bottom: none;
  }

  .blog-card-inner {
    border: none;
    background: #2c3e50;
  }

  .blog-card-inner >>> .el-card__body {
    padding: 20px;
  }

  .blog-header {
    margin-bottom: 16px;
  }

  .blog-title {
    margin: 0 0 12px 0;
    font-size: 24px;
    font-weight: 600;
    line-height: 1.4;
  }

  .title-link {
    color: #ecf0f1;
    text-decoration: none;
  }

  .title-link:hover {
    color: #3498db;
  }

  .blog-meta {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #95a5a6;
    font-size: 14px;
  }

  .meta-icon {
    font-size: 16px;
  }

  .blog-content {
    margin-bottom: 20px;
  }

  .blog-description {
    color: #bdc3c7;
    font-size: 16px;
    line-height: 1.8;
    margin: 0 0 12px 0;
  }

  .blog-excerpt {
    color: #95a5a6;
    font-size: 14px;
    line-height: 1.6;
    max-height: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .blog-excerpt >>> em {
    color: #e74c3c;
    font-style: normal;
    font-weight: 500;
    background: #34495e;
    padding: 2px 4px;
  }

  .blog-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 16px;
    border-top: 1px solid #34495e;
  }

  .blog-tags {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .tag-icon {
    color: #27ae60;
    font-size: 16px;
  }

  .blog-tag {
    margin: 0;
    background: #34495e;
    border: 1px solid #2c3e50;
    color: #27ae60;
  }

  .read-more {
    display: flex;
    align-items: center;
    gap: 6px;
    color: #3498db;
    text-decoration: none;
    font-size: 14px;
    font-weight: 500;
  }

  .read-more:hover {
    color: #5dade2;
  }

  /* 分页 */
  .pagination-container {
    margin-top: 40px;
    display: flex;
    justify-content: center;
  }

  .pagination {
    padding: 20px 0;
  }

  .pagination >>> .el-pagination {
    color: #bdc3c7;
  }

  .pagination >>> .el-pagination button {
    background: #34495e;
    border: 1px solid #2c3e50;
    color: #ecf0f1;
  }

  .pagination >>> .el-pagination button:hover {
    background: #3d566e;
    color: #3498db;
  }

  .pagination >>> .el-pager li {
    background: #34495e;
    border: 1px solid #2c3e50;
    color: #ecf0f1;
  }

  .pagination >>> .el-pager li:hover {
    background: #3d566e;
    color: #3498db;
  }

  .pagination >>> .el-pager li.active {
    background: #3498db;
    border-color: #2980b9;
    color: #ecf0f1;
  }

  .pagination >>> .el-pagination__jump {
    color: #bdc3c7;
  }

  .pagination >>> .el-pagination__editor {
    background: #34495e;
    border: 1px solid #2c3e50;
    color: #ecf0f1;
  }

  /* 建议项样式 */
  .suggestion-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px;
  }

  .suggestion-icon {
    color: #95a5a6;
    font-size: 16px;
  }

  /* 响应式设计 */
  @media (max-width: 768px) {
    .main-content {
      padding: 20px 15px;
    }

    .search-wrapper {
      padding: 20px;
    }

    .search-box {
      flex-direction: column;
    }

    .search-input {
      max-width: 100%;
    }

    .search-btn,
    .reset-btn {
      width: 100%;
    }

    .stats-content {
      flex-direction: column;
      align-items: flex-start;
    }

    .blogs-section {
      padding: 20px;
    }

    .blog-title {
      font-size: 20px;
    }

    .blog-footer {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
    }

    .read-more {
      width: 100%;
      justify-content: center;
    }
  }
</style>
