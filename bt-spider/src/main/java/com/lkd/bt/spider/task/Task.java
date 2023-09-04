package com.lkd.bt.spider.task;

// 定义抽象任务类，包含一个模板方法
public abstract class Task {
    protected String name;

    // 模板方法，定义了任务的执行流程
    public final void execute() {
        init();
        start();
        end();
    }

    // 初始化步骤，子类可以重写
    protected void init() {
        System.out.println("===================="+name+"开始初始化=======================");
    }

    // 执行具体的任务步骤，子类必须实现
    protected abstract void start();

    // 清理步骤，子类可以重写
    protected void end() {
        System.out.println("===================="+name+"执行结束=======================");
    }
}

