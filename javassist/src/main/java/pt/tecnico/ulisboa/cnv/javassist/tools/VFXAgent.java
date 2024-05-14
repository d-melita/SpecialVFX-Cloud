package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javassist.CannotCompileException;
import javassist.CtBehavior;

public class VFXAgent extends CodeDumper {

    public static class VFXMetrics {
        private long nblocks;
        private long ninsts;
        private long nmethods;

        public VFXMetrics(long nblocks, long ninsts, long nmethods) {
            this.nblocks = nblocks;
            this.ninsts = ninsts;
            this.nmethods = nmethods;
        }

        public long getNblocks() { return this.nblocks; }
        public long getNInsts() { return this.ninsts; }
        public long getNMethods() { return this.nmethods; }

        @Override
        public String toString() {
            return "VFXMetrics{" +
                "nblocks=" + nblocks +
                ", ninsts=" + ninsts +
                ", nmethods=" + nmethods +
                '}';
        }
    }

    /**
     * Number of executed basic blocks.
     */
    public static Map<Long,Long> nblocks = new ConcurrentHashMap<>();

    /**
     * Number of executed methods.
     */
    public static Map<Long,Long> nmethods = new ConcurrentHashMap<>();

    /**
     * Number of executed instructions.
     */
    public static Map<Long,Long> ninsts = new ConcurrentHashMap<>();

    public VFXAgent(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    /**
     * Zeros all stats for current thread.
     */
    public static void resetStats() {
        long tid = Thread.currentThread().getId();
        nblocks.put(tid, 0L);
        ninsts.put(tid, 0L);
        nmethods.put(tid, 0L);
    }

    /**
     * Get stats for current thread
     */
    public static VFXMetrics getStats() {
        long tid = Thread.currentThread().getId();
        return new VFXMetrics(nblocks.get(tid), ninsts.get(tid), nmethods.get(tid));
    }

    public static void incBasicBlock(int position, int length) {
        long tid = Thread.currentThread().getId();
        nblocks.put(tid, nblocks.getOrDefault(tid, 0L) + 1);
        ninsts.put(tid, nblocks.getOrDefault(tid, 0L) + length);
    }

    public static void incBehavior(String name) {
        long tid = Thread.currentThread().getId();
        nmethods.put(tid, nblocks.getOrDefault(tid, 0L) + 1);
    }

    // public static void printStatistics() {
    //     int nblocksT = 0;
    //     int nmethodsT = 0;
    //     int ninstsT = 0;
    //
    //     for (Map.Entry<Long,Long> entry: nblocks.entrySet()) nblocksT += entry.getValue();
    //     for (Map.Entry<Long,Long> entry: nmethods.entrySet()) nmethodsT += entry.getValue();
    //     for (Map.Entry<Long,Long> entry: ninsts.entrySet()) ninstsT += entry.getValue();
    //
    //     System.out.println(String.format("[%s] Number of executed methods: %s", VFXAgent.class.getSimpleName(), nmethodsT));
    //     System.out.println(String.format("[%s] Number of executed basic blocks: %s", VFXAgent.class.getSimpleName(), nblocksT));
    //     System.out.println(String.format("[%s] Number of executed instructions: %s", VFXAgent.class.getSimpleName(), ninstsT));
    // }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.insertAfter(String.format("%s.incBehavior(\"%s\");", VFXAgent.class.getName(), behavior.getLongName()));
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);", VFXAgent.class.getName(), block.getPosition(), block.getLength()));
    }

}
