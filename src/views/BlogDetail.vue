<template>
  <div>
    <Header></Header>

    <div class="mblog">
      <h2> {{ blog.title }}</h2>
      <div v-if="ownBlog" style="margin-bottom: 10px;">
        <el-link icon="el-icon-edit" style="margin-right: 15px;">
          <router-link :to="{name: 'BlogEdit', params: {blogId: blog.id}}" >
          编辑
          </router-link>
        </el-link>
        <el-link icon="el-icon-delete" type="danger" @click="handleDelete">
          删除
        </el-link>
      </div>
      <el-divider></el-divider>
      <div class="markdown-body" v-html="blog.content"></div>

    </div>

  </div>
</template>

<script>
  import 'github-markdown-css'
  import Header from "../components/Header";

  export default {
    name: "BlogDetail.vue",
    components: {Header},
    data() {
      return {
        blog: {
          id: "",
          title: "",
          content: ""
        },
        ownBlog: false
      }
    },
    created() {
      const blogId = this.$route.params.blogId
      console.log(blogId)
      const _this = this
      this.$axios.get('/blog/' + blogId).then(res => {
        const blog = res.data.data
        _this.blog.id = blog.id
        _this.blog.title = blog.title

        var MardownIt = require("markdown-it")
        var md = new MardownIt()

        var result = md.render(blog.content)
        _this.blog.content = result
        _this.ownBlog = (blog.userId === _this.$store.getters.getUser.id)

      })
    },
    methods: {
      handleDelete() {
        const _this = this
        this.$confirm('此操作将永久删除该博客, 是否继续?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          _this.$axios.delete('/blog/' + _this.blog.id).then(res => {
            _this.$message.success('删除成功')
            _this.$router.push('/blogs')
          }).catch(error => {
            console.error('删除失败:', error)
            if (error.response && error.response.data) {
              _this.$message.error(error.response.data.msg || '删除失败')
            } else {
              _this.$message.error('删除失败，请稍后重试')
            }
          })
        }).catch(() => {
          // 用户取消删除
        })
      }
    }
  }
</script>

<style scoped>
  .mblog {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    width: 100%;
    min-height: 700px;
    padding: 20px 15px;
  }

</style>