<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Espace client" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />
    <main class="page-content" id="main-content">
        <p class="eyebrow">Espace client</p>
        <h1>Bienvenue <c:out value="${sessionScope.usere.login}" /></h1>
        <h2>Films à l'affiche</h2>
        <div class="films-container">
            <c:choose>
                <c:when test="${not empty films}">
                    <c:forEach var="film" items="${films}">
                        <article class="card film-card">
                            <a href="${pageContext.request.contextPath}/client/listeSeances?filmId=${film.id}">
                                <img src="${pageContext.request.contextPath}/${film.affiche}" alt="">
                                <h3><c:out value="${film.titre}" /></h3>
                            </a>
                        </article>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                <p>Aucun film n'est disponible pour le moment.</p>
                </c:otherwise>
            </c:choose>
        </div>
    </main>
<jsp:include page="/WEB-INF/includes/footer.jsp" />