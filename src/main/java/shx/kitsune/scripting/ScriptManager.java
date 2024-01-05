package shx.kitsune.scripting;

public class ScriptManager {
    private ScriptEngine engine;

    public ScriptManager() {
        engine = new ScriptEngine();
    }

    public ScriptEngine getEngine() {
        return engine;
    }
}
