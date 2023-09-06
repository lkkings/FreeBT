import java.util.concurrent.Semaphore;

public class ExclusiveExecution {
    private static final Semaphore semaphore = new Semaphore(1);

    public void exclusiveFunction() {
        try {
            semaphore.acquire(); // 尝试获取许可，只有一个线程能够成功获取
            // 这里放置你希望只被一个线程执行的代码
            System.out.println("Function is executed by thread: " + Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 释放许可，允许其他线程获取
        }
    }

    public static void main(String[] args) {
        ExclusiveExecution executor = new ExclusiveExecution();

        // 创建三个相同的线程
        Thread thread1 = new Thread(() -> {
            executor.exclusiveFunction();
        });

        Thread thread2 = new Thread(() -> {
            executor.exclusiveFunction();
        });

        Thread thread3 = new Thread(() -> {
            executor.exclusiveFunction();
        });

        // 启动线程
        thread1.start();
        thread2.start();
        thread3.start();
    }
}
