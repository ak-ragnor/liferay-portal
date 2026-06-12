/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.asset.entry.query.processor.custom.user.attributes.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.asset.kernel.service.persistence.AssetEntryQuery;
import com.liferay.asset.util.AssetEntryQueryProcessor;
import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.expando.kernel.model.ExpandoColumn;
import com.liferay.expando.kernel.model.ExpandoTable;
import com.liferay.expando.kernel.service.ExpandoColumnLocalService;
import com.liferay.expando.kernel.service.ExpandoTableLocalService;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.portlet.MockPortletPreferences;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Akhash R
 */
@RunWith(Arquillian.class)
public class CustomUserAttributesAssetEntryQueryProcessorTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_user = UserTestUtil.addUser();

		_customUserAttributeName = RandomTestUtil.randomString();

		ExpandoBridge expandoBridge = _user.getExpandoBridge();

		expandoBridge.addAttribute(_customUserAttributeName, false);
	}

	@After
	public void tearDown() throws Exception {
		ExpandoTable expandoTable = _expandoTableLocalService.fetchDefaultTable(
			TestPropsValues.getCompanyId(), User.class.getName());

		if (expandoTable == null) {
			return;
		}

		ExpandoColumn expandoColumn = _expandoColumnLocalService.fetchColumn(
			expandoTable.getTableId(), _customUserAttributeName);

		if (expandoColumn != null) {
			_expandoColumnLocalService.deleteColumn(expandoColumn);
		}
	}

	@Test
	public void testProcessAssetEntryQuery() throws Exception {
		String userCustomFieldValue = RandomTestUtil.randomString();

		ExpandoBridge expandoBridge = _user.getExpandoBridge();

		expandoBridge.setAttribute(
			_customUserAttributeName, userCustomFieldValue, false);

		Group companyGroup = _groupLocalService.getCompanyGroup(
			TestPropsValues.getCompanyId());

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(
				companyGroup.getGroupId(), TestPropsValues.getUserId());

		AssetVocabulary assetVocabulary1 =
			_assetVocabularyLocalService.addVocabulary(
				TestPropsValues.getUserId(), companyGroup.getGroupId(),
				_customUserAttributeName, serviceContext);
		AssetVocabulary assetVocabulary2 =
			_assetVocabularyLocalService.addVocabulary(
				TestPropsValues.getUserId(), companyGroup.getGroupId(),
				RandomTestUtil.randomString(), serviceContext);

		AssetCategory assetCategory = _assetCategoryLocalService.addCategory(
			TestPropsValues.getUserId(), companyGroup.getGroupId(),
			userCustomFieldValue, assetVocabulary1.getVocabularyId(),
			serviceContext);

		_assetCategoryLocalService.addCategory(
			TestPropsValues.getUserId(), companyGroup.getGroupId(),
			userCustomFieldValue + RandomTestUtil.randomString(),
			assetVocabulary1.getVocabularyId(), serviceContext);
		_assetCategoryLocalService.addCategory(
			TestPropsValues.getUserId(), companyGroup.getGroupId(),
			userCustomFieldValue, assetVocabulary2.getVocabularyId(),
			serviceContext);

		MockPortletPreferences mockPortletPreferences =
			new MockPortletPreferences();

		mockPortletPreferences.setValue(
			"customUserAttributes", _customUserAttributeName);

		AssetEntryQuery assetEntryQuery = new AssetEntryQuery();

		_assetEntryQueryProcessor.processAssetEntryQuery(
			_user, mockPortletPreferences, assetEntryQuery);

		Assert.assertArrayEquals(
			new long[] {assetCategory.getCategoryId()},
			assetEntryQuery.getAllCategoryIds());
	}

	@Inject
	private AssetCategoryLocalService _assetCategoryLocalService;

	@Inject(
		filter = "component.name=com.liferay.asset.entry.query.processor.custom.user.attributes.internal.CustomUserAttributesAssetEntryQueryProcessor"
	)
	private AssetEntryQueryProcessor _assetEntryQueryProcessor;

	@Inject
	private AssetVocabularyLocalService _assetVocabularyLocalService;

	private String _customUserAttributeName;

	@Inject
	private ExpandoColumnLocalService _expandoColumnLocalService;

	@Inject
	private ExpandoTableLocalService _expandoTableLocalService;

	@Inject
	private GroupLocalService _groupLocalService;

	@DeleteAfterTestRun
	private User _user;

}