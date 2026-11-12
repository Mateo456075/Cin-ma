<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Séances" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <p class="eyebrow">Programmation</p>
    <h1>Séances de <c:out value="${film.titre}" /></h1>
    <div class="table-container card" role="region" aria-label="Séances disponibles, défilement horizontal possible" tabindex="0">
        <table class="film-table">
            <caption class="sr-only">Séances disponibles pour <c:out value="${film.titre}" /></caption>
            <thead>
                <tr>
                    <th scope="col">Affiche</th>
                    <th scope="col">Date</th>
                    <th scope="col">Heure</th>
                    <th scope="col">Réservation</th>
                </tr>
            </thead>
            <tbody>
                <c:choose>
                    <c:when test="${not empty seances}">
                        <c:forEach var="seance" items="${seances}">
                            <c:url var="reservationUrl" value="/billetterie">
                                <c:param name="seanceId" value="${seance.id}" />
                                <c:param name="filmId" value="${film.id}" />
                            </c:url>
                            <tr>
                                <td><img class="table-poster" src="${pageContext.request.contextPath}/${film.affiche}" alt=""></td>
                                <td><c:out value="${seance.date}" /></td>
                                <td><c:out value="${seance.horaire}" /></td>
                                <td><a class="btn" href="${reservationUrl}">Prendre une place</a></td>
                            </tr>
                        </c:forEach>
                    </c:when>
                    <c:otherwise>
                        <tr><td colspan="4" class="empty-state">Aucune séance n'est programmée pour ce film.</td></tr>
                    </c:otherwise>
                </c:choose>
            </tbody>
        </table>
    </div>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
