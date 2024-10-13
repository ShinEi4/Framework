package mg.ituprom16.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.ituprom16.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.gson.Gson;
import java.util.List;

public class FrontController extends HttpServlet {

    private HashMap<String, Mapping> urlMappings = new HashMap<>();
    private List<Exception> exceptions = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            getListeControlleurs(getServletContext().getInitParameter("controllerPackage"));
        } catch (Exception e) {
            exceptions.add(new ServletException("Erreur lors de l'initialisation des contrôleurs : " + e.getMessage(), e));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            processRequest(req, resp);
        } catch (Exception e) {
            handleExceptions(req, resp, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            processRequest(req, resp);
        } catch (Exception e) {
            handleExceptions(req, resp, e);
        }
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
                    Mapping currentMapping = null;
    
                    for (Method method : clazz.getDeclaredMethods()) {
                        String url = null;
                        String verb = "GET";
    
                        if (method.isAnnotationPresent(GET.class)) {
                            url = method.getAnnotation(GET.class).value();
                            verb = "GET";
                        } else if (method.isAnnotationPresent(POST.class)) {
                            url = method.getAnnotation(POST.class).value();
                            verb = "POST";
                        } 
                        currentMapping = urlMappings.get(url);
                        if (currentMapping == null) {
                            currentMapping = new Mapping(clazz.getName(), new VerbAction[0]);
                            urlMappings.put(url, currentMapping);
                        }
                        verifMethodParameter(method);
    
                        VerbAction verbAction = new VerbAction(verb, method.getName(), getParameterTypes(method));
                        VerbAction[] updatedActions = new VerbAction[currentMapping.getActions().length + 1];
                        System.arraycopy(currentMapping.getActions(), 0, updatedActions, 0, currentMapping.getActions().length);
                        updatedActions[updatedActions.length - 1] = verbAction;
                        currentMapping.setActions(updatedActions);
                    }
                }
            }
        }
    }
    

    private void verifMethodParameter(Method method) throws Exception {
        for (Parameter parameter : method.getParameters()) {
            if (!parameter.isAnnotationPresent(Param.class)) {
                throw new Exception("ETU002429-Erreur:  \"" + parameter.getName() + "\" de la méthode \"" + method.getName() + "\" dans la classe \"" + method.getDeclaringClass().getName() + "\" n'a pas l'annotation @Param.");
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
            throws Exception {
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
                } else if (parameterTypes[i] == Session.class) {
                    args[i] = new Session(request.getSession());
                } else {
                    args[i] = null;
                }
            }

            Object instance = clazz.getDeclaredConstructor().newInstance();
            returnValue = method.invoke(instance, args);

        } catch (Exception e) {
            exceptions.add(e);
        }
        return returnValue;
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String url = request.getRequestURI().substring(request.getContextPath().length());
        if (url.contains("?")) {
            url = url.substring(0, url.indexOf("?"));
        }

        Mapping mapping = urlMappings.get(url);
        response.setContentType("application/json;charset=UTF-8");

        if (mapping != null) {
            String httpVerb = request.getMethod();
            VerbAction verbAction = null;

            // Utilisation d'un Set pour éviter les doublons
            Set<VerbAction> actionsSet = new HashSet<>(Arrays.asList(mapping.getActions()));

            // Si la taille du Set est différente de celle du tableau, il y a un doublon
            if (actionsSet.size() != mapping.getActions().length) {
                // Préparation de la réponse avec un statut 500
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
                response.getWriter().write("{\"error\": \"Conflit : deux méthodes avec le même verb et le même nom sont définies.\"}");
                return;
            }

            // Recherche de l'action correspondant au verbe HTTP
            for (VerbAction action : mapping.getActions()) {
                if (action.getVerb().equals(httpVerb)) {
                    verbAction = action;
                    break;
                }
            }

            if (verbAction == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400
                response.getWriter().write("{\"error\": \"Le verbe HTTP " + httpVerb + " n'est pas pris en charge pour cette URL.\"}");
                return;
            }

            try {
                // Appel de la méthode avec les types de paramètres appropriés
                Object returnValue = invokeMethod(request, mapping.getClassName(), verbAction.getMethodName(), verbAction.getParameterTypes());

                if (returnValue.getClass().isAnnotationPresent(RestAPI.class)) {
                    Gson gson = new Gson();
                    if (returnValue instanceof ModelView) {
                        ModelView modelView = (ModelView) returnValue;
                        String json = gson.toJson(modelView.getData());
                        response.getWriter().write(json);
                    } else {
                        String json = gson.toJson(returnValue);
                        response.getWriter().write(json);
                    }
                } else {
                    if (returnValue instanceof String) {
                        try (PrintWriter out = response.getWriter()) {
                            out.println("<p>Contenu de la méthode <strong>" + verbAction.methodToString() + "</strong> : " + (String) returnValue + "</p>");
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
                        throw new ServletException("La méthode \"" + verbAction.methodToString() + "\" retourne une valeur NULL");
                    } else {
                        throw new ServletException("Le type de retour de l'objet \"" + returnValue.getClass().getName() + "\" n'est pas pris en charge par le Framework");
                    }
                }
            } catch (Exception e) {
                // Préparer la réponse en cas d'erreur interne
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
                response.getWriter().write("{\"error\": \"Erreur lors de l'invocation de la méthode : " + e.getMessage() + "\"}");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
            response.getWriter().write("{\"error\": \"Aucune méthode associée à l'URL: " + url + "\"}");
        }
    }

    
    
    
    private void handleExceptions(HttpServletRequest req, HttpServletResponse resp, Exception e) throws IOException {
        PrintWriter out = resp.getWriter();
        resp.setContentType("text/html");

        out.println("<html><body>");
        out.println("<h1>Des erreurs se sont produites:</h1>");
        out.println("<ul>");
        for (Exception exception : exceptions) {
            out.println("<li>" + exception.getMessage() + "</li>");
            for (StackTraceElement ste : exception.getStackTrace()) {
                out.println("<li>&emsp;" + ste + "</li>");
            }
        }
        out.println("</ul>");
        out.println("</body></html>");

        exceptions.clear();
    }
}
