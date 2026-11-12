<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Ajouter une salle" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="form-container">
        <p class="eyebrow">Infrastructure</p>
        <h1>Ajouter une salle</h1>
        <p class="form-hint">Tous les champs sont obligatoires.</p>
        <form action="${pageContext.request.contextPath}/creerSalle" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
            <label for="nom">Nom de la salle</label>
            <input type="text" id="nom" name="nom" placeholder="Ex. Salle 1" required>

            <label for="capacite">Capacité</label>
            <input type="number" id="capacite" name="capacite" min="1" placeholder="Ex. 150" required>

            <div class="form-actions">
                <button class="btn" type="submit">Enregistrer la salle</button>
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admins">Annuler</a>
            </div>
        </form>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
