<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Authentification" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="form-container">
        <h1>Connexion à Cynemma</h1>
        <p class="form-intro">Connectez-vous pour accéder à votre espace.</p>
        <p class="form-hint">Tous les champs sont obligatoires.</p>
        <c:if test="${not empty errors}">
            <p class="error-message" role="alert"><c:out value="${errors}" /></p>
        </c:if>
        <c:if test="${not empty error}">
            <p class="error-message" role="alert"><c:out value="${error}" /></p>
        </c:if>
        <form action="${pageContext.request.contextPath}/connexion" method="post">
            <label for="login">Identifiant</label>
            <input id="login" type="text" name="login" autocomplete="username" required>

            <label for="mdp">Mot de passe</label>
            <input id="mdp" type="password" name="mdp" autocomplete="current-password" required>

            <button type="submit" class="btn">Se connecter</button>
        </form>
        <p class="form-intro">
            Vous n'avez pas encore de compte ?
            <a href="${pageContext.request.contextPath}/nouveaucompte">Créer un compte</a>
        </p>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
