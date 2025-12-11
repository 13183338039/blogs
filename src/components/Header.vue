<template>
  <div class="header-container">
    <div class="header-content">
      <!-- Logo 和标题区域 -->
      <div class="header-left">
        <router-link to="/blogs" class="logo-link">
          <div class="logo">
            <img src="/logo.jpg" alt="Logo" class="logo-icon">
            <span class="logo-text">Zhang Shuhao's Blog</span>
          </div>
        </router-link>
        <h1 class="site-title">博客平台</h1>
      </div>

      <!-- 导航菜单 -->
      <div class="header-center">
        <nav class="nav-menu">
          <router-link to="/blogs" class="nav-item" active-class="active">
            <i class="el-icon-house nav-icon"></i>
            <span>首页</span>
          </router-link>
          <router-link 
            v-if="hasLogin" 
            to="/blog/add" 
            class="nav-item" 
            active-class="active"
          >
            <i class="el-icon-edit nav-icon"></i>
            <span>写博客</span>
          </router-link>
        </nav>
      </div>

      <!-- 用户信息区域 -->
      <div class="header-right">
        <div v-if="hasLogin" class="user-info">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-profile">
              <el-avatar :size="40" :src="user.avatar" class="user-avatar">
                <img src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" alt="默认头像"/>
              </el-avatar>
              <span class="username">{{ user.username }}</span>
              <i class="el-icon-arrow-down dropdown-icon"></i>
            </div>
            <el-dropdown-menu slot="dropdown" class="user-dropdown">
              <el-dropdown-item command="profile">
                个人中心
              </el-dropdown-item>
              <el-dropdown-item command="logout" >
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <div v-else class="auth-buttons">
          <router-link to="/login" class="login-btn">
            <i class="el-icon-user"></i>
            登录
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: "Header",
    data() {
      return {
        user: {
          username: '请先登录',
          avatar: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
        },
        hasLogin: false
      }
    },
    methods: {
      logout() {
        const _this = this
        _this.$confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          _this.$axios.get("/logout").then(res => {
            _this.$store.commit("REMOVE_INFO")
            _this.$message.success('已退出登录')
            _this.$router.push("/login")
          }).catch(error => {
            console.error('退出失败:', error)
            _this.$store.commit("REMOVE_INFO")
            _this.$router.push("/login")
          })
        }).catch(() => {
          // 用户取消
        })
      },
      handleCommand(command) {
        if (command === 'logout') {
          this.logout()
        } else if (command === 'profile') {
          this.$message.info('个人中心功能开发中...')
        }
      }
    },
    created() {
      if(this.$store.getters.getUser.username) {
        this.user.username = this.$store.getters.getUser.username
        this.user.avatar = this.$store.getters.getUser.avatar
        this.hasLogin = true
      }
    },
    watch: {
      '$store.getters.getUser': {
        handler(newVal) {
          if(newVal && newVal.username) {
            this.user.username = newVal.username
            this.user.avatar = newVal.avatar
            this.hasLogin = true
          } else {
            this.hasLogin = false
            this.user.username = '请先登录'
            this.user.avatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
          }
        },
        deep: true,
        immediate: true
      }
    }
  }
</script>

