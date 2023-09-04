import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
/**
 * Created by lkkings on 2023/8/24
 */

public class ExtendedTestRunner extends BlockJUnit4ClassRunner {

    public ExtendedTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected Statement methodInvoker(FrameworkMethod method, Object test) {
        if (method.getAnnotation(ExtendedTest.class) != null) {
            return new ExtendedTestStatement(super.methodInvoker(method, test),method);
        }
        return super.methodInvoker(method, test);
    }

    private static class ExtendedTestStatement extends Statement {
        private final Statement originalStatement;

        private final FrameworkMethod method;

        public ExtendedTestStatement(Statement originalStatement,FrameworkMethod method) {
            this.originalStatement = originalStatement;
            this.method = method;
        }

        @Override
        public void evaluate() throws Throwable {
            System.out.println("执行测试方法 >>> " + method.getName());
            System.out.print("执行输出: ");
            long startTime = System.nanoTime();
            originalStatement.evaluate();
            System.out.println("执行花费时间为: " + (System.nanoTime() - startTime)/ 1_000_000_000.0 + "s");
        }
    }
}
