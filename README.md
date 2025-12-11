# 开发日志

## 准备

### 启动数据库

musql -u root -p

表blog_articles

| id     | user_id | title   | description | content  | created  |
| ------ | ------- | ------- | ----------- | -------- | -------- |
| bigint | bigint  | varchar | varchar     | longtext | datetime |
| 主键   | 用户id  | 标题    | 概述        | 正文     | 创建时间 |

表users

| id     | username | avatar  | email   | password | status | created  | last_login   | permission |
| ------ | -------- | ------- | ------- | -------- | ------ | -------- | ------------ | ---------- |
| bigint | varchar  | varchar | varchar | varchar  | int    | datetime | datetime     | varchar    |
| 主键   | 用户名   |         | 邮箱    | 密码     |        | 创建时间 | 上次登陆时间 | 权限       |



### 启动redis

启动服务器：redis-server

启动客户端：redis-cli








### 工具获取

* xshell 6 绿色破解版：关注公众号：JavaCat，回复 xshell 获取
* Navicat 11 简体中文版：关注公众号：JavaCat，回复 navicat 获取1

公众号二维码：

![JavaCat](//image-1300566513.cos.ap-guangzhou.myqcloud.com/upload/images/20201020/7fa16a1f957f4cfebe7be1f6675f6f36.png "JavaCat")

直接扫码回复对应关键字

**推荐阅读：**

[B站86K播放量，SpringBoot+Vue前后端分离完整入门教程！](https://mp.weixin.qq.com/s/jGEkHTf2X8l-wUenc-PpEw)

[分享一套SpringBoot开发博客系统源码，以及完整开发文档！速度保存！](https://mp.weixin.qq.com/s/jz6e977xP-OyaAKNjNca8w)

[Github上最值得学习的100个Java开源项目，涵盖各种技术栈！](https://mp.weixin.qq.com/s/N-U0TaEUXnBFfBsmt_OESQ)

[2020年最新的常问企业面试题大全以及答案](https://mp.weixin.qq.com/s/lR5LC5GnD2Gs59ecV5R0XA)
