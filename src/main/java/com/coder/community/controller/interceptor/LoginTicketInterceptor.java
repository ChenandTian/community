package com.coder.community.controller.interceptor;

import com.coder.community.entity.LoginTicket;
import com.coder.community.entity.User;
import com.coder.community.service.UserService;
import com.coder.community.util.CommunityUtil;
import com.coder.community.util.CookieUtil;
import com.coder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //通过cookie得到ticket(从cookie中获取凭证)
        String ticket = CookieUtil.getValue(request, "ticket");

        if(ticket != null){
            //查询凭证
           LoginTicket loginTicket =  userService.findLoginTicket(ticket);
            System.out.println("我是loginTicket"+ loginTicket);

           //检查凭证是否有效
            if(loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())){
                //根据凭证查询用户
                User user  = userService.findUserById(loginTicket.getUserId());
                //在本次请求中存有User（持有用户）
                //在多线程中隔离存多个对象,存到了当前线程的map中
                hostHolder.setUsers(user);
                //构建用户认证的结果，并存入到SecurityContext,以便于Security进行授权
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

            }

        }
        return true;
    }

    //在模板引擎之前将User存在Model中

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
       User user =  hostHolder.getUser();
       if(user != null && modelAndView != null){
           modelAndView.addObject("loginUser", user);
       }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
