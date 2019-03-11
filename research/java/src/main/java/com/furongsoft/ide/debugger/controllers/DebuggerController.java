package com.furongsoft.ide.debugger.controllers;

import com.furongsoft.core.entities.RestResponse;
import com.furongsoft.ide.debugger.core.IDebugger;
import com.furongsoft.ide.debugger.entities.Breakpoint;
import com.furongsoft.ide.debugger.entities.Information;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/debugger")
@CrossOrigin
public class DebuggerController {
    private final IDebugger debugger;

    @Autowired
    public DebuggerController(IDebugger debugger) {
        this.debugger = debugger;
    }

    @GetMapping("/analyze")
    public RestResponse analyze() {
        debugger.analyze();
        return new RestResponse(HttpStatus.OK);
    }

    @GetMapping("/declarationSymbols")
    public RestResponse getDeclarationSymbol(@RequestParam String sourcePath, @RequestParam int lineNumber, @RequestParam int columnNumber) {
        return new RestResponse(HttpStatus.OK, null, debugger.getDeclarationSymbol(sourcePath, lineNumber, columnNumber));
    }

    @GetMapping("/symbolValues")
    public RestResponse getSymbolValue(@RequestParam String sourcePath, @RequestParam int lineNumber, @RequestParam int columnNumber) {
        return new RestResponse(HttpStatus.OK, null, debugger.getSymbolValue(sourcePath, lineNumber, columnNumber));
    }

    @GetMapping("/evaluation")
    public RestResponse evaluation(@RequestParam String expression) {
        return new RestResponse(HttpStatus.OK, null, debugger.evaluation(expression));
    }

    @GetMapping("/codes")
    public RestResponse getCode(@RequestParam String sourcePath) {
        return new RestResponse(HttpStatus.OK, null, debugger.getCode(sourcePath));
    }

    @GetMapping("/information")
    public RestResponse getInformation() {
        return new RestResponse(HttpStatus.OK, null, new Information(
                debugger.getState(),
                debugger.getBreakpoints(),
                debugger.getLocation(),
                debugger.getStack(),
                debugger.getVariables()
        ));
    }

    @GetMapping("/state")
    public RestResponse getState() {
        return new RestResponse(HttpStatus.OK, null, debugger.getState());
    }

    @GetMapping("/location")
    public RestResponse getLocation() {
        return new RestResponse(HttpStatus.OK, null, debugger.getLocation());
    }

    @GetMapping("/stack")
    public RestResponse getStack() {
        return new RestResponse(HttpStatus.OK, null, debugger.getStack());
    }

    @GetMapping("/variables")
    public RestResponse getVariables() {
        return new RestResponse(HttpStatus.OK, null, debugger.getVariables());
    }

    @GetMapping("/breakpoints")
    public RestResponse getBreakpoints() {
        return new RestResponse(HttpStatus.OK, null, debugger.getBreakpoints());
    }

    @GetMapping("/console")
    public RestResponse getConsole() {
        return new RestResponse(HttpStatus.OK, null, debugger.getConsole());
    }

    @PostMapping("/breakpoints")
    public RestResponse addBreakpoint(@RequestBody Breakpoint breakpoint) {
        boolean ret = debugger.addBreakpoint(breakpoint);
        return new RestResponse(ret ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PostMapping("/breakpoints/delete")
    public RestResponse deleteBreakpoint(@RequestBody Breakpoint breakpoint) {
        boolean ret = debugger.deleteBreakpoint(breakpoint);
        return new RestResponse(ret ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/breakpoints")
    public RestResponse deleteAllBreakpoints() {
        boolean ret = debugger.deleteAllBreakpoint();
        return new RestResponse(ret ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/start")
    public RestResponse start(@RequestParam String script, @RequestParam String arguments) {
        boolean ret = debugger.start(script, arguments);
        return new RestResponse(ret ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/stop")
    public RestResponse stop() {
        boolean ret = debugger.stop();
        return new RestResponse(ret ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/suspend")
    public RestResponse suspend() {
        boolean ret = debugger.suspend();
        return new RestResponse(ret ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/resume")
    public RestResponse resume() {
        boolean ret = debugger.resume();
        return new RestResponse(ret ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/stepInto")
    public RestResponse stepInto() {
        boolean ret = debugger.stepInto();
        return new RestResponse(ret ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/stepOut")
    public RestResponse stepOut() {
        boolean ret = debugger.stepOut();
        return new RestResponse(ret ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/stepOver")
    public RestResponse stepOver() {
        boolean ret = debugger.stepOver();
        return new RestResponse(ret ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
