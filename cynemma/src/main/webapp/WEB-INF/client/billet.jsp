<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Réserver une place" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="form-container">
        <p class="eyebrow">Billetterie</p>
        <h1>Réserver pour : <c:out value="${film.titre}" /></h1>

        <c:if test="${not empty erreur}">
            <p class="error-message" role="alert"><c:out value="${erreur}" /></p>
        </c:if>
        <c:if test="${not empty succes}">
            <p class="success-message" role="status"><c:out value="${succes}" /></p>
            <c:url var="seancesUrl" value="/client/listeSeances">
                <c:param name="filmId" value="${film.id}" />
            </c:url>
            <a class="btn" href="${seancesUrl}">Retour aux séances</a>
        </c:if>

        <c:if test="${empty succes}">
            <div class="booking-summary">
                <p><strong>Date :</strong> <c:out value="${seance.date}" /></p>
                <p><strong>Heure :</strong> <c:out value="${seance.horaire}" /></p>
                <p><strong>Salle :</strong> <c:out value="${salle.nom}" /></p>
                <p><strong>Places disponibles :</strong> <c:out value="${placesDisponibles}" /></p>
            </div>
            <p class="form-hint">Le nombre de places est obligatoire.</p>
            <form action="${pageContext.request.contextPath}/billetterie" method="post">
                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                <input type="hidden" name="seanceId" value="${seance.id}">
                <input type="hidden" name="filmId" value="${film.id}">
                <label for="nbPlaces">Nombre de places</label>
                <input type="number" id="nbPlaces" name="nbPlaces" min="1"
                       max="${placesDisponibles}" required>
                <div class="form-actions">
                    <button class="btn" type="submit">Confirmer la réservation</button>
                    <a class="btn btn-secondary" href="javascript:history.back()">Retour</a>
                </div>
            </form>
        </c:if>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
