package com.furongsoft.ide.debugger.java;

import com.furongsoft.core.misc.Tracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 进程执行器
 *
 * @author Alex
 */
public class ProcessExecutor implements Runnable {
    private Thread thread;
    private Process process;
    private boolean runFlag;
    private String command;
    private int maxLines;
    private ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    /**
     * 启动进程
     *
     * @param command 命令
     * @param maxLines 最大输出行数
     * @return 进程执行器
     */
    public ProcessExecutor start(String command, int maxLines) {
        stop();

        synchronized (this) {
            if (thread != null) {
                return null;
            }

            this.command = command;
            this.maxLines = maxLines;

            runFlag = true;
            thread = new Thread(this);
            thread.start();
        }

        return this;
    }

    /**
     * 停止进程
     *
     * @return 是否成功
     */
    public boolean stop() {
        Thread thread;
        synchronized (this) {
            if (process != null) {
                process.destroy();
                process = null;
            }

            runFlag = false;
            thread = this.thread;
        }

        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Tracker.error(e);
                return false;
            }
        }

        synchronized (this) {
            this.thread = null;
        }

        return true;
    }

    /**
     * 获取输出内容
     *
     * @return 输出内容
     */
    public String[] getOutput() {
        return (String[]) queue.toArray();
    }

    @Override
    public void run() {
        Process process;
        try {
            process = Runtime.getRuntime().exec(command);
            synchronized (this) {
                this.process = process;
            }
        } catch (IOException e) {
            Tracker.error(e);
            return;
        }

        read(process);
        process.destroy();
        Tracker.info("=========== process exit");

        synchronized (this) {
            this.process = null;
        }
    }

    /**
     * 读取进程输出
     *
     * @param process 进程
     */
    private void read(final Process process) {
        read(process.getInputStream());
        read(process.getErrorStream());
    }

    /**
     * 读取进程输出
     *
     * @param inputStream 输入流
     */
    private void read(InputStream inputStream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while (runFlag && ((line = reader.readLine()) != null)) {
                queue.add(line);
                if (queue.size() > maxLines) {
                    queue.poll();
                }
            }
        } catch (IOException e) {
            Tracker.error(e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                Tracker.error(e);
            }
        }
    }
}