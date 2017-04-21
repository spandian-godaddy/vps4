package com.godaddy.vps4.phase3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.godaddy.vps4.phase3.virtualmachine.VirtualMachinePool;

/**
 *
 * - tests within a test group can be run in parallel
 * - child TestGroups (in {@link #children}) are run after all tests
 *   in the parent TestGroup pass
 *
 */
public class TestGroup {

    private static final Logger logger = LoggerFactory.getLogger(TestGroup.class);

    protected final List<TestGroup> children = new ArrayList<>();

    protected final String name;

    public TestGroup(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void add(TestGroup group) {
        children.add(group);
    }

    public void addGroups(Collection<TestGroup> groups) {
        children.addAll(groups);
    }

    /**
     * submit all relevant work to the thread pool,
     * return an execution reference associated with that work
     *
     * No test execution-related exceptions should be thrown here,
     * rather they should be caught and associated with the returned TestGroupException
     *
     * @param threadPool
     * @param vmPool
     */
    public final TestGroupExecution execute(ExecutorService threadPool, VirtualMachinePool vmPool) {

        List<TestExecution> testExecutions = executeTests(threadPool, vmPool);

        TestGroupExecution groupExecution = new TestGroupExecution(this, testExecutions);

        groupExecution.status = TestStatus.RUNNING;

        // submit tests for execution
        groupExecution.future = threadPool.submit(() -> {

            // wait for the tests to complete
            try {
                awaitTests(groupExecution);

                // if all tests passed, execute children
                if (groupExecution.status == TestStatus.RUNNING) {

                    for (TestGroup group : this.children) {
                        groupExecution.children.add( group.execute(threadPool, vmPool) );
                    }

                    for (TestGroupExecution childExecution : groupExecution.children) {
                        childExecution.await();
                        if (childExecution.status == TestStatus.FAIL) {
                            groupExecution.status = TestStatus.FAIL;
                        }
                    }

                } else {
                   logger.warn("test failed, don't run children " + groupExecution.status + " " + this);
                }

                if (groupExecution.status == TestStatus.RUNNING) {
                    // if we've reached the end of execution
                    // and we're still in 'running' status,
                    // we have passed (by virtue of not failing)
                    groupExecution.status = TestStatus.PASS;
                }
                logger.debug("test group execution done: " + this);
            } catch (InterruptedException e) {
                groupExecution.status = TestStatus.FAIL;
                throw new RuntimeException(e);
            }

        });

        return groupExecution;
    }

    protected void awaitTests(TestGroupExecution groupExecution) throws InterruptedException {

        for (TestExecution testExecution : groupExecution.tests) {

            try {
                testExecution.future.get();
                testExecution.status = TestStatus.PASS;
                System.out.println("PASS: " + testExecution);
            }
            catch (ExecutionException e) {
                testExecution.exception = e;
                System.out.println("FAIL: " + testExecution + " " + e);
                testExecution.status = TestStatus.FAIL;
            }

            if (testExecution.status == TestStatus.FAIL) {
                groupExecution.status = TestStatus.FAIL;
            }
        }
    }

    protected List<TestExecution> executeTests(ExecutorService threadPool, VirtualMachinePool vmPool) {
        // override in implementations
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return "TestGroup [name=" + name + ", children=" + children + "]";
    }



}
