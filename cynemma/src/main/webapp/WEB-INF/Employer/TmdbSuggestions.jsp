<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Suggestions TMDB" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <div class="page-heading">
        <div>
            <p class="eyebrow">Catalogue</p>
            <h1>Nouveautés TMDB</h1>
            <p>Sélectionnez un film, vérifiez ses informations, puis confirmez son ajout.</p>
        </div>
        <a class="btn btn-secondary" href="${pageContext.request.contextPath}/listeFilms">Retour au catalogue</a>
    </div>

    <c:choose>
        <c:when test="${not empty movies}">
            <section class="dashboard-grid" aria-label="Films actuellement en salle">
                <c:forEach var="movie" items="${movies}">
                    <article class="card">
                        <c:if test="${not empty movie.posterUrl}">
                            <img class="table-poster" src="<c:out value='${movie.posterUrl}' />"
                                 alt="Affiche de <c:out value='${movie.title}' />">
                        </c:if>
                        <h2><c:out value="${movie.title}" /></h2>
                        <p>
                            <c:out value="${movie.releaseDate}" />
                            · ID TMDB <c:out value="${movie.tmdbId}" />
                        </p>
                        <p><c:out value="${movie.overview}" /></p>
                        <c:url var="reviewUrl" value="/employe/tmdb/catalogue">
                            <c:param name="tmdbId" value="${movie.tmdbId}" />
                        </c:url>
                        <a class="btn" href="${reviewUrl}">Vérifier et préparer l'ajout</a>
                    </article>
                </c:forEach>
            </section>
        </c:when>
        <c:otherwise>
            <p class="empty-state">Aucun film n'est proposé par TMDB pour cette page.</p>
        </c:otherwise>
    </c:choose>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
