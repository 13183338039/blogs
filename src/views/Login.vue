<template>
  <div class="login-container">
    <!-- 主内容区域 -->
    <div class="login-content">
      <!-- Logo 区域 -->
      <div class="logo-section">
        <div class="logo-wrapper">
          <img src="/logo.jpg" alt="Logo" class="logo-icon">
          <h1 class="logo-title">Zhang Shuhao's Blog</h1>
          <p class="logo-subtitle">欢迎来到张书豪的博客平台</p>
        </div>
      </div>

      <!-- 登录/注册卡片 -->
      <div class="form-card">
        <el-tabs 
          v-model="activeTab" 
          @tab-click="handleTabClick" 
          class="auth-tabs"
        >
          <!-- 登录 Tab -->
          <el-tab-pane label="登录" name="login">
            <div class="tab-content">
              <div class="tab-header">
                <i class="el-icon-user tab-icon"></i>
                <h2 class="tab-title">欢迎回来</h2>
                <p class="tab-subtitle">登录您的账户以继续</p>
              </div>

              <el-form 
                :model="loginForm" 
                :rules="loginRules" 
                ref="loginForm" 
                class="auth-form"
                label-position="top"
              >
                <el-form-item label="用户名" prop="username">
                  <el-input 
                    v-model="loginForm.username"
                    placeholder="请输入用户名"
                    prefix-icon="el-icon-user"
                    size="large"
                    class="form-input"
                  ></el-input>
                </el-form-item>

                <el-form-item label="密码" prop="password">
                  <el-input 
                    type="password" 
                    v-model="loginForm.password"
                    placeholder="请输入密码"
                    prefix-icon="el-icon-lock"
                    size="large"
                    class="form-input"
                    show-password
                  ></el-input>
                </el-form-item>

                <el-form-item>
                  <el-button 
                    type="primary" 
                    @click="submitLogin('loginForm')" 
                    class="submit-btn"
                    size="large"
                    :loading="loginLoading"
                  >
                    <span v-if="!loginLoading">登录</span>
                    <span v-else>登录中...</span>
                  </el-button>
                </el-form-item>

                <div class="form-footer">
                  <el-button 
                    @click="resetForm('loginForm')" 
                    type="text"
                    class="reset-link"
                  >
                    <i class="el-icon-refresh-left"></i>
                    重置表单
                  </el-button>
                </div>
              </el-form>
            </div>
          </el-tab-pane>

          <!-- 注册 Tab -->
          <el-tab-pane label="注册" name="register">
            <div class="tab-content">
              <div class="tab-header">
                <i class="el-icon-user-solid tab-icon"></i>
                <h2 class="tab-title">创建账户</h2>
                <p class="tab-subtitle">注册新账户以开始使用</p>
              </div>

              <el-form 
                :model="registerForm" 
                :rules="registerRules" 
                ref="registerForm" 
                class="auth-form"
                label-position="top"
              >
                <el-form-item label="用户名" prop="username">
                  <el-input 
                    v-model="registerForm.username"
                    placeholder="请输入用户名（3-15个字符）"
                    prefix-icon="el-icon-user"
                    size="large"
                    class="form-input"
                  ></el-input>
                </el-form-item>

                <el-form-item label="邮箱" prop="email">
                  <el-input 
                    v-model="registerForm.email"
                    placeholder="请输入邮箱地址"
                    prefix-icon="el-icon-message"
                    size="large"
                    class="form-input"
                  ></el-input>
                </el-form-item>

                <el-form-item label="密码" prop="password">
                  <el-input 
                    type="password" 
                    v-model="registerForm.password"
                    placeholder="请输入密码（至少6个字符）"
                    prefix-icon="el-icon-lock"
                    size="large"
                    class="form-input"
                    show-password
                  ></el-input>
                </el-form-item>

                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input 
                    type="password" 
                    v-model="registerForm.confirmPassword"
                    placeholder="请再次输入密码"
                    prefix-icon="el-icon-lock"
                    size="large"
                    class="form-input"
                    show-password
                  ></el-input>
                </el-form-item>

                <el-form-item>
                  <el-button 
                    type="primary" 
                    @click="submitRegister('registerForm')" 
                    class="submit-btn"
                    size="large"
                    :loading="registerLoading"
                  >
                    <span v-if="!registerLoading">立即注册</span>
                    <span v-else>注册中...</span>
                  </el-button>
                </el-form-item>

                <div class="form-footer">
                  <el-button 
                    @click="resetForm('registerForm')" 
                    type="text"
                    class="reset-link"
                  >
                    <i class="el-icon-refresh-left"></i>
                    重置表单
                  </el-button>
                </div>
              </el-form>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 底部链接 -->
      <div class="footer-links">
        <router-link to="/blogs" class="back-link">
          <i class="el-icon-arrow-left"></i>
          返回首页
        </router-link>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: "Login",
    data() {
      // 验证确认密码
      const validateConfirmPassword = (rule, value, callback) => {
        if (value === '') {
          callback(new Error('请再次输入密码'));
        } else if (value !== this.registerForm.password) {
          callback(new Error('两次输入密码不一致!'));
        } else {
          callback();
        }
      };

      // 验证邮箱格式
      const validateEmail = (rule, value, callback) => {
        const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (value === '') {
          callback(new Error('请输入邮箱'));
        } else if (!emailReg.test(value)) {
          callback(new Error('邮箱格式不正确'));
        } else {
          callback();
        }
      };

      return {
        activeTab: 'login',
        loginLoading: false,
        registerLoading: false,
        loginForm: {
          username: 'markerhub',
          password: '111111'
        },
        loginRules: {
          username: [
            { required: true, message: '请输入用户名', trigger: 'blur' },
            { min: 3, max: 15, message: '长度在 3 到 15 个字符', trigger: 'blur' }
          ],
          password: [
            { required: true, message: '请输入密码', trigger: 'blur' }
          ]
        },
        registerForm: {
          username: '',
          email: '',
          password: '',
          confirmPassword: ''
        },
        registerRules: {
          username: [
            { required: true, message: '请输入用户名', trigger: 'blur' },
            { min: 3, max: 15, message: '长度在 3 到 15 个字符', trigger: 'blur' }
          ],
          email: [
            { required: true, validator: validateEmail, trigger: 'blur' }
          ],
          password: [
            { required: true, message: '请输入密码', trigger: 'blur' },
            { min: 6, message: '密码长度至少为 6 个字符', trigger: 'blur' }
          ],
          confirmPassword: [
            { required: true, validator: validateConfirmPassword, trigger: 'blur' }
          ]
        }
      };
    },
    methods: {
      handleTabClick(tab) {
        // 切换tab时重置表单
        if (tab.name === 'login') {
          this.$refs.registerForm && this.$refs.registerForm.resetFields();
        } else {
          this.$refs.loginForm && this.$refs.loginForm.resetFields();
        }
      },
      submitLogin(formName) {
        this.$refs[formName].validate((valid) => {
          if (valid) {
            const _this = this
            _this.loginLoading = true
            this.$axios.post('/login', this.loginForm).then(res => {
              console.log(res.data)
              const jwt = res.headers['authorization']
              const userInfo = res.data.data

              // 把数据共享出去
              _this.$store.commit("SET_TOKEN", jwt)
              _this.$store.commit("SET_USERINFO", userInfo)

              // 获取
              console.log(_this.$store.getters.getUser)

              _this.$message.success('登录成功！')
              _this.loginLoading = false
              _this.$router.push("/blogs")
            }).catch(error => {
              console.error('登录失败:', error)
              _this.loginLoading = false
              if (error.response && error.response.data) {
                _this.$message.error(error.response.data.msg || '登录失败')
              } else {
                _this.$message.error('登录失败，请稍后重试')
              }
            })

          } else {
            console.log('error submit!!');
            return false;
          }
        });
      },
      submitRegister(formName) {
        this.$refs[formName].validate((valid) => {
          if (valid) {
            const _this = this
            _this.registerLoading = true
            const registerData = {
              username: this.registerForm.username,
              email: this.registerForm.email,
              password: this.registerForm.password
            }
            
            this.$axios.post('/register', registerData).then(res => {
              console.log(res.data)
              _this.$message.success('注册成功！请登录')
              _this.registerLoading = false
              // 切换到登录tab
              _this.activeTab = 'login'
              // 自动填充用户名和密码
              _this.loginForm.username = registerData.username
              _this.loginForm.password = registerData.password
            }).catch(error => {
              console.error('注册失败:', error)
              _this.registerLoading = false
              if (error.response && error.response.data) {
                _this.$message.error(error.response.data.msg || '注册失败')
              } else {
                _this.$message.error('注册失败，请稍后重试')
              }
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
    }
  }
</script>

<style scoped>
  .login-container {
    min-height: 100vh;
    background: #34495e;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40px 20px;
  }

  /* 主内容区域 */
  .login-content {
    width: 100%;
    max-width: 480px;
  }

  /* Logo 区域 */
  .logo-section {
    text-align: center;
    margin-bottom: 40px;
  }

  .logo-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
  }

  .logo-icon {
    width: 100px;
    height: 100px;
    object-fit: cover;
    border: 1px solid #2c3e50;
  }

  .logo-title {
    font-size: 36px;
    font-weight: bold;
    color: #ecf0f1;
    margin: 0;
  }

  .logo-subtitle {
    font-size: 16px;
    color: #bdc3c7;
    margin: 0;
  }

  /* 表单卡片 */
  .form-card {
    background: #2c3e50;
    border: 1px solid #34495e;
    margin-bottom: 30px;
  }

  /* Tabs 样式 */
  .auth-tabs {
    width: 100%;
  }

  .auth-tabs >>> .el-tabs__header {
    margin: 0;
    background: #34495e;
    padding: 0;
    border-bottom: 1px solid #2c3e50;
  }

  .auth-tabs >>> .el-tabs__nav-wrap {
    padding: 0;
    margin: 0;
  }

  .auth-tabs >>> .el-tabs__nav-wrap::after {
    display: none;
  }

  .auth-tabs >>> .el-tabs__nav {
    border: none;
    display: flex;
    width: 100%;
    margin: 0;
  }

  .auth-tabs >>> .el-tabs__item {
    color: #bdc3c7;
    border: none !important;
    font-size: 16px;
    font-weight: 500;
    padding: 20px 0;
    flex: 1;
    text-align: center;
    margin: 0;
    position: relative;
    height: auto;
    line-height: 1.5;
    border-right: 1px solid #2c3e50;
  }

  .auth-tabs >>> .el-tabs__item:last-child {
    border-right: none;
  }

  .auth-tabs >>> .el-tabs__item:hover {
    color: #ecf0f1;
    background: #3d566e;
  }

  .auth-tabs >>> .el-tabs__item.is-active {
    color: #3498db;
    background: #2c3e50;
    font-weight: 600;
  }

  .auth-tabs >>> .el-tabs__active-bar {
    background: #3498db;
    height: 2px;
    bottom: 0;
  }

  .auth-tabs >>> .el-tabs__nav-scroll {
    overflow: visible;
  }

  .auth-tabs >>> .el-tabs__content {
    padding: 0;
  }

  /* Tab 内容 */
  .tab-content {
    padding: 40px;
    background: #2c3e50;
  }

  .tab-header {
    text-align: center;
    margin-bottom: 32px;
  }

  .tab-icon {
    font-size: 48px;
    color: #3498db;
    margin-bottom: 16px;
    display: inline-block;
  }

  .tab-title {
    font-size: 28px;
    font-weight: 600;
    color: #ecf0f1;
    margin: 0 0 8px 0;
  }

  .tab-subtitle {
    font-size: 14px;
    color: #bdc3c7;
    margin: 0;
  }

  /* 表单样式 */
  .auth-form {
    margin-top: 32px;
  }

  .auth-form >>> .el-form-item__label {
    color: #bdc3c7;
    font-weight: 500;
    font-size: 14px;
    padding-bottom: 8px;
  }

  .form-input >>> .el-input__inner {
    height: 48px;
    border: 1px solid #34495e;
    background: #34495e;
    color: #ecf0f1;
    font-size: 15px;
    padding-left: 40px; /* 为图标留出空间，避免与输入内容重叠 */
  }

  .form-input >>> .el-input__inner:focus {
    border-color: #3498db;
    background: #3d566e;
  }

  .form-input >>> .el-input__prefix {
    left: 16px;
  }

  .form-input >>> .el-input__prefix .el-input__icon {
    color: #95a5a6;
  }

  .form-input >>> .el-input__inner::placeholder {
    color: #7f8c8d;
  }

  /* 提交按钮 */
  .submit-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    font-weight: 500;
    background: #3498db;
    border: 1px solid #2980b9;
    color: #ecf0f1;
    margin-top: 8px;
  }

  .submit-btn:hover {
    background: #2980b9;
  }

  .submit-btn:active {
    background: #21618c;
  }

  /* 表单底部 */
  .form-footer {
    text-align: center;
    margin-top: 20px;
  }

  .reset-link {
    color: #95a5a6;
    font-size: 14px;
    padding: 0;
  }

  .reset-link:hover {
    color: #3498db;
  }

  .reset-link i {
    margin-right: 4px;
  }

  /* 底部链接 */
  .footer-links {
    text-align: center;
  }

  .back-link {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #bdc3c7;
    text-decoration: none;
    font-size: 14px;
    padding: 10px 20px;
    background: #2c3e50;
    border: 1px solid #34495e;
  }

  .back-link:hover {
    color: #3498db;
    background: #34495e;
  }

  .back-link i {
    margin-right: 4px;
  }

  /* 响应式设计 */
  @media (max-width: 768px) {
    .login-container {
      padding: 20px 15px;
    }

    .logo-icon {
      width: 80px;
      height: 80px;
    }

    .logo-title {
      font-size: 28px;
    }

    .tab-content {
      padding: 30px 20px;
    }

    .tab-title {
      font-size: 24px;
    }

    .auth-tabs >>> .el-tabs__item {
      padding: 15px 20px;
      font-size: 14px;
    }
  }

  @media (max-width: 480px) {
    .logo-icon {
      width: 60px;
      height: 60px;
    }

    .logo-title {
      font-size: 24px;
    }

    .tab-content {
      padding: 24px 16px;
    }

    .tab-title {
      font-size: 20px;
    }

    .tab-icon {
      font-size: 40px;
    }
  }
</style>
