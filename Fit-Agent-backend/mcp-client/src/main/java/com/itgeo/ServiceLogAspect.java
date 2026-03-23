package com.itgeo;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Component
@Slf4j
@Aspect
public class ServiceLogAspect {

    /* AOP 环绕切面
     *      * 返回任意类型
     *      com.itgeo.service.impl 指定的包名，要切的class类的所在包
     *      .. 可以匹配到当前包和子包中的类
     *      * 匹配当钱包以及子包下的class类
     *      . 无意义
     *      * 匹配任意方法名
     *      (..) 方法的参数，匹配任意参数
     * 记录服务方法执行时间
     * */
    @Around("execution(* com.itgeo.service.impl..*.*(..))")
    public Object recordTimeLog(ProceedingJoinPoint joinPoint) throws Throwable {
//        long begin = System.currentTimeMillis();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Object proceed = joinPoint.proceed();
        String point = joinPoint.getTarget().getClass().getName()
                + "."
                + joinPoint.getSignature().getName();
        stopWatch.stop();

//        long end = System.currentTimeMillis();
//        long takeTime = end - begin;

        long takeTime = stopWatch.getTotalTimeMillis();

        if(takeTime > 100000) {
            log.error("{} 耗时偏长: {}ms", point, takeTime);
        } else if(takeTime > 10000) {
            log.warn("{} 执行耗时较长: {}ms", point, takeTime);
        }
        else {
            log.info("{} 执行时间: {}ms", point, takeTime);
        }
        return proceed;
    }

}
