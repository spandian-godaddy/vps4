package com.godaddy.vps4.cpanel;

import org.junit.Assert;
import org.junit.Test;

public class CpanelAccessDeniedExceptionTest {
    @Test
    public void testCpanelAccessDeniedException() {
        String expectedMessage = "Test";
        CpanelAccessDeniedException targetException = new CpanelAccessDeniedException(expectedMessage);
        Assert.assertEquals(expectedMessage, targetException.getMessage());
    }

    @Test
    public void testCpanelAccessDeniedExceptionWithException() {
        String expectedMessage = "Test";
        ArithmeticException testException = new ArithmeticException();
        CpanelAccessDeniedException targetException = new CpanelAccessDeniedException(expectedMessage, testException);

        Assert.assertEquals(expectedMessage, targetException.getMessage());
        Assert.assertTrue(targetException.getCause() instanceof ArithmeticException);
    }
}
