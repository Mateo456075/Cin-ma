<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="jakarta.tags.core" prefix="c" %>
<c:set var="pageTitle" value="Modifier un rôle" scope="request" />
<jsp:include page="/WEB-INF/includes/header.jsp" />

<main class="page-content" id="main-content">
    <section class="form-container">
        <p class="eyebrow">Administration</p>
        <h1>Modifier un rôle</h1>
        <p class="form-intro">Compte : <strong><c:out value="${userLogin}" /></strong></p>
        <p class="form-hint">Le rôle est obligatoire.</p>
        <form action="${pageContext.request.contextPath}/modifierrole" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
            <input type="hidden" name="login" value="<c:out value='${userLogin}' />">
            <label for="newRole">Nouveau rôle</label>
            <select name="newRole" id="newRole" required>
                <option value="CLIENT" ${currentRole == 'CLIENT' ? 'selected' : ''}>Client</option>
                <option value="ADMIN" ${currentRole == 'ADMIN' ? 'selected' : ''}>Administrateur</option>
                <option value="EMPLOYER" ${currentRole == 'EMPLOYER' ? 'selected' : ''}>Employé</option>
            </select>
            <div class="form-actions">
                <button class="btn" type="submit">Mettre à jour</button>
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admins">Annuler</a>
            </div>
        </form>
    </section>
</main>

<jsp:include page="/WEB-INF/includes/footer.jsp" />
