/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.asset.internal.service;

import com.liferay.asset.entry.rel.model.AssetEntryAssetCategoryRel;
import com.liferay.asset.entry.rel.service.AssetEntryAssetCategoryRelLocalService;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceWrapper;
import com.liferay.portal.kernel.util.SetUtil;

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Eudaldo Alonso
 */
@Component(service = ServiceWrapper.class)
public class AssetEntryLocalServiceWrapper
	extends com.liferay.asset.kernel.service.AssetEntryLocalServiceWrapper {

	@Override
	public AssetEntry deleteAssetEntry(AssetEntry entry) {
		_assetEntryAssetCategoryRelLocalService.
			deleteAssetEntryAssetCategoryRelByAssetEntry(entry);

		return super.deleteAssetEntry(entry);
	}

	@Override
	public AssetEntry deleteAssetEntry(long assetEntryId)
		throws PortalException {

		_assetEntryAssetCategoryRelLocalService.
			deleteAssetEntryAssetCategoryRelByAssetEntryId(assetEntryId);

		return super.deleteAssetEntry(assetEntryId);
	}

	@Override
	public AssetEntry deleteEntry(AssetEntry entry) throws PortalException {
		_assetEntryAssetCategoryRelLocalService.
			deleteAssetEntryAssetCategoryRelByAssetEntryId(entry.getEntryId());

		return super.deleteEntry(entry);
	}

	@Override
	public AssetEntry deleteEntry(long entryId) throws PortalException {
		_assetEntryAssetCategoryRelLocalService.
			deleteAssetEntryAssetCategoryRelByAssetEntryId(entryId);

		return super.deleteEntry(entryId);
	}

	@Override
	public AssetEntry deleteEntry(String className, long classPK)
		throws PortalException {

		AssetEntry entry = super.deleteEntry(className, classPK);

		if (entry != null) {
			_assetEntryAssetCategoryRelLocalService.
				deleteAssetEntryAssetCategoryRelByAssetEntry(entry);
		}

		return entry;
	}

	@Override
	public AssetEntry updateEntry(
			long userId, long groupId, Date createDate, Date modifiedDate,
			String className, long classPK, String classUuid, long classTypeId,
			long[] categoryIds, String[] tagNames, boolean listable,
			boolean visible, Date startDate, Date endDate, Date publishDate,
			Date expirationDate, String mimeType, String title,
			String description, String summary, String url, String layoutUuid,
			int height, int width, Double priority)
		throws PortalException {

		return updateEntry(
			userId, groupId, createDate, modifiedDate, className, classPK,
			classUuid, classTypeId, categoryIds, tagNames, listable, visible,
			startDate, endDate, publishDate, expirationDate, mimeType, title,
			description, summary, url, layoutUuid, height, width, priority,
			null);
	}

	@Override
	public AssetEntry updateEntry(
			long userId, long groupId, Date createDate, Date modifiedDate,
			String className, long classPK, String classUuid, long classTypeId,
			long[] categoryIds, String[] tagNames, boolean listable,
			boolean visible, Date startDate, Date endDate, Date publishDate,
			Date expirationDate, String mimeType, String title,
			String description, String summary, String url, String layoutUuid,
			int height, int width, Double priority,
			ServiceContext serviceContext)
		throws PortalException {

		AssetEntry entry = super.updateEntry(
			userId, groupId, createDate, modifiedDate, className, classPK,
			classUuid, classTypeId, categoryIds, tagNames, listable, visible,
			startDate, endDate, publishDate, expirationDate, mimeType, title,
			description, summary, url, layoutUuid, height, width, priority,
			serviceContext);

		if ((categoryIds != null) &&
			((categoryIds.length > 0) || _hasAssetCategoryRels(entry))) {

			_updateAssetEntryAssetCategoryRels(
				entry,
				_assetCategoryLocalService.getViewableCategoryIds(
					className, classPK, categoryIds),
				serviceContext);
		}

		return entry;
	}

	private boolean _hasAssetCategoryRels(AssetEntry entry) {
		if (entry.isNew()) {
			return false;
		}

		int count = _assetCategoryLocalService.getCompanyCategoriesCount(
			entry.getCompanyId());

		if (count > 0) {
			return true;
		}

		return false;
	}

	private void _updateAssetEntryAssetCategoryRels(
			AssetEntry entry, long[] categoryIds, ServiceContext serviceContext)
		throws PortalException {

		Set<Long> newCategoryIds = SetUtil.fromArray(categoryIds);
		boolean modified = false;

		for (AssetEntryAssetCategoryRel assetEntryAssetCategoryRel :
				_assetEntryAssetCategoryRelLocalService.
					getAssetEntryAssetCategoryRelsByAssetEntryId(
						entry.getEntryId())) {

			if (newCategoryIds.remove(
					assetEntryAssetCategoryRel.getAssetCategoryId())) {

				continue;
			}

			_assetEntryAssetCategoryRelLocalService.
				deleteAssetEntryAssetCategoryRel(assetEntryAssetCategoryRel);

			modified = true;
		}

		for (long categoryId : newCategoryIds) {
			_assetEntryAssetCategoryRelLocalService.
				addAssetEntryAssetCategoryRel(entry.getEntryId(), categoryId);

			modified = true;
		}

		if (modified &&
			((serviceContext == null) || serviceContext.isIndexingEnabled())) {

			reindex(Collections.singletonList(entry));
		}
	}

	@Reference
	private AssetCategoryLocalService _assetCategoryLocalService;

	@Reference
	private AssetEntryAssetCategoryRelLocalService
		_assetEntryAssetCategoryRelLocalService;

}