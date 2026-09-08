/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.layout.content.page.editor.web.internal.portlet.action;

import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.renderer.DefaultFragmentRendererContext;
import com.liferay.fragment.service.FragmentEntryLinkLocalService;
import com.liferay.layout.content.page.editor.constants.ContentPageEditorPortletKeys;
import com.liferay.layout.content.page.editor.web.internal.manager.FragmentEntryLinkManager;
import com.liferay.layout.content.page.editor.web.internal.util.layout.structure.LayoutStructureUtil;
import com.liferay.layout.display.page.LayoutDisplayPageProvider;
import com.liferay.layout.util.structure.LayoutStructure;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.portlet.JSONPortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCResourceCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.WebKeys;

import jakarta.portlet.ResourceRequest;
import jakarta.portlet.ResourceResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Víctor Galán
 */
@Component(
	property = {
		"jakarta.portlet.name=" + ContentPageEditorPortletKeys.CONTENT_PAGE_EDITOR_PORTLET,
		"mvc.command.name=/layout_content_page_editor/get_fragment_entry_links"
	},
	service = MVCResourceCommand.class
)
public class GetFragmentEntryLinksMVCResourceCommand
	extends BaseMVCResourceCommand {

	@Override
	protected void doServeResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		JSONArray fragmentEntryLinksJSONArray = _jsonFactory.createJSONArray();

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long segmentsExperienceId = ParamUtil.getLong(
			resourceRequest, "segmentsExperienceId");

		JSONArray jsonArray = _jsonFactory.createJSONArray(
			ParamUtil.getString(resourceRequest, "data"));

		LayoutStructure layoutStructure =
			LayoutStructureUtil.getLayoutStructure(
				themeDisplay.getScopeGroupId(), themeDisplay.getPlid(),
				segmentsExperienceId);

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);

			fragmentEntryLinksJSONArray.put(
				_getFragmentEntryLinkJSONObject(
					jsonObject.getLong("fragmentEntryLinkId"),
					jsonObject.getString("itemClassName"),
					jsonObject.getLong("itemClassPK"),
					jsonObject.getString("itemExternalReferenceCode"),
					ParamUtil.getString(
						resourceRequest, "languageId",
						themeDisplay.getLanguageId()),
					layoutStructure, resourceRequest, resourceResponse));
		}

		JSONPortletResponseUtil.writeJSON(
			resourceRequest, resourceResponse, fragmentEntryLinksJSONArray);
	}

	private JSONObject _getFragmentEntryLinkJSONObject(
			long fragmentEntryLinkId, String itemClassName, long itemClassPK,
			String itemExternalReferenceCode, String languageId,
			LayoutStructure layoutStructure, ResourceRequest resourceRequest,
			ResourceResponse resourceResponse)
		throws Exception {

		JSONObject jsonObject = _jsonFactory.createJSONObject();

		FragmentEntryLink fragmentEntryLink =
			_fragmentEntryLinkLocalService.fetchFragmentEntryLink(
				fragmentEntryLinkId);

		if (fragmentEntryLink == null) {
			return jsonObject;
		}

		DefaultFragmentRendererContext defaultFragmentRendererContext =
			new DefaultFragmentRendererContext(fragmentEntryLink);

		defaultFragmentRendererContext.setLocale(
			LocaleUtil.fromLanguageId(languageId));

		HttpServletRequest httpServletRequest = _portal.getHttpServletRequest(
			resourceRequest);

		LayoutDisplayPageProvider<?> currentLayoutDisplayPageProvider =
			_fragmentEntryLinkManager.applyItemContext(
				defaultFragmentRendererContext, itemClassName, itemClassPK,
				itemExternalReferenceCode, httpServletRequest);

		try {
			jsonObject =
				_fragmentEntryLinkManager.getFragmentEntryLinkJSONObject(
					defaultFragmentRendererContext, fragmentEntryLink,
					_portal.getHttpServletRequest(resourceRequest),
					_portal.getHttpServletResponse(resourceResponse),
					layoutStructure);
		}
		finally {
			_fragmentEntryLinkManager.resetItemContext(
				httpServletRequest, currentLayoutDisplayPageProvider);
		}

		if (SessionErrors.contains(
				httpServletRequest, "fragmentEntryContentInvalid")) {

			jsonObject.put("error", true);

			SessionErrors.clear(httpServletRequest);
		}

		return jsonObject;
	}

	@Reference
	private FragmentEntryLinkLocalService _fragmentEntryLinkLocalService;

	@Reference
	private FragmentEntryLinkManager _fragmentEntryLinkManager;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Portal _portal;

}