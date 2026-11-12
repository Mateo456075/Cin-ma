<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Créer un compte" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="form-container">
        <h1>Créer un compte client</h1>
        <p class="form-intro">Rejoignez Cynemma en quelques instants.</p>
        <p class="form-hint">Tous les champs sont obligatoires.</p>
        <c:if test="${not empty errors}">
            <p class="error-message" role="alert"><c:out value="${errors}" /></p>
        </c:if>
        <form action="${pageContext.request.contextPath}/nouveaucompte" method="post">
            <label for="nom">Nom</label>
            <input type="text" id="nom" name="nom" autocomplete="name" required>
            
            <label for="login">Login</label>
            <input type="text" id="login" name="login" autocomplete="username" required>

            <label for="mdp">Mot de passe</label>
            <input type="password" id="mdp" name="mdp" autocomplete="new-password" required>

            <label for="confirm_mdp">Confirmation du mot de passe</label>
            <input type="password" id="confirm_mdp" name="confirm_mdp" autocomplete="new-password" required>

            <label for="email">Email</label>
            <input type="email" id="email" name="email" autocomplete="email" required>

            <div class="form-actions">
                <button class="btn" type="submit">Créer mon compte</button>
                <button class="btn btn-secondary" type="reset">Effacer</button>
            </div>
        </form>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
