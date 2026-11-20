package de.swiftbyte.gmc.backend.security;

import de.swiftbyte.gmc.backend.domain.model.ApiUser;
import de.swiftbyte.gmc.backend.exception.ForbiddenException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class ApiAccountAllowedAspect {

    // Intercept all controller methods under our controller package hierarchy
    @Before("execution(* de.swiftbyte.gmc.backend.controller..*(..))")
    public void enforceApiAccountPolicy(JoinPoint joinPoint) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;
        Object principal = auth.getPrincipal();
        if (!(principal instanceof ApiUser)) return; // only restrict API users

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> declaringClass = method.getDeclaringClass();

        boolean allowedOnMethod = method.isAnnotationPresent(ApiAccountAllowed.class);
        boolean allowedOnClass = declaringClass.isAnnotationPresent(ApiAccountAllowed.class);

        if (!allowedOnMethod && !allowedOnClass) {
            throw new ForbiddenException("api_account_not_allowed");
        }
    }
}
