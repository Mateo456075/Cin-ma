<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Gérer les sièges" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="form-container">
        <p class="eyebrow">Infrastructure</p>
        <h1>Sièges — <c:out value="${salle.nom}" /></h1>
        <p class="form-hint">
            Capacité déclarée : <strong><c:out value="${salle.capacite}" /></strong> places &nbsp;|&nbsp;
            Type : <strong><c:out value="${salle.typeSalle}" /></strong>
        </p>

        <%-- ===== Formulaire : ajouter une rangée ===== --%>
        <h2>Ajouter une rangée</h2>
        <form action="${pageContext.request.contextPath}/gererSieges" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
            <input type="hidden" name="salleId"   value="${salle.id}">

            <label for="nomRangee">Nom de la rangée</label>
            <input type="text" id="nomRangee" name="nomRangee"
                   placeholder="Ex. A, B, Balcon…" maxlength="20" required>

            <label for="nombreSieges">Nombre de sièges dans cette rangée</label>
            <input type="number" id="nombreSieges" name="nombreSieges" min="1" required>

            <div class="form-actions">
                <button class="btn" type="submit">Ajouter la rangée</button>
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admins">Retour</a>
            </div>
        </form>

        <%-- ===== Tableau des rangées et sièges existants ===== --%>
        <h2>Rangées enregistrées</h2>
        <c:choose>
            <c:when test="${not empty rangees}">
                <div class="table-container card">
                    <table class="film-table">
                        <thead>
                            <tr>
                                <th scope="col">Rangée</th>
                                <th scope="col">Nb sièges</th>
                                <th scope="col">Numéros</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="rangee" items="${rangees}">
                                <tr>
                                    <td><c:out value="${rangee.nomRangee}" /></td>
                                    <td><c:out value="${rangee.nombreSieges}" /></td>
                                    <td>
                                        <c:forEach var="siege" items="${rangee.sieges}" varStatus="loop">
                                            <span class="seat"><c:out value="${siege.numero}" /></span>
                                            <c:if test="${!loop.last}">, </c:if>
                                        </c:forEach>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <p class="empty-state">Aucune rangée ajoutée pour l'instant.</p>
            </c:otherwise>
        </c:choose>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
