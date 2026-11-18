/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.document.library.web.internal.portlet.action;

import com.liferay.document.library.constants.DLPortletKeys;
import com.liferay.document.library.kernel.exception.NoSuchFolderException;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.document.library.kernel.service.DLFolderLocalService;
import com.liferay.document.library.repository.authorization.capability.AuthorizationCapability;
import com.liferay.document.library.web.internal.constants.DLWebKeys;
import com.liferay.document.library.web.internal.display.context.DLAdminDisplayContext;
import com.liferay.document.library.web.internal.display.context.DLAdminDisplayContextProvider;
import com.liferay.document.library.web.internal.display.context.DLAdminManagementToolbarDisplayContext;
import com.liferay.document.library.web.internal.display.context.DLViewFileEntryMetadataSetsDisplayContext;
import com.liferay.document.library.web.internal.helper.DLTrashHelper;
import com.liferay.dynamic.data.mapping.service.DDMStructureLinkLocalService;
import com.liferay.dynamic.data.mapping.service.DDMStructureService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.constants.MVCRenderConstants;
import com.liferay.portal.kernel.portlet.toolbar.contributor.PortletToolbarContributor;
import com.liferay.portal.kernel.repository.Repository;
import com.liferay.portal.kernel.repository.RepositoryProviderUtil;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.WebKeys;

import jakarta.portlet.PortletException;
import jakarta.portlet.RenderRequest;
import jakarta.portlet.RenderResponse;

import java.io.IOException;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Iván Zaera
 */
@Component(
	property = {
		"jakarta.portlet.name=" + DLPortletKeys.DOCUMENT_LIBRARY,
		"jakarta.portlet.name=" + DLPortletKeys.DOCUMENT_LIBRARY_ADMIN,
		"mvc.command.name=/", "mvc.command.name=/document_library/view",
		"mvc.command.name=/document_library/view_folder"
	},
	service = MVCRenderCommand.class
)
public class DLViewMVCRenderCommand extends BaseFolderMVCRenderCommand {

