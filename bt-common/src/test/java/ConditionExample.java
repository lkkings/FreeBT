import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConditionExample {

    private static final Lock lock = new ReentrantLock();
    private static final Condition condition = lock.newCondition();
    private static boolean conditionMet = false;

    public static void main(String[] args) {
        new Thread(ConditionExample::awaitCondition).start();
        new Thread(ConditionExample::signalCondition).start();
    }

    public static void awaitCondition() {
        lock.lock();
        try {
            while (!conditionMet) {
                System.out.println(Thread.currentThread().getName() + " is waiting.");
                condition.await();
            }
            System.out.println(Thread.currentThread().getName() + " is done waiting.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static void signalCondition() {
        lock.lock();
        try {
            Thread.sleep(2000); // 模拟一些操作
            conditionMet = true;
            System.out.println(Thread.currentThread().getName() + " signals the condition.");
            condition.signal();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
