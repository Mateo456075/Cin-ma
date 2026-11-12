<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Modifier un film" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="form-container">
        <p class="eyebrow">Catalogue</p>
        <h1>Modifier le film</h1>
        <p class="form-hint">Tous les champs sont obligatoires.</p>
        <form action="${pageContext.request.contextPath}/modifierFilm" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
            <input type="hidden" name="id" value="${film.id}">
            <input type="hidden" name="ancienneAffiche" value="${film.affiche}">

            <div class="form-grid">
                <div>
                    <label for="titre">Titre du film</label>
                    <input type="text" id="titre" name="titre" value="<c:out value='${film.titre}' />" required>
                </div>
                <div>
                    <label for="realisateur">Réalisateur</label>
                    <input type="text" id="realisateur" name="realisateur" value="<c:out value='${film.realisateur}' />" required>
                </div>
                <div>
                    <label for="annee">Année de sortie</label>
                    <input type="number" id="annee" name="annee" min="1895" max="2100" value="${film.annee}" required>
                </div>
                <div>
                    <label for="temps">Durée (minutes)</label>
                    <input type="number" id="temps" name="temps" min="1" value="${film.dureeMinutes}" required>
                </div>
                <div>
                    <label for="genre">Genre</label>
                    <select id="genre" name="genre" required>
                        <c:forEach var="genreOption"
                                   items="${['Action','Animation','Aventure','Comédie','Documentaire','Drame','Fantastique','Horreur','Policier / Thriller','Romance','Science-fiction','Western']}">
                            <option value="${genreOption}" ${film.genre == genreOption ? 'selected' : ''}>
                                <c:out value="${genreOption}" />
                            </option>
                        </c:forEach>
                    </select>
                </div>
            </div>
            <div class="form-actions">
                <button class="btn" type="submit">Mettre à jour</button>
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/listeFilms">Annuler</a>
            </div>
        </form>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
