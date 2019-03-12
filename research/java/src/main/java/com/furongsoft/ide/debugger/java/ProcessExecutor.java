package com.furongsoft.ide.debugger.java;

import com.furongsoft.core.misc.Tracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

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
    private List<String> queue = new LinkedList<>();

    /**
     * 启动进程
     *
     * @param command  命令
     * @param queue    输出队列
     * @param maxLines 最大输出行数
     * @return 进程执行器
     */
    public ProcessExecutor start(String command, List<String> queue, int maxLines) {
        stop();

        synchronized (this) {
            if (thread != null) {
                return null;
            }

            this.command = command;
            this.queue = queue;
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
     * 等待进程结束
     */
    public void join() {
        Thread thread;
        synchronized (this) {
            thread = this.thread;
        }

        if (thread != null) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Tracker.error(e);
            }
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
                synchronized (queue) {
                    queue.add(line);
                    if (queue.size() > maxLines) {
                        queue.remove(0);
                    }
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