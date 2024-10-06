package mg.ituprom16;

public class Mapping {
    String className;
    String methodName;
    String[] parameterTypes;
    String verb;

    public Mapping(String className, String methodName, String[] parameterTypes,String  verb) {
        this.setClassName(className);
        this.setMethodName(methodName);
        this.setParameterTypes(parameterTypes);
        this.setVerb(verb);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
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

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
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
