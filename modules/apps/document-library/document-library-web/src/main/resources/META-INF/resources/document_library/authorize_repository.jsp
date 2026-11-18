<%--
/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/document_library/init.jsp" %>

<%
DLFolder dlFolder = (DLFolder)request.getAttribute(DLFolder.class.getName());
%>

<portlet:renderURL var="viewFolderURL">
	<portlet:param name="mvcRenderCommandName" value="/document_library/view_folder" />
	<portlet:param name="folderId" value="<%= String.valueOf(dlFolder.getFolderId()) %>" />
</portlet:renderURL>

<clay:link
	displayType="primary"
	href='<%= HttpComponentsUtil.addParameter(viewFolderURL, "saveLastPath", false) %>'
	label='<%= LanguageUtil.format(request, "authorize-x", dlFolder.getName(), false) %>'
	type="button"
/>