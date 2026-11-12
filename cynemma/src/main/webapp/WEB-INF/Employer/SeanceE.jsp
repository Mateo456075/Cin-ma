<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Programmation des séances" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <div class="page-heading">
        <div>
            <p class="eyebrow">Programmation</p>
            <h1>Séances programmées</h1>
        </div>
        <a class="btn" href="${pageContext.request.contextPath}/nouvelleSeance">Ajouter une séance</a>
    </div>

    <div class="table-container card" role="region" aria-label="Séances programmées, défilement horizontal possible" tabindex="0">
        <table class="film-table">
            <caption class="sr-only">Séances programmées</caption>
            <thead>
                <tr>
                    <th scope="col">Affiche</th>
                    <th scope="col">Film</th>
                    <th scope="col">Date</th>
                    <th scope="col">Heure</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty seances}">
                        <c:forEach var="seance" items="${seances}">
                            <tr>
                                <td><img class="table-poster" src="${pageContext.request.contextPath}/${filmsMap[seance.filmId].affiche}" alt=""></td>
                                <td><c:out value="${filmsMap[seance.filmId].titre}" /></td>
                                <td><c:out value="${seance.date}" /></td>
                                <td><c:out value="${seance.horaire}" /></td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr><td colspan="4" class="empty-state">Aucune séance n'est programmée.</td></tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
