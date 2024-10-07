package mg.ituprom16;

public class VerbAction {
    private String verb;
    private String methodName;
    private String[] parameterTypes;

    public VerbAction(String verb, String methodName, String[] parameterTypes) {
        this.verb = verb;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    // Getters et Setters
    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(String[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String methodToString() {
        StringBuilder methodString = new StringBuilder();
        methodString.append(methodName).append("(");
        for (int i = 0; i < parameterTypes.length; i++) {
            methodString.append(parameterTypes[i]);
            if (i < parameterTypes.length - 1) {
                methodString.append(", ");
            }
        }
        methodString.append(")");
        return methodString.toString();
    }
}
