import java.util.concurrent.LinkedBlockingDeque;

public class LinkedBlockingDequeExample {

    public static void main(String[] args) throws InterruptedException {
        final LinkedBlockingDeque<Integer> deque = new LinkedBlockingDeque<>(3);

        // 插入元素到队列头部和尾部
        deque.offerFirst(1);
        deque.offerLast(2);
        deque.offerLast(3);

        // 队列已满，插入操作会被阻塞
        // deque.offerLast(4); // 阻塞等待队列有空间

        // 获取并移除队列头部元素
        int firstElement = deque.pollFirst();
        System.out.println("First Element: " + firstElement);

        // 获取并移除队列尾部元素
        int lastElement = deque.pollLast();
        System.out.println("Last Element: " + lastElement);

        // 获取队列中的元素个数
        int size = deque.size();
        System.out.println("Queue Size: " + size);

        // 使用迭代器遍历队列
        for (Integer element : deque) {
            System.out.println("Element: " + element);
        }
    }
}
