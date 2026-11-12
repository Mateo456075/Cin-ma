<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Administration" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content admin-main" id="main-content">
    <div class="page-heading">
        <div>
            <p class="eyebrow">Tableau de bord</p>
            <h1>Bienvenue, <c:out value="${sessionScope.usere.login}" /></h1>
        </div>
        <p class="section-description">Espace d'administration du cinéma</p>
    </div>

    <section class="card" style="margin-bottom: var(--space-6);">
        
        <div class="section-heading">
            <h2>Liste des utilisateurs inscrits</h2>
        </div>
        
        <div class="table-container" role="region" aria-label="Comptes utilisateurs" tabindex="0">
            <table>
                <caption class="sr-only">Comptes utilisateurs</caption>
                <thead>
                    <tr>
                        <th scope="col">Login</th>
                        <th scope="col">Email</th>
                        <th scope="col">Rôle</th>
                        <th scope="col">Action</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${not empty listUsers}">
                            <c:forEach var="user" items="${listUsers}">
                                <c:url var="editRoleUrl" value="/modifierrole">
                                    <c:param name="login" value="${user.login}" />
                                    <c:param name="role" value="${user.typeUtilisateur}" />
                                </c:url>
                                <tr>
                                    <td><c:out value="${user.login}" /></td>
                                    <td><c:out value="${user.email}" /></td>
                                    <td><c:out value="${user.typeUtilisateur}" /></td>
                                    <td>
                                        <div class="table-actions">
                                            <a class="btn btn-secondary" href="${editRoleUrl}">Modifier</a>
                                            
                                            <form action="${pageContext.request.contextPath}/supprimerUtilisateur" method="post"
                                                onsubmit="return confirm('Êtes-vous sûr de vouloir supprimer ce compte ?');">
                                                <input type="hidden" name="login" value="<c:out value='${user.login}' />">
                                                <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                                                <button class="btn btn-danger" type="submit">Supprimer</button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="4">Aucun utilisateur trouvé.</td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </section>

    <section class="card">
        <h2>Liste des salles de cinéma</h2>
        <p class="actions">
            <a class="btn" href="${pageContext.request.contextPath}/creerSalle">+ Ajouter une salle</a>
        </p>
        <div class="table-container" role="region" aria-label="Salles de cinéma, défilement horizontal possible" tabindex="0">
            <table>
                <caption class="sr-only">Salles de cinéma</caption>
                <thead>
                    <tr>
                        <th scope="col">Nom de la salle</th>
                        <th scope="col">Capacité (places)</th>
                        <th scope="col">Action</th>
                    </tr>
                </thead>
                <tbody>
                    <c:choose>
                        <c:when test="${not empty listSalles}">
                            <c:forEach var="salle" items="${listSalles}">
                                <tr>
                                    <td><c:out value="${salle.nom}" /></td>
                                    <td><c:out value="${salle.capacite}" /></td>
                                    <td><span class="text-muted">À venir</span></td>
                                </tr>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td colspan="3">Aucune salle trouvée.</td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                </tbody>
            </table>
        </div>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
