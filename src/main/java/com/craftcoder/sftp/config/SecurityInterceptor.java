package com.craftcoder.sftp.config;

import com.craftcoder.sftp.util.FileUtils;
import com.craftcoder.sftp.util.HttpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class SecurityInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o) throws Exception {
        String requestIp=HttpUtils.getIpAddress(httpServletRequest);
        String path = SecurityInterceptor.class.getResource("/").getFile();
        //获取允许访问的ip
        List<String> whiteUrls=FileUtils.readFile(path+"whiteIps");
        if(whiteUrls.size()==0){
            throw new Exception("请设置【whiteIps】配置文件访问IP白名单！");
        }
        if(whiteUrls.contains(requestIp)){
            return true;
        }else{
            throw new Exception("请设置【whiteIps】配置文件访问IP白名单！");
        }
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }
}