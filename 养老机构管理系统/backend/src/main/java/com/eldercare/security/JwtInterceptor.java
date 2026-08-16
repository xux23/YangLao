package com.eldercare.security;

import com.eldercare.common.BusinessException;
import com.eldercare.entity.SysUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * JWT 登录拦截器：
 * 1. 校验请求头中的令牌是否有效；
 * 2. 把用户信息放入 UserContext；
 * 3. 根据方法上的 @RequireRole 注解做角色权限校验。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 跨域预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 非 Controller 方法（如静态资源）直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        // 1. 取出并解析令牌
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或令牌已过期");
        }
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token.substring(7));
        } catch (Exception e) {
            throw new BusinessException(401, "未登录或令牌已过期");
        }

        // 2. 用户信息放入上下文
        SysUser user = new SysUser();
        user.setId(((Number) claims.get("userId")).longValue());
        user.setUsername(claims.getSubject());
        user.setRole((String) claims.get("role"));
        UserContext.set(user);

        // 3. 角色权限校验
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        if (requireRole != null) {
            String role = user.getRole();
            if (!Arrays.asList(requireRole.value()).contains(role)) {
                throw new BusinessException(403, "无权限访问该接口");
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束清理上下文，防止线程复用导致数据串号
        UserContext.clear();
    }
}