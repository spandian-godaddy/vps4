package com.godaddy.vps4.web.featureFlag;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigFeatureMaskInterceptorTest {
    private static class TestFeatureSetting implements FeatureSetting {
        @Override
        public Object handle(Object o) {
            return o;
        }
    }

    private static class TestClass {
        @ConfigFeatureMask(setting = TestFeatureSetting.class, disabled = false)
        public Object methodOne() {
            return null;
        }

        @ConfigFeatureMask(setting = TestFeatureSetting.class)
        public Object methodTwo() {
            return null;
        }
    }

    private Injector injector;
    private MethodInterceptor methodInterceptor;
    private MethodInvocation invocation = mock(MethodInvocation.class);
    private Object retObject = new Object();
    private TestFeatureSetting testFeatureSetting = new TestFeatureSetting();
    private TestFeatureSetting spyFeatureSetting = spy(testFeatureSetting);

    @Before
    public void setUp() throws Exception {
        injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
            bind(MethodInterceptor.class).to(ConfigFeatureMaskInterceptor.class);
            bind(TestFeatureSetting.class).toInstance(spyFeatureSetting);
            }
        });
        methodInterceptor = injector.getInstance(MethodInterceptor.class);

        try {
            when(invocation.proceed()).thenReturn(retObject);
        }
        catch (Throwable e) {

        }
    }

    @Test
    public void getsTheMethodAssociatedWithTheInvocation() throws Throwable {
        Method m = TestClass.class.getMethod("methodOne");
        when(invocation.getMethod()).thenReturn(m);
        methodInterceptor.invoke(invocation);

        verify(invocation, times(1)).getMethod();
    }

    @Test
    public void delegatesInitiallyToTheInvocationMethod() throws Throwable {
        Method m = TestClass.class.getMethod("methodOne");
        when(invocation.getMethod()).thenReturn(m);
        methodInterceptor.invoke(invocation);

        verify(invocation, times(1)).proceed();
    }

    @Test
    public void delegatesCallsToFeatureSettingIfFeatureMaskIsActive() throws Throwable {
        Method m = TestClass.class.getMethod("methodOne");
        when(invocation.getMethod()).thenReturn(m);
        methodInterceptor.invoke(invocation);

        verify(spyFeatureSetting, times(1)).handle(retObject);
    }

    @Test
    public void returnsValueOfFeatureSettingIfFeatureMaskIsActive() throws Throwable {
        Method m = TestClass.class.getMethod("methodOne");
        when(invocation.getMethod()).thenReturn(m);
        assertEquals(retObject, methodInterceptor.invoke(invocation));
    }

    @Test
    public void doesNotDelegateCallToFeatureSettingIfFeatureMaskDisabled() throws Throwable {
        Method m = TestClass.class.getMethod("methodTwo");
        when(invocation.getMethod()).thenReturn(m);

        assertEquals(retObject, methodInterceptor.invoke(invocation));
        verify(spyFeatureSetting, times(0)).handle(retObject);
    }
}