<style scoped>
  .header-container {
    background: #2c3e50;
    border-bottom: 1px solid #34495e;
    position: sticky;
    top: 0;
    z-index: 1000;
  }

  .header-content {
    width: 100%;
    margin: 0 auto;
    padding: 0 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 70px;
  }

  /* 左侧 Logo 区域 */
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    flex: 0 0 auto;
  }

  .logo-link {
    text-decoration: none;
    display: flex;
    align-items: center;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .logo-icon {
    width: 40px;
    height: 40px;
    object-fit: cover;
  }

  .logo-text {
    font-size: 24px;
    font-weight: bold;
    color: #ecf0f1;
    letter-spacing: 1px;
  }

  .site-title {
    font-size: 18px;
    font-weight: 400;
    color: #bdc3c7;
    margin: 0;
    padding-left: 16px;
    border-left: 1px solid #34495e;
  }

  /* 中间导航区域 */
  .header-center {
    flex: 1;
    display: flex;
    justify-content: center;
  }

  .nav-menu {
    display: flex;
    gap: 0;
    align-items: center;
  }

  .nav-item {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 20px;
    color: #bdc3c7;
    text-decoration: none;
    font-size: 15px;
    font-weight: 500;
    position: relative;
    border-right: 1px solid #34495e;
  }

  .nav-item:last-child {
    border-right: none;
  }

  .nav-item:hover {
    background: #34495e;
    color: #ecf0f1;
  }

  .nav-item.active {
    background: #34495e;
    color: #3498db;
  }

  .nav-icon {
    font-size: 18px;
  }

  /* 右侧用户区域 */
  .header-right {
    flex: 0 0 auto;
    display: flex;
    align-items: center;
  }

  .user-info {
    display: flex;
    align-items: center;
  }

  .user-profile {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 16px;
    background: #34495e;
    cursor: pointer;
    border-left: 1px solid #2c3e50;
  }

  .user-profile:hover {
    background: #3d566e;
  }

  .user-avatar {
    border: 1px solid #34495e;
  }

  .username {
    color: #ecf0f1;
    font-size: 15px;
    font-weight: 500;
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .dropdown-icon {
    color: #bdc3c7;
    font-size: 12px;
  }

  /* 下拉菜单样式 */
  .user-dropdown >>> .el-dropdown-menu {
    background: #2c3e50;
    border: 1px solid #34495e;
    min-width: 160px;
    padding: 4px 0;
  }

  .user-dropdown >>> .el-dropdown-menu__item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 24px;
    color: #3498db;
    white-space: nowrap;
    font-size: 14px;
    font-weight: 500;
  }

  .user-dropdown >>> .el-dropdown-menu__item:hover {
    background: #34495e;
    color: #5dade2;
  }

  .user-dropdown >>> .el-dropdown-menu__item i {
    font-size: 16px;
    color: #ffffff;
    flex-shrink: 0;
    min-width: 18px;
    text-align: left;
    display: inline-block;
  }

  .user-dropdown >>> .el-dropdown-menu__item:hover i {
    color: #3498db;
  }

  .user-dropdown >>> .el-dropdown-menu__item span {
    flex: 1;
    min-width: 0;
    line-height: 1.5;
  }

  .user-dropdown >>> .el-dropdown-menu__item.is-divided {
    border-top: 1px solid #34495e;
  }

  /* 登录按钮 */
  .auth-buttons {
    display: flex;
    align-items: center;
    gap: 0;
  }

  .login-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 24px;
    background: #34495e;
    color: #ecf0f1;
    text-decoration: none;
    font-size: 15px;
    font-weight: 500;
    border-left: 1px solid #2c3e50;
  }

  .login-btn:hover {
    background: #3d566e;
    color: #fff;
  }

  .login-btn i {
    font-size: 16px;
  }

  /* 响应式设计 */
  @media (max-width: 768px) {
    .header-content {
      height: auto;
      padding: 15px;
      flex-wrap: wrap;
    }

    .header-left {
      width: 100%;
      justify-content: center;
      margin-bottom: 15px;
    }

    .site-title {
      display: none;
    }

    .header-center {
      width: 100%;
      order: 3;
      margin-top: 15px;
    }

    .nav-menu {
      width: 100%;
      justify-content: center;
    }

    .nav-item {
      flex: 1;
      justify-content: center;
      padding: 10px;
      border-right: 1px solid #34495e;
      border-bottom: 1px solid #34495e;
    }

    .nav-item:last-child {
      border-right: 1px solid #34495e;
    }

    .header-right {
      width: 100%;
      justify-content: center;
      order: 2;
    }

    .username {
      max-width: 100px;
    }
  }

  @media (max-width: 480px) {
    .logo-text {
      font-size: 20px;
    }

    .logo-icon {
      width: 32px;
      height: 32px;
    }

    .nav-item span {
      display: none;
    }

    .nav-item {
      padding: 12px;
    }

    .username {
      display: none;
    }
  }
</style>
