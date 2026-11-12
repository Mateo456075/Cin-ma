<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Ajouter une séance" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="form-container">
        <p class="eyebrow">Programmation</p>
        <h1>Créer une nouvelle séance</h1>
        <p class="form-hint">Tous les champs sont obligatoires.</p>

        <c:if test="${not empty errorMsg}">
            <p class="error-message" role="alert"><c:out value="${errorMsg}" /></p>
        </c:if>
        <c:if test="${not empty successMsg}">
            <p class="success-message" role="status"><c:out value="${successMsg}" /></p>
        </c:if>

        <form action="${pageContext.request.contextPath}/nouvelleSeance" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
            <div class="form-grid">
                <div>
                    <label for="filmId">Film</label>
                    <select id="filmId" name="filmId" required>
                        <option value="">-- Sélectionnez un film --</option>
                        <c:forEach var="film" items="${films}">
                            <option value="${film.id}">
                                <c:out value="${film.titre}" /> (Durée : ${film.dureeMinutes} min)
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div>
                    <label for="salleId">Salle</label>
                    <select id="salleId" name="salleId" required>
                        <option value="">-- Sélectionnez une salle --</option>
                        <c:forEach var="salle" items="${salles}">
                            <option value="${salle.id}">
                                <c:out value="${salle.nom}" /> (Capacité : ${salle.capacite} places)
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div>
                    <label for="date">Date</label>
                    <input type="date" id="date" name="date" required>
                </div>
                <div>
                    <label for="horaire">Heure de début</label>
                    <input type="time" id="horaire" name="horaire" required>
                </div>
            </div>
            <div class="form-actions">
                <button class="btn" type="submit">Enregistrer la séance</button>
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/employe/listeSeances">Annuler</a>
            </div>
        </form>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
