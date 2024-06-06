package mg.ituprom16.controller;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import mg.ituprom16.Mapping;
import mg.ituprom16.ModelView;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.ArrayList;

public class FrontController extends HttpServlet {

    private HashMap<String, Mapping> urlMappings = new HashMap<>();
    private ArrayList<String> exceptions = new ArrayList<>();

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            getListeControlleurs(getServletContext().getInitParameter("controllerPackage"));
        } catch (Exception e) {
            exceptions.add(e.getMessage());
            e.printStackTrace();
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

        File b = new File(bin_path);
        if (!b.exists()) {
            throw new Exception("Le package spécifié n'existe pas.");
        }

        for (File fichier : b.listFiles()) {
            if (fichier.isFile() && fichier.getName().endsWith(".class")) {
                String className = packagename + "." + fichier.getName().replace(".class", "");
                Class<?> classe = Class.forName(className);
                if (classe.isAnnotationPresent(Controller.class)) {
                    for (Method method : classe.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(Get.class)) {
                            Get getAnnotation = method.getAnnotation(Get.class);
                            String url = getAnnotation.value();
                            if (urlMappings.containsKey(url)) {
                                exceptions.add("Deux fonctions mappées pour l'URL: " + url);
                            } else {
                                Mapping mapping = new Mapping(classe.getName(), method.getName());
                                urlMappings.put(url, mapping);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String contextPath = req.getContextPath();
        String urlPath = req.getRequestURI().substring(contextPath.length());

        if (urlPath == null || urlPath.isEmpty()) {
            resp.getWriter().println("Aucun url trouve");
            return;
        }

        if (!urlMappings.containsKey(urlPath)) {
            exceptions.add("Aucun mapping pour l'url actuel: " + urlPath);
        } else {
            Mapping mapping = urlMappings.get(urlPath);

            try {
                Class<?> clazz = Class.forName(mapping.getClassName());
                Object instance = clazz.getDeclaredConstructor().newInstance();
                Method method = clazz.getDeclaredMethod(mapping.getMethodName());

                Object result = method.invoke(instance);

                if (result instanceof String) {
                    resp.getWriter().println((String) result);
                } else if (result instanceof ModelView) {
                    ModelView modelView = (ModelView) result;
                    for (HashMap.Entry<String, Object> entry : modelView.getData().entrySet()) {
                        req.setAttribute(entry.getKey(), entry.getValue());
                    }
                    RequestDispatcher dispatcher = req.getRequestDispatcher(modelView.getUrl());
                    dispatcher.forward(req, resp);
                } else {
                    exceptions.add("Type de retour non reconnu pour l'URL: " + urlPath);
                }
            } catch (Exception e) {
                exceptions.add("Erreur lors de l'invocation de la méthode: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (!exceptions.isEmpty()) {
            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.println("<html><body>");
            out.println("<h3>Erreurs:</h3>");
            for (String exception : exceptions) {
                out.println("<p>" + exception + "</p>");
            }
            out.println("</body></html>");
            exceptions.clear();
        }
    }
}
