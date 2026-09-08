/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.layout.content.page.editor.web.internal.manager;

import com.liferay.asset.kernel.AssetRendererFactoryRegistryUtil;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.asset.util.LinkedAssetEntryIdsUtil;
import com.liferay.fragment.constants.FragmentConstants;
import com.liferay.fragment.constants.FragmentEntryLinkConstants;
import com.liferay.fragment.contributor.FragmentCollectionContributorRegistry;
import com.liferay.fragment.entry.processor.constants.FragmentEntryProcessorConstants;
import com.liferay.fragment.entry.processor.util.EditableFragmentEntryProcessorUtil;
import com.liferay.fragment.helper.FragmentEntryLinkHelper;
import com.liferay.fragment.model.FragmentEntry;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.renderer.DefaultFragmentRendererContext;
import com.liferay.fragment.renderer.FragmentRenderer;
import com.liferay.fragment.renderer.FragmentRendererController;
import com.liferay.fragment.renderer.FragmentRendererRegistry;
import com.liferay.fragment.renderer.constants.FragmentRendererConstants;
import com.liferay.fragment.service.FragmentEntryLinkLocalService;
import com.liferay.fragment.service.FragmentEntryLocalService;
import com.liferay.fragment.util.configuration.FragmentEntryConfigurationParser;
import com.liferay.info.constants.InfoDisplayWebKeys;
import com.liferay.info.exception.NoSuchFormVariationException;
import com.liferay.info.exception.NoSuchInfoItemException;
import com.liferay.info.form.InfoForm;
import com.liferay.info.item.ClassPKInfoItemIdentifier;
import com.liferay.info.item.ERCInfoItemIdentifier;
import com.liferay.info.item.InfoItemIdentifier;
import com.liferay.info.item.InfoItemReference;
import com.liferay.info.item.InfoItemServiceRegistry;
import com.liferay.info.item.provider.InfoItemDetailsProvider;
import com.liferay.info.item.provider.InfoItemFormProvider;
import com.liferay.info.item.provider.InfoItemObjectProvider;
import com.liferay.item.selector.ItemSelector;
import com.liferay.layout.content.page.editor.web.internal.comment.CommentUtil;
import com.liferay.layout.content.page.editor.web.internal.util.FragmentEntryLinkItemSelectorUtil;
import com.liferay.layout.display.page.LayoutDisplayPageProvider;
import com.liferay.layout.display.page.LayoutDisplayPageProviderRegistry;
import com.liferay.layout.display.page.constants.LayoutDisplayPageWebKeys;
import com.liferay.layout.util.constants.LayoutDataItemTypeConstants;
import com.liferay.layout.util.structure.FormStyledLayoutStructureItem;
import com.liferay.layout.util.structure.FragmentStyledLayoutStructureItem;
import com.liferay.layout.util.structure.LayoutStructure;
import com.liferay.layout.util.structure.LayoutStructureItem;
import com.liferay.layout.util.structure.LayoutStructureItemUtil;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerListFactory;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.comment.Comment;
import com.liferay.portal.kernel.comment.CommentManager;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.portlet.PortletIdCodec;
import com.liferay.portal.kernel.portlet.configuration.icon.EditModePortletConfigurationIcon;
import com.liferay.portal.kernel.service.PortletLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Eudaldo Alonso
 */
@Component(service = FragmentEntryLinkManager.class)
public class FragmentEntryLinkManager {

