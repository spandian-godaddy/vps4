package com.godaddy.vps4.phase3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.godaddy.vps4.phase3.virtualmachine.VirtualMachine;
import com.godaddy.vps4.phase3.virtualmachine.VirtualMachinePool;

public class ImageTestGroup extends TestGroup {

    protected final List<VmTest> tests = new ArrayList<>();

    final String imageName;

    public ImageTestGroup(String imageName) {
        super("[image: " + imageName + "]");

        this.imageName = imageName;
    }

    public void add(VmTest test) {
        tests.add(test);
    }

    public void addTests(Collection<VmTest> tests) {
        this.tests.addAll(tests);
    }

    @Override
    protected List<TestExecution> executeTests(ExecutorService threadPool, VirtualMachinePool vmPool) {

        List<TestExecution> testExecutions = new ArrayList<>();

        for (VmTest test : this.tests) {
            TestExecution execution = new TestExecution(test);
            try {
                execution.future = threadPool.submit( () -> {
                    execution.status = TestStatus.RUNNING;
                    VirtualMachine vm = vmPool.getVm(this.imageName);
                    try {
                        test.execute(vm);
                    } finally {
                        vm.release();
                    }
                } );
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
            testExecutions.add(execution);
        }

        return testExecutions;
    }

    @Override
    public String toString() {
        return "ImageTestGroup [imageName=" + imageName + ", tests=" + tests + "]";
    }


}