	@Override
	public String render(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws PortletException {

		try {
			String authorizationMVCPath = _getAuthorizationMVCPath(
				renderRequest, renderResponse);

			if (authorizationMVCPath != null) {
				return authorizationMVCPath;
			}

			renderRequest.setAttribute(
				DLWebKeys.DOCUMENT_LIBRARY_PORTLET_TOOLBAR_CONTRIBUTOR,
				_dlPortletToolbarContributor);
			renderRequest.setAttribute(
				DLWebKeys.
					DOCUMENT_LIBRARY_VIEW_FILE_ENTRY_METADATA_SETS_DISPLAY_CONTEXT,
				new DLViewFileEntryMetadataSetsDisplayContext(
					_ddmStructureLinkLocalService, _ddmStructureService,
					_portal.getLiferayPortletRequest(renderRequest),
					_portal.getLiferayPortletResponse(renderResponse),
					_portal));
			renderRequest.setAttribute(
				WebKeys.DOCUMENT_LIBRARY_FOLDER, _getFolder(renderRequest));

			DLAdminDisplayContext dlAdminDisplayContext =
				_dlAdminDisplayContextProvider.getDLAdminDisplayContext(
					_portal.getHttpServletRequest(renderRequest));

			renderRequest.setAttribute(
				DLAdminDisplayContext.class.getName(), dlAdminDisplayContext);
			renderRequest.setAttribute(
				DLAdminManagementToolbarDisplayContext.class.getName(),
				_dlAdminDisplayContextProvider.
					getDLAdminManagementToolbarDisplayContext(
						_portal.getHttpServletRequest(renderRequest),
						dlAdminDisplayContext));

			return super.render(renderRequest, renderResponse);
		}
		catch (PortalException portalException) {
			if (portalException instanceof
					PrincipalException.MustBeAuthenticated) {

				SessionErrors.add(
					renderRequest, portalException.getClass(), portalException);
			}
			else {
				SessionErrors.add(
					renderRequest, "repositoryPingFailed", portalException);
			}

			SessionMessages.add(
				renderRequest,
				_portal.getPortletId(renderRequest) +
					SessionMessages.KEY_SUFFIX_HIDE_DEFAULT_ERROR_MESSAGE);

			return "/document_library/error.jsp";
		}
		catch (IOException ioException) {
			throw new PortletException(ioException);
		}
	}

	@Override
	protected DLTrashHelper getDLTrashHelper() {
		return _dlTrashHelper;
	}

	@Override
	protected String getPath() {
		return "/document_library/view.jsp";
	}

	private String _getAuthorizationMVCPath(
			RenderRequest renderRequest, RenderResponse renderResponse)
		throws IOException, PortalException {

		long folderId = ParamUtil.getLong(renderRequest, "folderId");

		long pingFolderId = folderId;

		if ((pingFolderId <= 0) &&
			!ParamUtil.getBoolean(renderRequest, "ignoreRootFolder")) {

			try {
				pingFolderId = ActionUtil.getRootFolderId(
					_portal.getHttpServletRequest(renderRequest));
			}
			catch (NoSuchFolderException noSuchFolderException) {
				if (_log.isDebugEnabled()) {
					_log.debug(noSuchFolderException);
				}

				return null;
			}
		}

		if (pingFolderId <= 0) {
			return null;
		}

		DLFolder dlFolder = _dlFolderLocalService.fetchDLFolder(pingFolderId);

		if ((dlFolder == null) || !dlFolder.isMountPoint()) {
			return null;
		}

		Repository repository = RepositoryProviderUtil.getRepository(
			dlFolder.getRepositoryId());

		if (!repository.isCapabilityProvided(AuthorizationCapability.class)) {
			if (folderId > 0) {
				_dlAppService.getFileEntriesCount(
					dlFolder.getRepositoryId(), dlFolder.getFolderId());
			}

			return null;
		}

		ThemeDisplay themeDisplay = (ThemeDisplay)renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		if (!themeDisplay.isSignedIn()) {
			throw new PrincipalException.MustBeAuthenticated(
				themeDisplay.getUserId());
		}

		AuthorizationCapability authorizationCapability =
			repository.getCapability(AuthorizationCapability.class);

		if ((folderId <= 0) &&
			authorizationCapability.hasCustomRedirectFlow(
				renderRequest, renderResponse)) {

			renderRequest.setAttribute(DLFolder.class.getName(), dlFolder);

			return "/document_library/authorize_repository.jsp";
		}

		authorizationCapability.authorize(renderRequest, renderResponse);

		if (authorizationCapability.hasCustomRedirectFlow(
				renderRequest, renderResponse)) {

			return MVCRenderConstants.MVC_PATH_VALUE_SKIP_DISPATCH;
		}

		return null;
	}

	private Folder _getFolder(RenderRequest renderRequest)
		throws PortalException {

		long folderId = ParamUtil.getLong(renderRequest, "folderId");

		if (folderId == DLFolderConstants.DEFAULT_PARENT_FOLDER_ID) {
			return null;
		}

		return _dlAppService.getFolder(folderId);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DLViewMVCRenderCommand.class);

	@Reference
	private DDMStructureLinkLocalService _ddmStructureLinkLocalService;

	@Reference
	private DDMStructureService _ddmStructureService;

	@Reference
	private DLAdminDisplayContextProvider _dlAdminDisplayContextProvider;

	@Reference
	private DLAppService _dlAppService;

	@Reference
	private DLFolderLocalService _dlFolderLocalService;

	@Reference(
		target = "(jakarta.portlet.name=" + DLPortletKeys.DOCUMENT_LIBRARY + ")"
	)
	private PortletToolbarContributor _dlPortletToolbarContributor;

	@Reference
	private DLTrashHelper _dlTrashHelper;

	@Reference
	private Portal _portal;

}