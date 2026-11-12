<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Ajouter un film" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="form-container">
        <p class="eyebrow">Catalogue</p>
        <h1>Ajouter un film</h1>
        <p class="form-hint">Tous les champs sont obligatoires.</p>
        <form action="${pageContext.request.contextPath}/creerfilm"
              method="post" enctype="multipart/form-data">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
            <div class="form-grid">
                <div>
                    <label for="titre">Titre du film</label>
                    <input type="text" id="titre" name="titre" required>
                </div>
                <div>
                    <label for="realisateur">Réalisateur</label>
                    <input type="text" id="realisateur" name="realisateur" required>
                </div>
                <div>
                    <label for="annee">Année de sortie</label>
                    <input type="number" id="annee" name="annee" min="1895" max="2100" required>
                </div>
                <div>
                    <label for="temps">Durée (minutes)</label>
                    <input type="number" id="temps" name="temps" min="1" required>
                </div>
                <div>
                    <label for="genre">Genre</label>
                    <select id="genre" name="genre" required>
                        <option value="">-- Sélectionnez un genre --</option>
                        <option value="Action">Action</option>
                        <option value="Animation">Animation</option>
                        <option value="Aventure">Aventure</option>
                        <option value="Comédie">Comédie</option>
                        <option value="Documentaire">Documentaire</option>
                        <option value="Drame">Drame</option>
                        <option value="Fantastique">Fantastique</option>
                        <option value="Horreur">Horreur</option>
                        <option value="Policier / Thriller">Policier / Thriller</option>
                        <option value="Romance">Romance</option>
                        <option value="Science-fiction">Science-fiction</option>
                        <option value="Western">Western</option>
                    </select>
                </div>
                <div>
                    <label for="affiche">Affiche</label>
                    <input type="file" id="affiche" name="affiche" accept="image/*" required>
                </div>
            </div>
            <div class="form-actions">
                <button class="btn" type="submit">Enregistrer le film</button>
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/listeFilms">Annuler</a>
            </div>
        </form>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
