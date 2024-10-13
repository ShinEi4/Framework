package mg.ituprom16;

import java.util.Objects;

public class VerbAction {
    private String methodName;
    private String verb; // GET ou POST
    private String[] parameterTypes;

    public VerbAction(String methodName, String verb, String[] parameterTypes) {
        this.methodName = methodName;
        this.verb = verb;
        this.parameterTypes = parameterTypes;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getVerb() {
        return verb;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        VerbAction other = (VerbAction) obj;
        return methodName.equals(other.methodName) && verb.equals(other.verb);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, verb);
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
