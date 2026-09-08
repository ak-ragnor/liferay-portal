/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.layout.content.page.editor.web.internal.portlet.action;

import com.liferay.asset.kernel.AssetRendererFactoryRegistryUtil;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.asset.util.LinkedAssetEntryIdsUtil;
import com.liferay.fragment.constants.FragmentEntryLinkConstants;
import com.liferay.fragment.listener.FragmentEntryLinkListener;
import com.liferay.fragment.listener.FragmentEntryLinkListenerRegistry;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.processor.DefaultFragmentEntryProcessorContext;
import com.liferay.fragment.processor.FragmentEntryProcessorContext;
import com.liferay.fragment.processor.FragmentEntryProcessorRegistry;
import com.liferay.fragment.renderer.DefaultFragmentRendererContext;
import com.liferay.fragment.service.FragmentEntryLinkService;
import com.liferay.fragment.util.configuration.FragmentConfigurationField;
import com.liferay.fragment.util.configuration.FragmentEntryConfigurationParser;
import com.liferay.info.constants.InfoDisplayWebKeys;
import com.liferay.info.item.ClassPKInfoItemIdentifier;
import com.liferay.info.item.ERCInfoItemIdentifier;
import com.liferay.info.item.InfoItemIdentifier;
import com.liferay.info.item.InfoItemReference;
import com.liferay.info.item.InfoItemServiceRegistry;
import com.liferay.info.item.provider.InfoItemDetailsProvider;
import com.liferay.info.item.provider.InfoItemObjectProvider;
import com.liferay.layout.content.page.editor.constants.ContentPageEditorPortletKeys;
import com.liferay.layout.content.page.editor.web.internal.manager.FragmentEntryLinkManager;
import com.liferay.layout.content.page.editor.web.internal.util.layout.structure.LayoutStructureUtil;
import com.liferay.layout.display.page.LayoutDisplayPageProvider;
import com.liferay.layout.display.page.LayoutDisplayPageProviderRegistry;
import com.liferay.layout.display.page.constants.LayoutDisplayPageWebKeys;
import com.liferay.layout.util.structure.LayoutStructure;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;

import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Eudaldo Alonso
 */
