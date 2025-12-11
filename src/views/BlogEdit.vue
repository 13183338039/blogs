<template>
  <div class="blog-edit-container">
    <Header></Header>

    <div class="m-content">
      <div class="form-wrapper">
        <h2 class="form-title">{{ ruleForm.id ? '编辑博客' : '创建博客' }}</h2>
        
        <el-form :model="ruleForm" :rules="rules" ref="ruleForm" label-width="100px" class="demo-ruleForm">
          <el-form-item label="标题" prop="title">
            <el-input v-model="ruleForm.title" class="form-input"></el-input>
          </el-form-item>

          <el-form-item label="摘要" prop="description">
            <el-input type="textarea" v-model="ruleForm.description" :rows="4" class="form-textarea"></el-input>
          </el-form-item>

          <el-form-item label="内容" prop="content">
            <mavon-editor v-model="ruleForm.content" class="mavon-editor-wrapper"></mavon-editor>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="submitForm('ruleForm')" class="submit-btn">
              {{ ruleForm.id ? '保存修改' : '立即创建' }}
            </el-button>
            <el-button @click="resetForm('ruleForm')" class="reset-btn">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script>
  import Header from "../components/Header";
  export default {
    name: "BlogEdit.vue",
    components: {Header},
    data() {
      return {
        ruleForm: {
          id: '',
          title: '',
          description: '',
          content: ''
        },
        rules: {
          title: [
            { required: true, message: '请输入标题', trigger: 'blur' },
            { min: 3, max: 25, message: '长度在 3 到 25 个字符', trigger: 'blur' }
          ],
          description: [
            { required: true, message: '请输入摘要', trigger: 'blur' }
          ],
          content: [
            { trequired: true, message: '请输入内容', trigger: 'blur' }
          ]
        }
      };
    },
    methods: {
      submitForm(formName) {
        this.$refs[formName].validate((valid) => {
          if (valid) {

            const _this = this
            this.$axios.post('/blog/edit', this.ruleForm, {
              headers: {
                "Authorization": localStorage.getItem("token")
              }
            }).then(res => {
              console.log(res)
              _this.$alert('操作成功', '提示', {
                confirmButtonText: '确定',
                callback: action => {
                  _this.$router.push("/blogs")
                }
              });

            })

          } else {
            console.log('error submit!!');
            return false;
          }
        });
      },
      resetForm(formName) {
        this.$refs[formName].resetFields();
      }
    },
    created() {
      const blogId = this.$route.params.blogId
      console.log(blogId)
      const _this = this
      if(blogId) {
        this.$axios.get('/blog/' + blogId).then(res => {
          const blog = res.data.data
          _this.ruleForm.id = blog.id
          _this.ruleForm.title = blog.title
          _this.ruleForm.description = blog.description
          _this.ruleForm.content = blog.content
        })
      }

    }
  }
</script>

<style scoped>
  .blog-edit-container {
    min-height: 100vh;
    background: #34495e;
  }

  .m-content {
    width: 100%;
    margin: 0 auto;
    padding: 30px 20px;
  }

  .form-wrapper {
    background: #2c3e50;
    border: 1px solid #34495e;
    padding: 40px;
  }

  .form-title {
    font-size: 28px;
    font-weight: 600;
    color: #ecf0f1;
    margin: 0 0 30px 0;
    padding-bottom: 20px;
    border-bottom: 1px solid #34495e;
  }

  .demo-ruleForm {
    text-align: left;
  }

  .demo-ruleForm >>> .el-form-item__label {
    color: #bdc3c7;
    font-weight: 500;
    font-size: 14px;
  }

  .form-input >>> .el-input__inner {
    background: #34495e;
    border: 1px solid #2c3e50;
    color: #ecf0f1;
    font-size: 15px;
  }

  .form-input >>> .el-input__inner:focus {
    border-color: #3498db;
    background: #3d566e;
  }

  .form-input >>> .el-input__inner::placeholder {
    color: #7f8c8d;
  }

  .form-textarea >>> .el-textarea__inner {
    background: #34495e;
    border: 1px solid #2c3e50;
    color: #ecf0f1;
    font-size: 15px;
    font-family: inherit;
  }

  .form-textarea >>> .el-textarea__inner:focus {
    border-color: #3498db;
    background: #3d566e;
  }

  .form-textarea >>> .el-textarea__inner::placeholder {
    color: #7f8c8d;
  }

  .mavon-editor-wrapper {
    min-height: 500px;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper {
    background: #34495e;
    border: 1px solid #2c3e50;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-op {
    background: #2c3e50;
    border-bottom: 1px solid #34495e;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-op .op-icon {
    color: #bdc3c7;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-op .op-icon:hover {
    color: #3498db;
    background: #34495e;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-op .op-icon-divider {
    background: #34495e;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-edit {
    background: #34495e;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-edit .v-show-content {
    background: #34495e;
    color: #ecf0f1;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-edit textarea {
    background: #34495e;
    color: #ecf0f1;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-edit textarea::placeholder {
    color: #7f8c8d;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show {
    background: #34495e;
    color: #ecf0f1;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content {
    background: #34495e;
    color: #ecf0f1;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content h1,
  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content h2,
  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content h3,
  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content h4,
  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content h5,
  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content h6 {
    color: #ecf0f1;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content p {
    color: #bdc3c7;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content code {
    background: #2c3e50;
    color: #e74c3c;
    border: 1px solid #34495e;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content pre {
    background: #2c3e50;
    border: 1px solid #34495e;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content pre code {
    background: transparent;
    border: none;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content blockquote {
    border-left: 3px solid #3498db;
    background: #2c3e50;
    color: #bdc3c7;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content table {
    border: 1px solid #34495e;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content table th,
  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content table td {
    border: 1px solid #34495e;
    background: #2c3e50;
    color: #ecf0f1;
  }

  .mavon-editor-wrapper >>> .v-note-wrapper .v-note-show .v-show-content table th {
    background: #34495e;
  }

  .submit-btn {
    background: #3498db;
    border: 1px solid #2980b9;
    color: #ecf0f1;
    font-size: 16px;
    font-weight: 500;
    padding: 12px 30px;
  }

  .submit-btn:hover {
    background: #2980b9;
  }

  .submit-btn:active {
    background: #21618c;
  }

  .reset-btn {
    background: #95a5a6;
    border: 1px solid #7f8c8d;
    color: #ecf0f1;
    font-size: 16px;
    padding: 12px 30px;
  }

  .reset-btn:hover {
    background: #7f8c8d;
  }

  .reset-btn:active {
    background: #6c7a7b;
  }

  /* 响应式设计 */
  @media (max-width: 768px) {
    .m-content {
      padding: 20px 15px;
    }

    .form-wrapper {
      padding: 30px 20px;
    }

    .form-title {
      font-size: 24px;
    }

    .demo-ruleForm {
      margin: 0;
    }

    .mavon-editor-wrapper {
      min-height: 400px;
    }
  }

  @media (max-width: 480px) {
    .form-wrapper {
      padding: 20px 15px;
    }

    .form-title {
      font-size: 20px;
      margin-bottom: 20px;
      padding-bottom: 15px;
    }

    .demo-ruleForm >>> .el-form-item {
      margin-bottom: 20px;
    }

    .submit-btn,
    .reset-btn {
      width: 100%;
      margin-bottom: 10px;
    }

    .mavon-editor-wrapper {
      min-height: 300px;
    }
  }
</style>