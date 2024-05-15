package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javassist.CannotCompileException;
import javassist.CtBehavior;

public class ICountParallel extends CodeDumper {

    /**
     * Number of executed basic blocks.
     */
    private static Map<Long,Long> nblocks = new ConcurrentHashMap<>();

    /**
     * Number of executed methods.
     */
    private static Map<Long,Long> nmethods = new ConcurrentHashMap<>();

    /**
     * Number of executed instructions.
     */
    private static Map<Long,Long> ninsts = new ConcurrentHashMap<>();

    private static AtomicLong total = new AtomicLong(0);

    public ICountParallel(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    public static void incBasicBlock(int position, int length) {
        long tid = Thread.currentThread().getId();
        nblocks.put(tid, nblocks.getOrDefault(tid, 0L) + 1);
        ninsts.put(tid, nblocks.getOrDefault(tid, 0L) + length);
    }

    public static void incBehavior(String name) {
        long tid = Thread.currentThread().getId();
        nmethods.put(tid, nblocks.getOrDefault(tid, 0L) + 1);
        total.getAndIncrement();
    }

    public static void printStatistics() {
        int nblocksT = 0;
        int nmethodsT = 0;
        int ninstsT = 0;

        for (Map.Entry<Long,Long> entry: nblocks.entrySet()) nblocksT += entry.getValue();
        for (Map.Entry<Long,Long> entry: nmethods.entrySet()) nmethodsT += entry.getValue();
        for (Map.Entry<Long,Long> entry: ninsts.entrySet()) ninstsT += entry.getValue();

        System.out.println(String.format("[%s] Value of total: %s", ICountParallel.class.getSimpleName(), total.get()));
        System.out.println(String.format("[%s] Number of executed methods: %s", ICountParallel.class.getSimpleName(), nmethodsT));
        System.out.println(String.format("[%s] Number of executed basic blocks: %s", ICountParallel.class.getSimpleName(), nblocksT));
        System.out.println(String.format("[%s] Number of executed instructions: %s", ICountParallel.class.getSimpleName(), ninstsT));
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
        behavior.insertAfter(String.format("%s.incBehavior(\"%s\");", ICountParallel.class.getName(), behavior.getLongName()));

        if (behavior.getName().equals("main")) {
            behavior.insertAfter(String.format("%s.printStatistics();", ICountParallel.class.getName()));
        }
    }

    @Override
    protected void transform(BasicBlock block) throws CannotCompileException {
        super.transform(block);
        block.behavior.insertAt(block.line, String.format("%s.incBasicBlock(%s, %s);", ICountParallel.class.getName(), block.getPosition(), block.getLength()));
    }

}
