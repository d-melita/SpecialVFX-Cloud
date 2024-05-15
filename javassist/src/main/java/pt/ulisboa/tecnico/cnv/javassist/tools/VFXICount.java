package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javassist.CannotCompileException;
import javassist.CtBehavior;

public class VFXICount extends CodeDumper {

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

    public VFXICount(List<String> packageNameList, String writeDestination) {
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
    public static Map<String, Long> getStats() {
        long tid = Thread.currentThread().getId();
        Map<String, Long> map = new HashMap<>();
        map.put("nblocks", nblocks.get(tid));
        map.put("ninsts", ninsts.get(tid));
        map.put("nmethods", nmethods.get(tid));
        return map;
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

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.insertAfter(String.format("%s.incBehavior(\"%s\");", VFXICount.class.getName(), behavior.getLongName()));
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);", VFXICount.class.getName(), block.getPosition(), block.getLength()));
    }

}
