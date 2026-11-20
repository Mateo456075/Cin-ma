package fr.turgot.utils;
import java.io.IOException;
import java.util.Set;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/page")

public class PageServlet extends HttpServlet {
    private static final Set<String> PUBLIC_PAGES = Set.of(
            "Authentification",
            "Creat",
            "visiteur/visiteur");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String path = request.getParameter("path");
        
        if (isAllowedPublicPage(path)) {
            request.getRequestDispatcher(LinkHelper.JSP_ROOT + path + ".jsp")
                    .forward(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    static boolean isAllowedPublicPage(String path) {
        return path != null && PUBLIC_PAGES.contains(path);
    }
}
