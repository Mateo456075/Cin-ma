<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Espace employé" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <h1>Bienvenue <c:out value="${sessionScope.usere.login}" />, dans votre espace employé</h1>
    <section class="dashboard-grid">
        <a class="card dashboard-card" href="${pageContext.request.contextPath}/creerfilm">
            <span class="dashboard-icon" aria-hidden="true">＋</span>
            <h2>Ajouter un film</h2>
            <p>Enregistrer un nouveau film dans le catalogue.</p>
        </a>
        <a class="card dashboard-card" href="${pageContext.request.contextPath}/listeFilms">
            <span class="dashboard-icon" aria-hidden="true">▤</span>
            <h2>Gérer les films</h2>
            <p>Consulter, modifier ou supprimer les films.</p>
        </a>
        <a class="card dashboard-card" href="${pageContext.request.contextPath}/nouvelleSeance">
            <span class="dashboard-icon" aria-hidden="true">＋</span>
            <h2>Ajouter une séance</h2>
            <p>Programmer un film dans une salle.</p>
        </a>
        <a class="card dashboard-card" href="${pageContext.request.contextPath}/employe/listeSeances">
            <span class="dashboard-icon" aria-hidden="true">◷</span>
            <h2>Voir les séances</h2>
            <p>Consulter la programmation du cinéma.</p>
        </a>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
