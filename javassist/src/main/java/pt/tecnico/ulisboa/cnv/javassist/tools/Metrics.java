package pt.ulisboa.tecnico.cnv.javassist.tools;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;

public class Metrics extends AbstractJavassistTool {
    // This is going to be the our set of metrics to extract via instrumentation

    protected static class Statistics {
        private long basicBlockCount;
        private long instCount;

        public Statistics() {
        }

        public long getBasicBlockCount() {
            return basicBlockCount;
        }

        public long getInstCount() {
            return instCount;
        }

        public void setBasicBlockCount(long basicBlockCount) {
            this.basicBlockCount = basicBlockCount;
        }

        public void setInstCount(long instCount) {
            this.instCount = instCount;
        }
    }

    public static long getThreadId() {
        return Thread.currentThread().getId();
    }

    public static void printStatistics() {
        // TODO
    }

    public static void writeStatisticsToFile() {
        // TODO
    }
}
