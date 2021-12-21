package com.community.controller;


import com.community.dto.AccessTokenDTO;
import com.community.model.User;
import com.community.provider.GithubProvider;
import com.community.provider.UFileResult;
import com.community.provider.UFileService;
import com.community.provider.dto.GithubUser;
import com.community.service.UserService;
import com.community.strategy.LoginUserInfo;
import com.community.strategy.UserStrategy;
import com.community.strategy.UserStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
public class AuthorizeController {

    @Autowired
    private UserStrategyFactory userStrategyFactory;

    @Autowired
    private GithubProvider githubProvider;

    @Autowired
    private UFileService uFileService;

    @Autowired
    private UserService userService;

    @GetMapping("/callback")
    public String callback(@RequestParam(name = "code") String code,
                           @RequestParam(name = "state") String state){
        AccessTokenDTO accessTokenDTO = new AccessTokenDTO();
        accessTokenDTO.setCode(code);
        accessTokenDTO.setClient_id("ec7693e0020c0c125e34");
        accessTokenDTO.setRedirect_uri("http://localhost:8887/callback");
        accessTokenDTO.setClient_secret("96775b535e282ba96e478ad4c4b7baad0309b0d9");
        accessTokenDTO.setState(state);

        String accessToken = githubProvider.getAccessToken(accessTokenDTO);
        GithubUser user = githubProvider.getUser(accessToken);
        System.out.println(user.getName());

        return "index";
    }

    @GetMapping("/callback/{type}")
    public String newCallback(@PathVariable(name = "type") String type,
                              @RequestParam(name = "code") String code,
                              @RequestParam(name = "state",required = false) String state,
                              HttpServletRequest request,
                              HttpServletResponse response
                              ){
        UserStrategy userStrategy = userStrategyFactory.getStrategy(type);
        LoginUserInfo loginUserInfo = userStrategy.getUser(code,state);
        if (loginUserInfo != null && loginUserInfo.getId() != null){
            User user = new User();
            String token = UUID.randomUUID().toString();
            user.setToken(token);
            user.setName(loginUserInfo.getName());
            user.setAccountId(String.valueOf(loginUserInfo.getId()));
            user.setType(type);
            UFileResult fileResult = null;
            try {
                fileResult = uFileService.upload(loginUserInfo.getAvatarUrl());
                user.setAvatarUrl(fileResult.getFileUrl());
            }catch (Exception e){
                user.setAvatarUrl(loginUserInfo.getAvatarUrl());
            }
            userService.createOrUpdate(user);
            Cookie cookie = new Cookie("token", token);
            cookie.setMaxAge(60 * 60 * 24 * 30 * 6);
            cookie.setPath("/");
            response.addCookie(cookie);
            return "redirect:/";
        }else {
//            log.error("callback get github error,{}", loginUserInfo);
            // 登录失败，重新登录
            return "redirect:/";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
                         HttpServletResponse response) {
        request.getSession().invalidate();
        Cookie cookie = new Cookie("token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/";
    }
}
