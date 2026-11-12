<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Gestion des films" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <div class="page-heading">
        <div>
            <p class="eyebrow">Programmation</p>
            <h1>Gestion des films</h1>
        </div>
        <a class="btn" href="${pageContext.request.contextPath}/creerfilm">Ajouter un film</a>
    </div>

    <c:if test="${param.error == 'seance'}">
        <p class="error-message" role="alert">Impossible de supprimer ce film car des séances sont encore programmées pour celui-ci.</p>
    </c:if>

    <div class="table-container card" role="region" aria-label="Catalogue des films, défilement horizontal possible" tabindex="0">
        <table class="film-table">
            <caption class="sr-only">Catalogue des films</caption>
            <thead>
                <tr>
                    <th scope="col">Affiche</th>
                    <th scope="col">Titre</th>
                    <th scope="col">Réalisateur</th>
                    <th scope="col">Année</th>
                    <th scope="col">Genre</th>
                    <th scope="col">Durée</th>
                    <th scope="col">Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty films}">
                        <c:forEach var="film" items="${films}">
                            <c:url var="editUrl" value="/modifierFilm">
                                <c:param name="id" value="${film.id}" />
                            </c:url>
                            <tr>
                                <td><img class="table-poster" src="${pageContext.request.contextPath}/${film.affiche}" alt="Affiche du film"></td>
                                <td><c:out value="${film.titre}" /></td>
                                <td><c:out value="${film.realisateur}" /></td>
                                <td><c:out value="${film.annee}" /></td>
                                <td><c:out value="${film.genre}" /></td>
                                <td><c:out value="${film.dureeMinutes}" /> min</td>
                                <td class="table-actions">
                                    <a class="btn btn-secondary" href="${editUrl}">Modifier</a>
                                    <form action="${pageContext.request.contextPath}/supprimerFilm" method="post"
                                          onsubmit="return confirm('Supprimer ce film ?');">
                                        <input type="hidden" name="id" value="${film.id}">
                                        <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                                        <button class="btn btn-danger" type="submit">Supprimer</button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr><td colspan="7" class="empty-state">Aucun film enregistré.</td></tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
