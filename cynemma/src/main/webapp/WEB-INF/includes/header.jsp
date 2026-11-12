<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${requestScope.pageTitle}" default="Cinema" /></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="site-shell">
    <a class="skip-link" href="#main-content">Passer au contenu principal</a>
    <header class="site-header">
        <a href="${pageContext.request.contextPath}/accueil" class="site-brand">Cynemma</a>
        <nav class="site-nav" aria-label="Navigation principale">
            <ul>
                <!-- Lien public visible par tous (connectés ou non) -->
                <li><a href="${pageContext.request.contextPath}/accueil">À l'affiche</a></li>

                <c:choose>
                    <%-- VISITEUR NON CONNECTÉ --%>
                    <c:when test="${empty sessionScope.usere}">
                        <li><a href="${pageContext.request.contextPath}/connexion">Connexion</a></li>
                        <li><a href="${pageContext.request.contextPath}/nouveaucompte">Créer un compte</a></li>
                    </c:when>

                    <%-- UTILISATEUR CONNECTÉ --%>
                    <c:otherwise>
                        
                        <%-- Menu Spécifique CLIENT --%>
                        <c:if test="${sessionScope.usere.typeUtilisateur == 'CLIENT'}">
                            <li><a href="${pageContext.request.contextPath}/client">Espace client</a></li>
                        </c:if>

                        <%-- Menu Spécifique EMPLOYE --%>
                        <c:if test="${sessionScope.usere.typeUtilisateur == 'EMPLOYE'}">
                            <li><a href="${pageContext.request.contextPath}/nouvelleSeance">Créer une séance</a></li>
                            <li><a href="${pageContext.request.contextPath}/creerfilm">Créer un film</a></li>
                            <li><a href="${pageContext.request.contextPath}/employe/listeSeances">Séances</a></li>
                            <li><a href="${pageContext.request.contextPath}/listeFilms">Films</a></li>
                        </c:if>

                        <%-- Menu Spécifique ADMINISTRATEUR --%>
                        <c:if test="${sessionScope.usere.typeUtilisateur == 'ADMINISTRATEUR'}">
                            <li><a href="${pageContext.request.contextPath}/creersalle">Créer une salle</a></li>
                            <li><a href="${pageContext.request.contextPath}/admins">Administration</a></li>
                        </c:if>

                        <%-- Actions communes à tous les rôles connectés --%>
                        <li><a href="${pageContext.request.contextPath}/changerMotDePasse">Changer le mot de passe</a></li>
                        
                        <%-- Bouton déconnexion avec le pseudo --%>
                        <li>
                            <a href="${pageContext.request.contextPath}/deconnexion" class="btn">
                                Déconnexion (${sessionScope.usere.login})
                            </a>
                        </li>
                    </c:otherwise>
                </c:choose>
            </ul>
        </nav>
    </header>
