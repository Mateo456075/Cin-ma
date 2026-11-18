<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Vérifier un film TMDB" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="form-container">
        <p class="eyebrow">Import assisté · ID TMDB <c:out value="${movie.tmdbId}" /></p>
        <h1>Vérifier les informations du film</h1>
        <p class="form-hint">
            Les informations sont préremplies depuis TMDB. Vérifiez-les et complétez les champs avant l'ajout.
        </p>

        <c:if test="${not empty error}">
            <p class="error-message" role="alert"><c:out value="${error}" /></p>
        </c:if>

        <c:if test="${not empty movie.overview}">
            <p><c:out value="${movie.overview}" /></p>
        </c:if>
        <c:if test="${not empty movie.posterUrl}">
            <img class="table-poster" src="<c:out value='${movie.posterUrl}' />"
                 alt="Affiche TMDB de <c:out value='${movie.title}' />">
        </c:if>
        <c:if test="${empty movie.posterPath}">
            <p class="error-message" role="alert">
                Aucune affiche TMDB n'est disponible. Vous pouvez utiliser la création manuelle du film.
            </p>
        </c:if>

        <form action="${pageContext.request.contextPath}/employe/tmdb/catalogue" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
            <input type="hidden" name="tmdbId" value="<c:out value='${movie.tmdbId}' />">

            <div class="form-grid">
                <div>
                    <label for="title">Titre</label>
                    <input type="text" id="title" name="title" maxlength="255"
                           value="<c:out value='${formTitle}' />" required>
                </div>
                <div>
                    <label for="director">Réalisateur</label>
                    <input type="text" id="director" name="director" maxlength="255"
                           value="<c:out value='${formDirector}' />" required>
                </div>
                <div>
                    <label for="year">Année de sortie</label>
                    <input type="number" id="year" name="year" min="1895" max="2100"
                           value="<c:out value='${formYear}' />" required>
                </div>
                <div>
                    <label for="duration">Durée (minutes)</label>
                    <input type="number" id="duration" name="duration" min="1"
                           value="<c:out value='${formDuration}' />" required>
                </div>
                <div>
                    <label for="genre">Genre(s)</label>
                    <input type="text" id="genre" name="genre" maxlength="100"
                           value="<c:out value='${formGenre}' />" required>
                </div>
            </div>

            <div class="form-actions">
                <c:choose>
                    <c:when test="${empty movie.posterPath}">
                        <button class="btn" type="submit" disabled>Confirmer l'ajout au catalogue</button>
                    </c:when>
                    <c:otherwise>
                        <button class="btn" type="submit">Confirmer l'ajout au catalogue</button>
                    </c:otherwise>
                </c:choose>
                <c:url var="suggestionsUrl" value="/employe/tmdb/catalogue" />
                <a class="btn btn-secondary" href="${suggestionsUrl}">Retour aux suggestions</a>
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/creerfilm">
                    Création manuelle
                </a>
            </div>
        </form>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