	/**
	 * Resolves the previewed/mapped item (for example, the object entry
	 * selected via "Preview With") into
	 * <code>defaultFragmentRendererContext</code> and into request
	 * attributes, so a fragment render can make that item available to its
	 * FreeMarker template (as the info item, and to the display page
	 * provider). Returns the previously set {@link LayoutDisplayPageProvider}
	 * request attribute so it can be restored afterwards with {@link
	 * #resetItemContext(HttpServletRequest, LayoutDisplayPageProvider)}.
	 */
	public LayoutDisplayPageProvider<?> applyItemContext(
			DefaultFragmentRendererContext defaultFragmentRendererContext,
			String itemClassName, long itemClassPK,
			String itemExternalReferenceCode,
			HttpServletRequest httpServletRequest)
		throws NoSuchInfoItemException {

		LayoutDisplayPageProvider<?> currentLayoutDisplayPageProvider =
			(LayoutDisplayPageProvider<?>)httpServletRequest.getAttribute(
				LayoutDisplayPageWebKeys.LAYOUT_DISPLAY_PAGE_PROVIDER);

		if (Validator.isNull(itemClassName) ||
			((itemClassPK <= 0) &&
			 Validator.isNull(itemExternalReferenceCode))) {

			return currentLayoutDisplayPageProvider;
		}

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
					infoItemDetailsProvider.getInfoItemDetails(infoItemObject));
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
					_portal.getCompanyId(httpServletRequest), itemClassName);

		if (layoutDisplayPageProvider != null) {
			httpServletRequest.setAttribute(
				LayoutDisplayPageWebKeys.LAYOUT_DISPLAY_PAGE_PROVIDER,
				layoutDisplayPageProvider);
		}

		return currentLayoutDisplayPageProvider;
	}

	public List<FragmentEntryLink> getChildrenFragmentEntryLinks(
			List<String> itemIds, LayoutStructure layoutStructure)
		throws PortalException {

		List<FragmentEntryLink> fragmentEntryLinks = new ArrayList<>();

		for (String itemId : itemIds) {
			LayoutStructureItem layoutStructureItem =
				layoutStructure.getLayoutStructureItem(itemId);

			if (layoutStructureItem instanceof
					FragmentStyledLayoutStructureItem) {

				FragmentStyledLayoutStructureItem
					fragmentStyledLayoutStructureItem =
						(FragmentStyledLayoutStructureItem)layoutStructureItem;

				fragmentEntryLinks.add(
					_fragmentEntryLinkLocalService.getFragmentEntryLink(
						fragmentStyledLayoutStructureItem.
							getFragmentEntryLinkId()));
			}

			fragmentEntryLinks.addAll(
				getChildrenFragmentEntryLinks(
					layoutStructureItem.getChildrenItemIds(), layoutStructure));
		}

		return fragmentEntryLinks;
	}

	public FragmentEntry getFragmentEntry(
		long groupId, String fragmentEntryKey, Locale locale) {

		FragmentEntry fragmentEntry =
			_fragmentEntryLocalService.fetchFragmentEntry(
				groupId, fragmentEntryKey);

		if (fragmentEntry != null) {
			return fragmentEntry;
		}

		Map<String, FragmentEntry> fragmentEntries =
			_fragmentCollectionContributorRegistry.getFragmentEntries(locale);

		return fragmentEntries.get(fragmentEntryKey);
	}

	public Set<String> getFragmentEntryLinkFieldTypes(
		long fragmentEntryLinkId) {

		FragmentEntryLink fragmentEntryLink =
			_fragmentEntryLinkLocalService.fetchFragmentEntryLink(
				fragmentEntryLinkId);

		if (fragmentEntryLink == null) {
			return Collections.emptySet();
		}

		FragmentRenderer fragmentRenderer =
			_fragmentRendererRegistry.getFragmentRenderer(
				fragmentEntryLink.getRendererKey());

		if (fragmentRenderer != null) {
			return _getFieldTypes(fragmentRenderer.getTypeOptions());
		}

		FragmentEntry fragmentEntry = _getFragmentEntry(
			fragmentEntryLink, LocaleUtil.getMostRelevantLocale());

		if (fragmentEntry != null) {
			return _getFieldTypes(fragmentEntry.getTypeOptions());
		}

		return Collections.emptySet();
	}

	public JSONObject getFragmentEntryLinkJSONObject(
			DefaultFragmentRendererContext defaultFragmentRendererContext,
			FragmentEntryLink fragmentEntryLink,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse,
			LayoutStructure layoutStructure)
		throws PortalException {

		ThemeDisplay themeDisplay =
			(ThemeDisplay)httpServletRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		boolean isolated = themeDisplay.isIsolated();

		themeDisplay.setIsolated(true);

		try {
			JSONObject editableValuesJSONObject =
				fragmentEntryLink.getEditableValuesJSONObject();

			String content = _getContent(
				defaultFragmentRendererContext, editableValuesJSONObject,
				fragmentEntryLink, httpServletRequest, httpServletResponse,
				layoutStructure, themeDisplay);

			if (fragmentEntryLink.isTypePortlet()) {
				String portletId = editableValuesJSONObject.getString(
					"portletId");

				return JSONUtil.put(
					"actions",
					() -> {
						Portlet portlet = _portletLocalService.fetchPortletById(
							themeDisplay.getCompanyId(), portletId);

						if (portlet == null) {
							return _jsonFactory.createJSONObject();
						}

						return _getActionsJSONObject(
							httpServletRequest,
							editableValuesJSONObject.getString("instanceId"),
							portletId);
					}
				).put(
					"comments",
					_getFragmentEntryLinkCommentsJSONArray(
						fragmentEntryLink, httpServletRequest)
				).put(
					"configuration", _jsonFactory.createJSONObject()
				).put(
					"content", content
				).put(
					"cssClass",
					() -> {
						FragmentStyledLayoutStructureItem
							fragmentStyledLayoutStructureItem =
								(FragmentStyledLayoutStructureItem)
									layoutStructure.
										getLayoutStructureItemByFragmentEntryLinkId(
											fragmentEntryLink.
												getFragmentEntryLinkId());

						if (fragmentStyledLayoutStructureItem == null) {
							return null;
						}

						return fragmentStyledLayoutStructureItem.
							getFragmentEntryLinkCssClass(fragmentEntryLink);
					}
				).put(
					"defaultConfigurationValues",
					_jsonFactory.createJSONObject()
				).put(
					"editableTypes", Collections.emptyMap()
				).put(
					"editableValues",
					fragmentEntryLink.getEditableValuesJSONObject()
				).put(
					"fragmentEntryId", 0
				).put(
					"fragmentEntryKey",
					FragmentRendererConstants.
						FRAGMENT_RENDERER_KEY_FRAGMENT_ENTRY
				).put(
					"fragmentEntryLinkId",
					String.valueOf(fragmentEntryLink.getFragmentEntryLinkId())
				).put(
					"fragmentEntryType",
					FragmentConstants.getTypeLabel(
						FragmentConstants.TYPE_PORTLET)
				).put(
					"name",
					_portal.getPortletTitle(portletId, themeDisplay.getLocale())
				).put(
					"portletId", portletId
				).put(
					"segmentsExperienceId",
					String.valueOf(fragmentEntryLink.getSegmentsExperienceId())
				);
			}

			defaultFragmentRendererContext.setLocale(themeDisplay.getLocale());

			JSONObject configurationJSONObject =
				_fragmentRendererController.getConfigurationJSONObject(
					defaultFragmentRendererContext);

			FragmentEntryLinkItemSelectorUtil.
				addFragmentEntryLinkFieldsSelectorURL(
					_itemSelector, httpServletRequest, configurationJSONObject);

			FragmentEntry fragmentEntry = _getFragmentEntry(
				fragmentEntryLink, themeDisplay.getLocale());

			return JSONUtil.put(
				"comments",
				_getFragmentEntryLinkCommentsJSONArray(
					fragmentEntryLink, httpServletRequest)
			).put(
				"configuration", configurationJSONObject
			).put(
				"content", content
			).put(
				"cssClass",
				() -> {
					FragmentStyledLayoutStructureItem
						fragmentStyledLayoutStructureItem =
							(FragmentStyledLayoutStructureItem)
								layoutStructure.
									getLayoutStructureItemByFragmentEntryLinkId(
										fragmentEntryLink.
											getFragmentEntryLinkId());

					if (fragmentStyledLayoutStructureItem == null) {
						return null;
					}

					return fragmentStyledLayoutStructureItem.
						getFragmentEntryLinkCssClass(fragmentEntryLink);
				}
			).put(
				"defaultConfigurationValues",
				_fragmentEntryConfigurationParser.
					getConfigurationDefaultValuesJSONObject(
						configurationJSONObject)
			).put(
				"editableTypes",
				EditableFragmentEntryProcessorUtil.getEditableTypes(content)
			).put(
				"editableValues", editableValuesJSONObject
			).put(
				"fieldTypes",
				() -> {
					if (fragmentEntry != null) {
						return _jsonFactory.createJSONArray(
							_getFieldTypes(fragmentEntry.getTypeOptions()));
					}

					FragmentRenderer fragmentRenderer =
						_fragmentRendererRegistry.getFragmentRenderer(
							fragmentEntryLink.getRendererKey());

					if (fragmentRenderer != null) {
						return _jsonFactory.createJSONArray(
							_getFieldTypes(fragmentRenderer.getTypeOptions()));
					}

					return _jsonFactory.createJSONArray();
				}
			).put(
				"fragmentEntryId",
				() -> {
					if (fragmentEntry != null) {
						return fragmentEntry.getFragmentEntryId();
					}

					return 0;
				}
			).put(
				"fragmentEntryKey",
				() -> {
					if (fragmentEntry != null) {
						return fragmentEntry.getFragmentEntryKey();
					}

					String rendererKey = fragmentEntryLink.getRendererKey();

					if (Validator.isNull(rendererKey)) {
						rendererKey =
							FragmentRendererConstants.
								FRAGMENT_RENDERER_KEY_FRAGMENT_ENTRY;
					}

					FragmentRenderer fragmentRenderer =
						_fragmentRendererRegistry.getFragmentRenderer(
							rendererKey);

					if (fragmentRenderer != null) {
						return fragmentRenderer.getKey();
					}

					return StringPool.BLANK;
				}
			).put(
				"fragmentEntryLinkId",
				String.valueOf(fragmentEntryLink.getFragmentEntryLinkId())
			).put(
				"fragmentEntryType",
				() -> {
					int fragmentEntryType = FragmentConstants.TYPE_COMPONENT;

					if (fragmentEntry != null) {
						fragmentEntryType = fragmentEntry.getType();
					}
					else {
						FragmentRenderer fragmentRenderer =
							_fragmentRendererRegistry.getFragmentRenderer(
								fragmentEntryLink.getRendererKey());

						if (fragmentRenderer != null) {
							fragmentEntryType = fragmentRenderer.getType();
						}
					}

					return FragmentConstants.getTypeLabel(fragmentEntryType);
				}
			).put(
				"groupId",
				() -> {
					if (fragmentEntry != null) {
						return fragmentEntry.getGroupId();
					}

					return null;
				}
			).put(
				"icon",
				() -> {
					if (fragmentEntry != null) {
						return fragmentEntry.getIcon();
					}

					return null;
				}
			).put(
				"name",
				_fragmentEntryLinkHelper.getFragmentEntryName(
					fragmentEntryLink, themeDisplay.getLocale())
			).put(
				"segmentsExperienceId",
				String.valueOf(fragmentEntryLink.getSegmentsExperienceId())
			);
		}
		finally {
			themeDisplay.setIsolated(isolated);
		}
	}

	public JSONObject getFragmentEntryLinkJSONObject(
			FragmentEntryLink fragmentEntryLink,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse,
			LayoutStructure layoutStructure)
		throws PortalException {

		return getFragmentEntryLinkJSONObject(
			new DefaultFragmentRendererContext(fragmentEntryLink),
			fragmentEntryLink, httpServletRequest, httpServletResponse,
			layoutStructure);
	}

	public JSONObject mergeEditableValuesJSONObject(
		JSONObject defaultEditableValuesJSONObject,
		JSONObject editableValuesJSONObject) {

		for (String fragmentEntryProcessorKey :
				_FRAGMENT_ENTRY_PROCESSOR_KEYS) {

			JSONObject editableFragmentEntryProcessorJSONObject =
				editableValuesJSONObject.getJSONObject(
					fragmentEntryProcessorKey);

			JSONObject defaultEditableFragmentEntryProcessorJSONObject =
				defaultEditableValuesJSONObject.getJSONObject(
					fragmentEntryProcessorKey);

			if (defaultEditableFragmentEntryProcessorJSONObject == null) {
				continue;
			}

			if (editableFragmentEntryProcessorJSONObject != null) {
				Iterator<String> iterator =
					defaultEditableFragmentEntryProcessorJSONObject.keys();

				while (iterator.hasNext()) {
					String key = iterator.next();

					if (editableFragmentEntryProcessorJSONObject.has(key)) {
						defaultEditableFragmentEntryProcessorJSONObject.put(
							key,
							editableFragmentEntryProcessorJSONObject.get(key));
					}
				}
			}

			editableValuesJSONObject.put(
				fragmentEntryProcessorKey,
				defaultEditableFragmentEntryProcessorJSONObject);
		}

		return editableValuesJSONObject;
	}

	/**
	 * Undoes {@link #applyItemContext(DefaultFragmentRendererContext, String,
	 * long, String, HttpServletRequest)}, restoring the request to the state
	 * it was in beforehand.
	 */
	public void resetItemContext(
		HttpServletRequest httpServletRequest,
		LayoutDisplayPageProvider<?> previousLayoutDisplayPageProvider) {

		httpServletRequest.removeAttribute(
			InfoDisplayWebKeys.INFO_ITEM_REFERENCE);

		httpServletRequest.setAttribute(
			LayoutDisplayPageWebKeys.LAYOUT_DISPLAY_PAGE_PROVIDER,
			previousLayoutDisplayPageProvider);
	}

	@Activate
	protected void activate(BundleContext bundleContext) {
		_serviceTrackerList = ServiceTrackerListFactory.open(
			bundleContext, EditModePortletConfigurationIcon.class);
	}

	@Deactivate
	protected void deactivate() {
		_serviceTrackerList.close();
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

	private JSONObject _getActionsJSONObject(
		HttpServletRequest httpServletRequest, String instanceId,
		String portletId) {

		JSONObject jsonObject = _jsonFactory.createJSONObject();

		String encodedPortletId = PortletIdCodec.encode(portletId, instanceId);

		for (EditModePortletConfigurationIcon editModePortletConfigurationIcon :
				_serviceTrackerList.toList()) {

			if (!editModePortletConfigurationIcon.isShow(
					httpServletRequest, encodedPortletId)) {

				continue;
			}

			Class<?> clazz = editModePortletConfigurationIcon.getClass();

			jsonObject.put(
				clazz.getSimpleName(),
				JSONUtil.put(
					"group",
					editModePortletConfigurationIcon.
						getPortletConfigurationIconGroup()
				).put(
					"icon", editModePortletConfigurationIcon.getIcon()
				).put(
					"title",
					editModePortletConfigurationIcon.getTitle(
						httpServletRequest)
				).put(
					"url",
					editModePortletConfigurationIcon.getURL(
						httpServletRequest, encodedPortletId)
				));
		}

		return jsonObject;
	}

	private String _getContent(
		DefaultFragmentRendererContext defaultFragmentRendererContext,
		JSONObject editableValuesJSONObject,
		FragmentEntryLink fragmentEntryLink,
		HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse,
		LayoutStructure layoutStructure, ThemeDisplay themeDisplay) {

		if (fragmentEntryLink.isTypePortlet()) {
			String portletId = editableValuesJSONObject.getString("portletId");

			Portlet portlet = _portletLocalService.fetchPortletById(
				themeDisplay.getCompanyId(), portletId);

			if ((portlet == null) || portlet.isUndeployedPortlet()) {
				String message = _language.get(
					httpServletRequest,
					"this-portlet-could-not-be-found.-please-redeploy-it-or-" +
						"remove-it-from-the-page");

				return "<div class=\"alert alert-info\">" + message + "</div>";
			}
		}

		defaultFragmentRendererContext.setInfoForm(
			_getInfoForm(fragmentEntryLink, layoutStructure));

		String languageId = ParamUtil.getString(
			httpServletRequest, "languageId",
			LocaleUtil.toLanguageId(themeDisplay.getSiteDefaultLocale()));

		defaultFragmentRendererContext.setLocale(
			LocaleUtil.fromLanguageId(languageId));

		defaultFragmentRendererContext.setMode(FragmentEntryLinkConstants.EDIT);

		return _fragmentRendererController.render(
			defaultFragmentRendererContext, httpServletRequest,
			httpServletResponse);
	}

	private Set<String> _getFieldTypes(String typeOptions) {
		try {
			JSONObject jsonObject = _jsonFactory.createJSONObject(typeOptions);

			JSONArray jsonArray = jsonObject.getJSONArray("fieldTypes");

			if (jsonArray != null) {
				return JSONUtil.toStringSet(jsonArray);
			}
		}
		catch (JSONException jsonException) {
			_log.error(jsonException);
		}

		return Collections.emptySet();
	}

	private FragmentEntry _getFragmentEntry(
		FragmentEntryLink fragmentEntryLink, Locale locale) {

		FragmentEntry fragmentEntry = fragmentEntryLink.fetchFragmentEntry();

		if (fragmentEntry != null) {
			return fragmentEntry;
		}

		return getFragmentEntry(
			fragmentEntryLink.getGroupId(), fragmentEntryLink.getRendererKey(),
			locale);
	}

	private JSONArray _getFragmentEntryLinkCommentsJSONArray(
		FragmentEntryLink fragmentEntryLink,
		HttpServletRequest httpServletRequest) {

		JSONArray jsonArray = _jsonFactory.createJSONArray();

		try {
			if (!_commentManager.hasDiscussion(
					FragmentEntryLink.class.getName(),
					fragmentEntryLink.getFragmentEntryLinkId())) {

				return jsonArray;
			}

			List<Comment> rootComments = _commentManager.getRootComments(
				FragmentEntryLink.class.getName(),
				fragmentEntryLink.getFragmentEntryLinkId(),
				WorkflowConstants.STATUS_ANY, QueryUtil.ALL_POS,
				QueryUtil.ALL_POS);

			for (Comment rootComment : rootComments) {
				JSONObject commentJSONObject = CommentUtil.getCommentJSONObject(
					rootComment, httpServletRequest);

				List<Comment> childComments = _commentManager.getChildComments(
					rootComment.getCommentId(), WorkflowConstants.STATUS_ANY,
					QueryUtil.ALL_POS, QueryUtil.ALL_POS);

				JSONArray childCommentsJSONArray =
					_jsonFactory.createJSONArray();

				for (Comment childComment : childComments) {
					childCommentsJSONArray.put(
						CommentUtil.getCommentJSONObject(
							childComment, httpServletRequest));
				}

				commentJSONObject.put("children", childCommentsJSONArray);

				jsonArray.put(commentJSONObject);
			}
		}
		catch (PortalException portalException) {
			_log.error(portalException);

			return jsonArray;
		}

		return jsonArray;
	}

	private InfoForm _getInfoForm(
		FormStyledLayoutStructureItem formStyledLayoutStructureItem,
		long groupId) {

		if (formStyledLayoutStructureItem == null) {
			return null;
		}

		String className = formStyledLayoutStructureItem.getClassName();

		if (Validator.isNull(className)) {
			return null;
		}

		InfoItemFormProvider<Object> infoItemFormProvider =
			_infoItemServiceRegistry.getFirstInfoItemService(
				InfoItemFormProvider.class, className);

		if (infoItemFormProvider == null) {
			return null;
		}

		try {
			return infoItemFormProvider.getInfoForm(
				String.valueOf(formStyledLayoutStructureItem.getClassTypeId()),
				groupId);
		}
		catch (NoSuchFormVariationException noSuchFormVariationException) {
			if (_log.isDebugEnabled()) {
				_log.debug(noSuchFormVariationException);
			}

			return null;
		}
	}

	private InfoForm _getInfoForm(
		FragmentEntryLink fragmentEntryLink, LayoutStructure layoutStructure) {

		FragmentStyledLayoutStructureItem fragmentStyledLayoutStructureItem =
			(FragmentStyledLayoutStructureItem)
				layoutStructure.getLayoutStructureItemByFragmentEntryLinkId(
					fragmentEntryLink.getFragmentEntryLinkId());

		if (fragmentStyledLayoutStructureItem == null) {
			return null;
		}

		LayoutStructureItem layoutStructureItem =
			LayoutStructureItemUtil.getAncestor(
				fragmentStyledLayoutStructureItem.getItemId(),
				LayoutDataItemTypeConstants.TYPE_FORM, layoutStructure);

		if (!(layoutStructureItem instanceof FormStyledLayoutStructureItem)) {
			return null;
		}

		return _getInfoForm(
			(FormStyledLayoutStructureItem)layoutStructureItem,
			fragmentEntryLink.getGroupId());
	}

	private static final String[] _FRAGMENT_ENTRY_PROCESSOR_KEYS = {
		FragmentEntryProcessorConstants.
			KEY_BACKGROUND_IMAGE_FRAGMENT_ENTRY_PROCESSOR,
		FragmentEntryProcessorConstants.KEY_EDITABLE_FRAGMENT_ENTRY_PROCESSOR
	};

	private static final Log _log = LogFactoryUtil.getLog(
		FragmentEntryLinkManager.class);

	@Reference
	private CommentManager _commentManager;

	@Reference
	private FragmentCollectionContributorRegistry
		_fragmentCollectionContributorRegistry;

	@Reference
	private FragmentEntryConfigurationParser _fragmentEntryConfigurationParser;

	@Reference
	private FragmentEntryLinkHelper _fragmentEntryLinkHelper;

	@Reference
	private FragmentEntryLinkLocalService _fragmentEntryLinkLocalService;

	@Reference
	private FragmentEntryLocalService _fragmentEntryLocalService;

	@Reference
	private FragmentRendererController _fragmentRendererController;

	@Reference
	private FragmentRendererRegistry _fragmentRendererRegistry;

	@Reference
	private InfoItemServiceRegistry _infoItemServiceRegistry;

	@Reference
	private ItemSelector _itemSelector;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Language _language;

	@Reference
	private LayoutDisplayPageProviderRegistry
		_layoutDisplayPageProviderRegistry;

	@Reference
	private Portal _portal;

	@Reference
	private PortletLocalService _portletLocalService;

	private ServiceTrackerList<EditModePortletConfigurationIcon>
		_serviceTrackerList;

}