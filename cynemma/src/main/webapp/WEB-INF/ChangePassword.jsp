<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Changer mon mot de passe" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="form-container">
        <h1>Modifier mon mot de passe</h1>
        <p class="form-hint">Tous les champs sont obligatoires.</p>

        <c:if test="${not empty errors}">
            <p class="error-message" role="alert"><c:out value="${errors}" /></p>
        </c:if>
        <c:if test="${not empty success}">
            <p class="success-message" role="status"><c:out value="${success}" /></p>
        </c:if>

        <form action="${pageContext.request.contextPath}/changerMotDePasse" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
            <label for="oldPassword">Ancien mot de passe</label>
            <input type="password" id="oldPassword" name="oldPassword" autocomplete="current-password" required>

            <label for="newPassword">Nouveau mot de passe</label>
            <input type="password" id="newPassword" name="newPassword" autocomplete="new-password" required>

            <label for="confirmPassword">Confirmer le nouveau mot de passe</label>
            <input type="password" id="confirmPassword" name="confirmPassword" autocomplete="new-password" required>

            <button type="submit" class="btn">Mettre à jour</button>
        </form>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
