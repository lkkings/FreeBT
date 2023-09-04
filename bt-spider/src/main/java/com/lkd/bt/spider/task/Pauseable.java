package com.lkd.bt.spider.task;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by lkkings on 2023/8/24
 */
public interface Pauseable {
	/**
	 * 传入lock 和 condition 暂停指定时间
	 * @param lock 锁
	 * @param condition 该锁创建的condition
	 * @param time 暂停时间
	 * @param timeUnit 时间单位
	 */
	default void pause(ReentrantLock lock, Condition condition, long time, TimeUnit timeUnit){
		if(time <= 0)
			return;
		try {
			lock.lock();
			condition.await(time, timeUnit);
		} catch (Exception e) {
			//..不可能发生
		} finally {
			lock.unlock();
		}
	}
}
