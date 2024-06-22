package mg.ituprom16.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.ituprom16.Mapping;
import mg.ituprom16.ModelView;
import mg.ituprom16.annotations.ParamAttribut;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ModuleLayer.Controller;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class FrontController extends HttpServlet {

    private HashMap<String, Mapping> urlMappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            getListeControlleurs(getServletContext().getInitParameter("controllerPackage"));
        } catch (Exception e) {
            throw new ServletException("Erreur lors de l'initialisation des contrôleurs : " + e.getMessage(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        processRequest(req, resp);
    }

    private void getListeControlleurs(String packagename) throws Exception {
        if (packagename == null || packagename.isEmpty()) {
            throw new Exception("Le nom du package est vide ou null.");
        }

        String bin_path = "WEB-INF/classes/" + packagename.replace(".", "/");
        bin_path = getServletContext().getRealPath(bin_path);

        File packageDir = new File(bin_path);
        if (!packageDir.exists()) {
            throw new Exception("Le package spécifié n'existe pas.");
        }

        for (File file : packageDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".class")) {
                String className = packagename + "." + file.getName().replace(".class", "");
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(AnnotationController.class)) {
                    for (Method method : clazz.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(GetController.class)) {
                            GetController getAnnotation = method.getAnnotation(GetController.class);
                            String url = getAnnotation.value();
                            if (urlMappings.containsKey(url)) {
                                throw new Exception("Deux fonctions mappées pour l'URL: " + url);
                            } else {
                                Mapping mapping = new Mapping(clazz.getName(), method.getName(), getParameterTypes(method));
                                urlMappings.put(url, mapping);
                            }
                        }
                    }
                }
            }
        }
    }

    private String[] getParameterTypes(Method method) {
        Class<?>[] paramTypes = method.getParameterTypes();
        String[] paramTypeNames = new String[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypeNames[i] = paramTypes[i].getName();
        }
        return paramTypeNames;
    }
    
    protected Object invokeMethod(HttpServletRequest request, String className, String methodName, String[] parameterTypeNames)
            throws IOException, NoSuchMethodException {
        Object returnValue = null;
        try {
            Class<?> clazz = Class.forName(className);
            Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
            for (int i = 0; i < parameterTypeNames.length; i++) {
                parameterTypes[i] = Class.forName(parameterTypeNames[i]);
            }
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);

            Parameter[] methodParams = method.getParameters();
            Object[] args = new Object[methodParams.length];

            Enumeration<String> params = request.getParameterNames();
            Map<String, String> paramMap = new HashMap<>();

            while (params.hasMoreElements()) {
                String paramName = params.nextElement();
                paramMap.put(paramName, request.getParameter(paramName));
            }
            for (int i = 0; i < methodParams.length; i++) {
                if (methodParams[i].isAnnotationPresent(Param.class)) {
                    String paramName = methodParams[i].getAnnotation(Param.class).name();
                    String paramValue = paramMap.get(paramName);
                    args[i] = paramValue;
                } else if (methodParams[i].isAnnotationPresent(ParamObjet.class)) {
                    Class<?> paramType = methodParams[i].getType();
                    Object paramObject = paramType.getDeclaredConstructor().newInstance();
                    Field[] fields = paramType.getDeclaredFields();
                    for (Field field : fields) {
                        String fieldName = field.getName();
                        if (field.isAnnotationPresent(ParamAttribut.class)) {
                            String paramAttributName = field.getAnnotation(ParamAttribut.class).name();
                            if (!paramAttributName.isEmpty()) {
                                fieldName = paramAttributName;
                            }
                        }
                        field.setAccessible(true);
                        String paramValue = paramMap.get(fieldName);
                        field.set(paramObject, paramValue);
                    }
                    args[i] = paramObject;
                } else {
                    args[i] = null;
                }
            }

            Object instance = clazz.getDeclaredConstructor().newInstance();
            returnValue = method.invoke(instance, args);

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        return returnValue;
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        String url = request.getRequestURI().substring(request.getContextPath().length());
        
        Mapping mapping = urlMappings.get(url);

        if (mapping != null) {
            try {
                Object returnValue = invokeMethod(request, mapping.getClassName(), mapping.getMethodName(), mapping.getParameterTypes());

                if (returnValue instanceof String) {
                    try (PrintWriter out = response.getWriter()) {
                        out.println("<p>Contenu de la méthode <strong>" + mapping.methodToString() + "</strong> : " + (String) returnValue + "</p>");
                    }
                } else if (returnValue instanceof ModelView) {
                    ModelView modelView = (ModelView) returnValue;
                    String viewUrl = modelView.getUrl();
                    HashMap<String, Object> data = modelView.getData();

                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }

                    RequestDispatcher dispatcher = request.getRequestDispatcher(viewUrl);
                    dispatcher.forward(request, response);
                } else if (returnValue == null) {
                    throw new ServletException("La méthode \"" + mapping.methodToString() + "\" retourne une valeur NULL");
                } else {
                    throw new ServletException("Le type de retour de l'objet \"" + returnValue.getClass().getName() + "\" n'est pas pris en charge par le Framework");
                }

            } catch (NoSuchMethodException | IOException e) {
                throw new ServletException("Erreur lors de l'invocation de la méthode \"" + mapping.methodToString() + "\"", e);
            }

        } else {
            throw new ServletException("Pas de méthode Get associée à l'URL: \"" + url + "\"");
        }
    }
}
