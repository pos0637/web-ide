package com.furongsoft;

import com.furongsoft.ide.debugger.java.JavaDebugger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IdeApplication {

    public static void main(String[] args) {
        JavaDebugger debugger = new JavaDebugger();
        debugger.run();
        SpringApplication.run(IdeApplication.class, args);
    }
}
