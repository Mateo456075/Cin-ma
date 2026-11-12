<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Espace visiteur" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="card welcome-card">
        <p class="eyebrow">Bienvenue</p>
        <h1>Bonjour <c:out value="${sessionScope.usere.login}" default="Visiteur" /></h1>
        <p>Votre compte n'a pas encore de rôle spécifique attribué.</p>
        <a class="btn" href="${pageContext.request.contextPath}/accueil">Retour à l'accueil</a>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
