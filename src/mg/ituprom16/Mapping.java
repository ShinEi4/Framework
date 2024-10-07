package mg.ituprom16;

public class Mapping {
    private String className;
    private VerbAction[] actions;

    public Mapping(String className, VerbAction[] actions) {
        this.className = className;
        this.actions = actions;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public VerbAction[] getActions() {
        return actions;
    }

    public void setActions(VerbAction[] actions) {
        this.actions = actions;
    }

    public VerbAction findActionByVerb(String verb) {
        for (VerbAction action : actions) {
            if (action.getVerb().equalsIgnoreCase(verb)) {
                return action;
            }
        }
        return null;
    }
}
