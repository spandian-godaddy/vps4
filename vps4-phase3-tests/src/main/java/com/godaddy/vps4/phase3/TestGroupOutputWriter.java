package com.godaddy.vps4.phase3;

import java.io.IOException;
import java.io.Writer;

public class TestGroupOutputWriter {

    final Writer writer;

    int depth;

    public TestGroupOutputWriter(Writer writer) {
        this.writer = writer;
    }

    public void write(TestGroupExecution groupExecution) throws IOException {

        indent();
        writer.write("  ");
        write(groupExecution.status);
        writer.write("  ");
        writer.write(groupExecution.testGroup.getName());
        writer.write('\n');

        // tests
        for (TestExecution testExecution : groupExecution.tests) {
            write(testExecution);
        }

        if (groupExecution.tests.size() > 0) {
            writer.write('\n');
        }

        // children
        depth++;
        for (TestGroupExecution child : groupExecution.children) {
            write(child);
        }
        depth--;
    }

    protected void write(TestStatus status) throws IOException {
        switch(status) {
        case FAIL: writer.write('X'); break;
        case PASS: writer.write("\u2714"); break;
        default:
            writer.write('*');
        }
    }

    private static final char[] INDENT = { ' ', ' ', ' ', ' ' };

    protected void indent() throws IOException {
        for (int i=0; i<depth; i++) {
            writer.write(INDENT);
        }
    }

    protected void write(TestExecution testExecution) throws IOException {
        indent();
        indent();
        write(testExecution.status);
        writer.write("  ");
        writer.write(testExecution.test.toString());
        writer.write("\n");

        // write exception
        if (testExecution.status == TestStatus.FAIL) {
            write(testExecution.exception);
        }
    }

    protected void write(Throwable e) throws IOException {

        for (StackTraceElement ste : e.getStackTrace()) {
            indent();
            writer.write("     ");
            writer.write(ste.toString());
            writer.write('\n');
        }

        // cause
        if (e.getCause() != null) {
            write(e.getCause());
        }

        // suppressed
        for (Throwable suppressed : e.getSuppressed()) {
            write(suppressed);
        }
    }
}

