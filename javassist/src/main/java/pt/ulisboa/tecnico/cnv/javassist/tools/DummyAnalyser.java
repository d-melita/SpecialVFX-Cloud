package pt.ulisboa.tecnico.cnv.javassist.tools;

import java.util.List;

import javassist.CtBehavior;

public class DummyAnalyser extends AbstractJavassistTool {

    public DummyAnalyser(List<String> packageNameList, String writeDestination) {
        super(packageNameList, writeDestination);
    }

    @Override
    protected void transform(CtBehavior behavior) throws Exception {
        super.transform(behavior);
    }
}
