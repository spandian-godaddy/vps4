package com.godaddy.vps4.phase3;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TestGroupExecution {

    final TestGroup testGroup;

    final List<TestExecution> tests;

    final Deque<TestGroupExecution> children = new ConcurrentLinkedDeque<>();

    volatile Future<?> future;

    volatile TestStatus status = TestStatus.PENDING;

    public TestGroupExecution(TestGroup testGroup, List<TestExecution> tests) {
        this.testGroup = testGroup;
        this.tests = tests;
    }

    public void await() throws InterruptedException {

        try {
            future.get();
        } catch (ExecutionException e) {
//            System.err.println("FAIL: " + testGroup + " " + e);
//            this.status = TestStatus.FAIL;
            throw new RuntimeException(e);
            
        }

        for (TestExecution testExecution : tests) {
            testExecution.await();
        }

        for (TestGroupExecution childExecution : this.children) {
            childExecution.await();
        }
    }
    
    public void printResults(){
        printResults("");
    }
    
    public void printResults(String prefix){
        System.out.println(prefix + "Test Group " + testGroup.name + " results:");
        if(!children.isEmpty()){
            System.out.println(prefix + testGroup.name + " children:");
            for(TestGroupExecution group : children){
                group.printResults(prefix+"--");
            }
        }
        for (TestExecution test : tests){
            System.out.println(prefix + "--" + test);
        }
        System.out.println(prefix + "Test Group " + testGroup.name + " combined result:" + status);
    }

    @Override
    public String toString() {
        return "TestGroupExecution [testGroup=" + testGroup + ", tests=" + tests + ", children=" + children + ", status=" + status + "]";
    }
}
