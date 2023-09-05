import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import com.clearspring.analytics.stream.cardinality.HyperLogLog;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Shorts;
import com.lkd.bt.common.util.CodeUtil;
import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by lkkings on 2023/8/24
 */
@RunWith(ExtendedTestRunner.class)
public class UtilsTest{
    // 创建 HyperLogLog 过滤器
    HyperLogLog hyperLogLog = new HyperLogLog(0.05); // 误差率为 5%

    @ExtendedTest
    @Test
    public void generateNodeId() {
        Random secureRandom = new Random();
        byte[] randomBytes = new byte[20];
        secureRandom.nextBytes(randomBytes);
        System.out.println(Arrays.toString(randomBytes));
    }

    @ExtendedTest
    @Test
    public void stringToMd5() {
        String md5 = CodeUtil.stringToMd5("r345335353522224");

        System.out.println(md5.length());
    }

    @Test
    @ExtendedTest
    public void print(){
        System.out.println(Arrays.toString(":".getBytes(StandardCharsets.ISO_8859_1)));
    }

    @Test
    @ExtendedTest
    public void hll(){
        // 添加元素
        hyperLogLog.offer("element1");
        hyperLogLog.offer("element2");
        System.out.println(hyperLogLog.offer("element3"));
        System.out.println(hyperLogLog.offer("element1"));
    }

    @Test
    public void deque() throws InterruptedException {
        LinkedBlockingDeque<Object> objects = new LinkedBlockingDeque<>(10);
        objects.take();
    }
}