@Component(
	property = {
		"jakarta.portlet.name=" + ContentPageEditorPortletKeys.CONTENT_PAGE_EDITOR_PORTLET,
		"mvc.command.name=/layout_content_page_editor/update_configuration_values"
	},
	service = MVCActionCommand.class
)
public class UpdateConfigurationValuesMVCActionCommand
	extends BaseContentPageEditorTransactionalMVCActionCommand {

	@Override
	protected JSONObject doTransactionalCommand(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		return _processUpdateConfigurationValues(actionRequest, actionResponse);
	}

	private void _addDefaultEditableValues(
		JSONObject defaultEditableValuesJSONObject,
		JSONObject editableValuesJSONObject) {

		Set<String> fragmentEntryProcessorKeys =
			editableValuesJSONObject.keySet();

		for (String fragmentEntryProcessorKey : fragmentEntryProcessorKeys) {
			JSONObject editableFragmentEntryProcessorJSONObject =
				editableValuesJSONObject.getJSONObject(
					fragmentEntryProcessorKey);

			JSONObject defaultEditableFragmentEntryProcessorJSONObject =
				defaultEditableValuesJSONObject.getJSONObject(
					fragmentEntryProcessorKey);

			if (defaultEditableFragmentEntryProcessorJSONObject == null) {
				defaultEditableValuesJSONObject.put(
					fragmentEntryProcessorKey,
					editableFragmentEntryProcessorJSONObject);

				continue;
			}

			Set<String> editableIds =
				editableFragmentEntryProcessorJSONObject.keySet();

			for (String editableId : editableIds) {
				if (!defaultEditableFragmentEntryProcessorJSONObject.has(
						editableId)) {

					defaultEditableFragmentEntryProcessorJSONObject.put(
						editableId,
						editableFragmentEntryProcessorJSONObject.get(
							editableId));
				}
			}
		}
	}

	private void _addLinkedAssetEntryId(
		String className, long classPK, HttpServletRequest httpServletRequest) {

		AssetRendererFactory<?> assetRendererFactory =
			AssetRendererFactoryRegistryUtil.getAssetRendererFactoryByClassName(
				className);

		if (assetRendererFactory == null) {
			return;
		}

		try {
			AssetEntry assetEntry = assetRendererFactory.getAssetEntry(
				className, classPK);

			if (assetEntry != null) {
				LinkedAssetEntryIdsUtil.addLinkedAssetEntryId(
					httpServletRequest, assetEntry.getEntryId());
			}
		}
		catch (PortalException portalException) {
			if (_log.isDebugEnabled()) {
				_log.debug(portalException);
			}
		}
	}

	private JSONObject _getDefaultEditableValuesJSONObject(
			ActionRequest actionRequest, ActionResponse actionResponse,
			FragmentEntryLink fragmentEntryLink, ThemeDisplay themeDisplay)
		throws Exception {

		JSONObject defaultEditableValuesJSONObject =
			_jsonFactory.createJSONObject();

		JSONObject configurationJSONObject =
			fragmentEntryLink.getConfigurationJSONObject();

		for (Locale locale :
				_getLocales(configurationJSONObject, themeDisplay)) {

			FragmentEntryProcessorContext fragmentEntryProcessorContext =
				new DefaultFragmentEntryProcessorContext(
					fragmentEntryLink.getCompanyId(),
					_portal.getHttpServletRequest(actionRequest),
					_portal.getHttpServletResponse(actionResponse), locale,
					FragmentEntryLinkConstants.EDIT,
					fragmentEntryLink.getGroupId());

			_addDefaultEditableValues(
				defaultEditableValuesJSONObject,
				_fragmentEntryProcessorRegistry.
					getDefaultEditableValuesJSONObject(
						_fragmentEntryProcessorRegistry.
							processFragmentEntryLinkHTML(
								fragmentEntryLink,
								fragmentEntryProcessorContext),
						configurationJSONObject));
		}

		return defaultEditableValuesJSONObject;
	}

	private Collection<Locale> _getLocales(
			JSONObject configurationJSONObject, ThemeDisplay themeDisplay)
		throws Exception {

		Locale locale = _portal.getSiteDefaultLocale(
			themeDisplay.getSiteGroupId());

		if (!ListUtil.exists(
				_fragmentEntryConfigurationParser.
					getFragmentConfigurationFields(configurationJSONObject),
				FragmentConfigurationField::isLocalizable)) {

			return Collections.singletonList(locale);
		}

		Collection<Locale> locales = new LinkedHashSet<>();

		locales.add(locale);

		locales.addAll(
			_language.getAvailableLocales(themeDisplay.getSiteGroupId()));

		return locales;
	}

	private JSONObject _mergeEditableValuesJSONObject(
			JSONObject defaultEditableValuesJSONObject, String editableValues)
		throws Exception {

		return _fragmentEntryLinkManager.mergeEditableValuesJSONObject(
			defaultEditableValuesJSONObject,
			_jsonFactory.createJSONObject(editableValues));
	}

	private JSONObject _processUpdateConfigurationValues(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		long fragmentEntryLinkId = ParamUtil.getLong(
			actionRequest, "fragmentEntryLinkId");

		String editableValues = ParamUtil.getString(
			actionRequest, "editableValues");

		String itemClassName = ParamUtil.getString(
			actionRequest, "itemClassName");
		long itemClassPK = ParamUtil.getLong(actionRequest, "itemClassPK");
		String itemExternalReferenceCode = ParamUtil.getString(
			actionRequest, "itemExternalReferenceCode");

		FragmentEntryLink fragmentEntryLink =
			_fragmentEntryLinkService.updateFragmentEntryLink(
				fragmentEntryLinkId, editableValues);

		JSONObject newEditableValuesJSONObject = _mergeEditableValuesJSONObject(
			_getDefaultEditableValuesJSONObject(
				actionRequest, actionResponse, fragmentEntryLink, themeDisplay),
			editableValues);

		fragmentEntryLink = _fragmentEntryLinkService.updateFragmentEntryLink(
			fragmentEntryLinkId, newEditableValuesJSONObject.toString());

		for (FragmentEntryLinkListener fragmentEntryLinkListener :
				_fragmentEntryLinkListenerRegistry.
					getFragmentEntryLinkListeners()) {

			fragmentEntryLinkListener.
				onUpdateFragmentEntryLinkConfigurationValues(fragmentEntryLink);
		}

		LayoutStructure layoutStructure =
			LayoutStructureUtil.getLayoutStructure(
				themeDisplay.getScopeGroupId(), themeDisplay.getPlid(),
				fragmentEntryLink.getSegmentsExperienceId());

		DefaultFragmentRendererContext defaultFragmentRendererContext =
			new DefaultFragmentRendererContext(fragmentEntryLink);

		HttpServletRequest httpServletRequest = _portal.getHttpServletRequest(
			actionRequest);

		LayoutDisplayPageProvider<?> currentLayoutDisplayPageProvider =
			(LayoutDisplayPageProvider<?>)httpServletRequest.getAttribute(
				LayoutDisplayPageWebKeys.LAYOUT_DISPLAY_PAGE_PROVIDER);

		if (Validator.isNotNull(itemClassName) &&
			((itemClassPK > 0) ||
			 Validator.isNotNull(itemExternalReferenceCode))) {

			InfoItemIdentifier infoItemIdentifier = null;

			if (itemClassPK > 0) {
				infoItemIdentifier = new ClassPKInfoItemIdentifier(itemClassPK);
			}
			else {
				infoItemIdentifier = new ERCInfoItemIdentifier(
					itemExternalReferenceCode);
			}

			InfoItemObjectProvider<Object> infoItemObjectProvider =
				_infoItemServiceRegistry.getFirstInfoItemService(
					InfoItemObjectProvider.class, itemClassName,
					infoItemIdentifier.getInfoItemServiceFilter());

			if (infoItemObjectProvider != null) {
				Object infoItemObject = infoItemObjectProvider.getInfoItem(
					infoItemIdentifier);

				defaultFragmentRendererContext.setContextInfoItemReference(
					new InfoItemReference(itemClassName, infoItemIdentifier));

				httpServletRequest.setAttribute(
					InfoDisplayWebKeys.INFO_ITEM, infoItemObject);

				InfoItemDetailsProvider infoItemDetailsProvider =
					_infoItemServiceRegistry.getFirstInfoItemService(
						InfoItemDetailsProvider.class, itemClassName);

				if (infoItemDetailsProvider != null) {
					httpServletRequest.setAttribute(
						InfoDisplayWebKeys.INFO_ITEM_DETAILS,
						infoItemDetailsProvider.getInfoItemDetails(
							infoItemObject));
				}

				httpServletRequest.setAttribute(
					InfoDisplayWebKeys.INFO_ITEM_REFERENCE,
					new InfoItemReference(itemClassName, infoItemIdentifier));

				_addLinkedAssetEntryId(
					itemClassName, itemClassPK, httpServletRequest);
			}

			LayoutDisplayPageProvider<?> layoutDisplayPageProvider =
				_layoutDisplayPageProviderRegistry.
					getLayoutDisplayPageProviderByClassName(
						_portal.getCompanyId(httpServletRequest),
						itemClassName);

			if (layoutDisplayPageProvider != null) {
				httpServletRequest.setAttribute(
					LayoutDisplayPageWebKeys.LAYOUT_DISPLAY_PAGE_PROVIDER,
					layoutDisplayPageProvider);
			}
		}

		JSONObject fragmentEntryLinkJSONObject;

		try {
			fragmentEntryLinkJSONObject =
				_fragmentEntryLinkManager.getFragmentEntryLinkJSONObject(
					defaultFragmentRendererContext, fragmentEntryLink,
					httpServletRequest,
					_portal.getHttpServletResponse(actionResponse),
					layoutStructure);
		}
		finally {
			httpServletRequest.removeAttribute(
				InfoDisplayWebKeys.INFO_ITEM_REFERENCE);

			httpServletRequest.setAttribute(
				LayoutDisplayPageWebKeys.LAYOUT_DISPLAY_PAGE_PROVIDER,
				currentLayoutDisplayPageProvider);
		}

		return JSONUtil.put(
			"fragmentEntryLink", fragmentEntryLinkJSONObject
		).put(
			"layoutData", layoutStructure.toJSONObject()
		);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		UpdateConfigurationValuesMVCActionCommand.class);

	@Reference
	private FragmentEntryConfigurationParser _fragmentEntryConfigurationParser;

	@Reference
	private FragmentEntryLinkListenerRegistry
		_fragmentEntryLinkListenerRegistry;

	@Reference
	private FragmentEntryLinkManager _fragmentEntryLinkManager;

	@Reference
	private FragmentEntryLinkService _fragmentEntryLinkService;

	@Reference
	private FragmentEntryProcessorRegistry _fragmentEntryProcessorRegistry;

	@Reference
	private InfoItemServiceRegistry _infoItemServiceRegistry;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Language _language;

	@Reference
	private LayoutDisplayPageProviderRegistry
		_layoutDisplayPageProviderRegistry;

	@Reference
	private Portal _portal;

}