package com.furongsoft.ide.debugger.controllers;

import com.furongsoft.ide.debugger.entities.Breakpoint;
import com.furongsoft.ide.debugger.java.JavaDebugger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/debugger")
public class DebuggerController {
    @RequestMapping("/start")
    public void start() {
        JavaDebugger debugger = new JavaDebugger();
        debugger.addBreakpoint(new Breakpoint("Test", 14, true));
        debugger.start("Test", "{\"-classpath\": \"D:/programs/web/web-ide/research/java/demos/demo1\"}");
    }
}
