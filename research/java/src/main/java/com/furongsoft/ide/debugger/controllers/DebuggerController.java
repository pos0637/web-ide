package com.furongsoft.ide.debugger.controllers;

import com.furongsoft.ide.debugger.entities.Breakpoint;
import com.furongsoft.ide.debugger.java.JavaDebugger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/debugger")
public class DebuggerController {
    private JavaDebugger debugger = new JavaDebugger();

    @RequestMapping("/start")
    public void start() {
        debugger.addBreakpoint(new Breakpoint("Test", 9, true));
        debugger.start("Test", "{\"-classpath\": \"./demos/demo1\"}");
    }

    @RequestMapping("/stop")
    public void stop() {
        debugger.stop();
    }

    @RequestMapping("/suspend")
    public void suspend() {
        debugger.suspend();
    }

    @RequestMapping("/resume")
    public void resume() {
        debugger.resume();
    }

    @RequestMapping("/stepinto")
    public void stepInto() {
        debugger.stepInto();
    }

    @RequestMapping("/stepout")
    public void stepOut() {
        debugger.stepOut();
    }

    @RequestMapping("/stepover")
    public void stepOver() {
        debugger.stepOver();
    }
}
