package com.qiyuey.proxy;

import net.sf.cglib.core.DebuggingClassWriter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class DynamicProxyPerformanceTest {

    private static NopService jdkProxy;
    private static NopService cglibProxy;

    static {
        System.setProperty(DebuggingClassWriter.DEBUG_LOCATION_PROPERTY, "./");
        System.setProperty("sun.misc.ProxyGenerator.saveGeneratedFiles", "true");
        init();
    }

    private static void init() {

        NopService nopService = new NopServiceImpl();

        jdkProxy = createJdkDynamicProxy(nopService);
        cglibProxy = createCglibDynamicProxy(nopService);

    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(DynamicProxyPerformanceTest.class.getSimpleName())
                .forks(1)
                .warmupIterations(0)
                .measurementIterations(1)
                .build();
        new Runner(opt).run();
    }

    private static NopService createJdkDynamicProxy(
            final NopService delegate) {
        return (NopService) Proxy
                .newProxyInstance(ClassLoader.getSystemClassLoader(),
                        new Class[]{NopService.class},
                        new JdkHandler(delegate));
    }

    private static NopService createCglibDynamicProxy(
            final NopService delegate) {
        Enhancer enhancer = new Enhancer();
        enhancer.setCallback(new CglibInterceptor(delegate));
        enhancer.setSuperclass(NopService.class);
        return (NopService) enhancer.create();
    }

    @Benchmark
    public void jdkProxy() {
        jdkProxy.m();
    }

    @Benchmark
    public void cglibProxy() {
        cglibProxy.m();
    }

    private static class JdkHandler implements InvocationHandler {

        final Object delegate;

        JdkHandler(Object delegate) {
            this.delegate = delegate;
        }

        public Object invoke(Object object, Method method, Object[] objects)
                throws Throwable {
            return method.invoke(delegate, objects);
        }
    }

    private static class CglibInterceptor implements MethodInterceptor {

        final Object delegate;

        CglibInterceptor(Object delegate) {
            this.delegate = delegate;
        }

        public Object intercept(Object object, Method method, Object[] objects,
                                MethodProxy methodProxy) throws Throwable {
            return methodProxy.invoke(delegate, objects);
        }
    }
}
