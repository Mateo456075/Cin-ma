<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Films à l'affiche" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />
<main class="page-content" id="main-content">
    <section class="cinema-hero" aria-labelledby="home-title">
        <div class="hero-copy">
            <p class="eyebrow">Cynemma · Votre cinéma</p>
            <h1>Les histoires prennent vie dans le noir.</h1>
            <p class="hero-description">Découvrez les films à l'affiche et choisissez votre prochaine séance. L'expérience commence ici.</p>
            <a href="${pageContext.request.contextPath}/films" class="btn">Parcourir les films</a>
        </div>
        <div class="hero-visual" aria-hidden="true">
            <div class="featured-poster-wrapper">
                <div class="featured-poster">
                    <div class="poster-glass"></div>
                        <img src="https://images.unsplash.com/photo-1536440136628-849c177e76a1?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80" 
                        alt="Film à l'affiche" 
                        style="width: 100%; height: 100%; object-fit: cover; z-index: 0;">
                    </div>
                <div class="poster-reflection"></div>
            </div>
        </div>
        <div class="hero-visual" aria-hidden="true">
            <span class="theater-screen"></span>
            <span class="theater-beam"></span>
            <div class="theater-seats">
                <div class="seat-row">
                    <span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span>
                </div>
                <div class="seat-row">
                    <span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span>
                </div>
                <div class="seat-row">
                    <span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span><span class="seat"></span>
                </div>
            </div>
        </div>
    </section>

    <section id="catalogue" class="catalogue-section" aria-labelledby="catalogue-title">
        <div class="section-heading">
            <div>
                <p class="eyebrow">À découvrir</p>
                <h2 id="catalogue-title">Films à l'affiche</h2>
            </div>
            <p class="section-description">Une sélection pour votre prochaine séance.</p>
        </div>
        <div class="films-container">
            <c:choose>
                <c:when test="${not empty films}">
                    <c:forEach items="${films}" var="film">
                        <c:url var="afficheUrl" value="/${film.affiche}" />
                        <article class="card film-card film-card-static" aria-labelledby="film-title-${film.id}">
                            <img src="${afficheUrl}" alt="" width="160" loading="lazy">
                            <div class="film-card-content">
                                <h3 id="film-title-${film.id}"><c:out value="${film.titre}" /></h3>
                                <p><c:out value="${film.genre}" /> <span aria-hidden="true">·</span> <c:out value="${film.annee}" /></p>
                                <p>Durée : <c:out value="${film.dureeMinutes}" /> min</p>
                            </div>
                        </article>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <p class="empty-state">Aucun film n'est actuellement à l'affiche.</p>
                </c:otherwise>
            </c:choose>
        </div>
    </section>
</main>
<jsp:include page="/WEB-INF/includes/footer.jsp" />
