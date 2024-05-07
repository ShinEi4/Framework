package mg.ituprom16;

import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.*;
public class FrontController extends HttpServlet{
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try{
            processRequest(request, response);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    protected void doPost(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException{
        try{
            processRequest(request, response);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void processRequest(HttpServletRequest request,HttpServletResponse response) throws Exception
    {
        response.setContentType("text/html");
        PrintWriter out=response.getWriter();
        out.println("<html>");
        out.println("<head><title>Print URL</title></head>");
        out.println("<body>");
        out.println("<h1>URL à imprimer :</h1>");
        out.println("<p>" + request.getRequestURL() + "</p>");
        out.println("</body></html>");
    }
    
}