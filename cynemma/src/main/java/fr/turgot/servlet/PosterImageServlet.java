package fr.turgot.servlet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import fr.turgot.utils.PosterStorage;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/image/*")
public class PosterImageServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String pathInfo = request.getPathInfo();
        String relativePath = pathInfo == null ? null : "image" + pathInfo;
        if (!PosterStorage.isManagedPath(relativePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Path image;
        try {
            image = PosterStorage.resolve(relativePath, getServletContext());
        } catch (PosterStorage.StorageConfigurationException e) {
            getServletContext().log("Le stockage persistant des affiches n'est pas configuré.", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        } catch (IOException e) {
            getServletContext().log("Impossible d'accéder au fichier d'affiche.", e);
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        if (image == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        response.setContentType(PosterStorage.contentType(relativePath));
        response.setContentLengthLong(Files.size(image));
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "public, max-age=86400");
        Files.copy(image, response.getOutputStream());
    }
}
