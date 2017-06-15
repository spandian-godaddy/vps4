package com.godaddy.vps4.cpanel;

import org.junit.Assert;
import org.junit.Test;

public class CpanelTimeoutExceptionTest {
    @Test
    public void testCpanelTimeoutException() {
        String expectedMessage = "Test";
        CpanelTimeoutException targetException = new CpanelTimeoutException(expectedMessage);
        Assert.assertEquals(expectedMessage, targetException.getMessage());
    }

    @Test
    public void testCpanelTimeoutExceptionWithException() {
        String expectedMessage = "Test";
        ArithmeticException testException = new ArithmeticException();
        CpanelTimeoutException targetException = new CpanelTimeoutException(expectedMessage, testException);

        Assert.assertEquals(expectedMessage, targetException.getMessage());
        Assert.assertTrue(targetException.getCause() instanceof ArithmeticException);
    }
}
