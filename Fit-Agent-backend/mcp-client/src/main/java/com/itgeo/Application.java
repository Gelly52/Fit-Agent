package com.itgeo;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author gzx
 * &#064;description:  应用入口
 * &#064;date  2024-05-20 10:00:00
 */
@MapperScan("com.itgeo.mapper")
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        //加载.env文件
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        //把.env文件中的变量添加到系统环境变量中
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(Application.class, args);
    }
}
