package com.markerhub.controller;

import cn.hutool.core.map.MapUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.markerhub.common.dto.LoginDto;
import com.markerhub.common.dto.RegisterDto;
import com.markerhub.common.lang.Result;
import com.markerhub.entity.User;
import com.markerhub.service.UserService;
import com.markerhub.util.JwtUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@Slf4j
@RestController
public class AccountController {

    @Autowired
    UserService userService;

    @Autowired
    JwtUtils jwtUtils;

    @GetMapping("/test")
    public String func1(){
        return "hello world;";
    }

    @PostMapping("/login")
    public Result login(@Validated @RequestBody LoginDto loginDto, HttpServletResponse response) {
        log.info("用户登录请求 - 用户名: {}", loginDto.getUsername());

        User user = userService.getOne(new QueryWrapper<User>().eq("username", loginDto.getUsername()));//查询密码
        Assert.notNull(user, "用户不存在");

        if(!user.getPassword().equals(SecureUtil.md5(loginDto.getPassword()))){//比较验证密码
            log.warn("用户登录失败 - 用户名: {}, 原因: 密码不正确", loginDto.getUsername());
            return Result.fail("密码不正确");
        }
        
        String jwt = jwtUtils.generateToken(user.getId());//前端保存Token，后续请求携带在请求头
        log.info("用户登录成功 - 用户名: {}, 用户ID: {}", user.getUsername(), user.getId());

        response.setHeader("Authorization", jwt);
        response.setHeader("Access-control-Expose-Headers", "Authorization");

        return Result.succ(MapUtil.builder()
                .put("id", user.getId())
                .put("username", user.getUsername())
                .put("avatar", user.getAvatar())
                .put("email", user.getEmail())
                .map()
        );
    }

    @PostMapping("/register")
    public Result register(@Validated @RequestBody RegisterDto registerDto) {
        log.info("用户注册请求 - 用户名: {}, 邮箱: {}", registerDto.getUsername(), registerDto.getEmail());

        // 检查用户名是否已存在
        User existUser = userService.getOne(new QueryWrapper<User>().eq("username", registerDto.getUsername()));
        if (existUser != null) {
            log.warn("用户注册失败 - 用户名: {}, 原因: 用户名已存在", registerDto.getUsername());
            return Result.fail("用户名已存在");
        }

        // 检查邮箱是否已存在
        existUser = userService.getOne(new QueryWrapper<User>().eq("email", registerDto.getEmail()));
        if (existUser != null) {
            log.warn("用户注册失败 - 邮箱: {}, 原因: 邮箱已被注册", registerDto.getEmail());
            return Result.fail("邮箱已被注册");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setPassword(SecureUtil.md5(registerDto.getPassword())); // MD5加密密码
        user.setEmail(registerDto.getEmail());
        user.setStatus(0); // 0表示正常状态
        user.setCreated(LocalDateTime.now());
        user.setAvatar("https://image-1300566513.cos.ap-guangzhou.myqcloud.com/upload/images/5a9f48118166308daba8b6da7e466aab.jpg"); // 默认头像

        userService.save(user);
        log.info("用户注册成功 - 用户名: {}, 用户ID: {}", user.getUsername(), user.getId());

        return Result.succ(MapUtil.builder()
                .put("id", user.getId())
                .put("username", user.getUsername())
                .put("email", user.getEmail())
                .map()
        );
    }

    @RequiresAuthentication
    @GetMapping("/logout")
    public Result logout() {
        SecurityUtils.getSubject().logout();
        return Result.succ(null);
    }

}
