/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.v7_4_x.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.layout.test.util.LayoutTestUtil;
import com.liferay.portal.kernel.cache.CacheRegistryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutFriendlyURL;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.ResourcePermission;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutFriendlyURLLocalService;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ResourceLocalService;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.CompanyProviderClassTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.CompanyTestUtil;
import com.liferay.portal.kernel.test.util.RoleTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.v7_4_x.LayoutCompanyIdUpgradeProcess;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Akhash Ramprakash
 */
@RunWith(Arquillian.class)
public class LayoutCompanyIdUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule() {
			{
				skipTestRule(CompanyProviderClassTestRule.INSTANCE);
			}
		};

	@Before
	public void setUp() throws Exception {
		_company = CompanyTestUtil.addCompany();

		_group = _groupLocalService.getGroup(
			_company.getCompanyId(), GroupConstants.GUEST);
	}

	@Test
	public void testUpgrade() throws Exception {
		Layout layout = _corruptLayout(
			LayoutTestUtil.addTypePortletLayout(_group.getGroupId()));

		Assert.assertEquals(
			TestPropsValues.getCompanyId(), layout.getCompanyId());
		Assert.assertEquals(
			3,
			_getResourcePermissionsCount(
				TestPropsValues.getCompanyId(), layout.getPlid()));

		layout = _upgrade(layout);

		Assert.assertEquals(_company.getCompanyId(), layout.getCompanyId());
		Assert.assertEquals(
			0,
			_getResourcePermissionsCount(
				TestPropsValues.getCompanyId(), layout.getPlid()));
		Assert.assertEquals(
			3,
			_getResourcePermissionsCount(
				_company.getCompanyId(), layout.getPlid()));
		Assert.assertTrue(
			_hasResourcePermission(
				ActionKeys.VIEW, layout, RoleConstants.GUEST));
		Assert.assertTrue(
			_hasResourcePermission(
				ActionKeys.VIEW, layout, RoleConstants.OWNER));
		Assert.assertTrue(
			_hasResourcePermission(
				ActionKeys.VIEW, layout, RoleConstants.SITE_MEMBER));

		_assertLayoutFriendlyURLs(layout);
	}

	@Test
	public void testUpgradeContentLayout() throws Exception {
		Layout layout = _corruptLayout(
			LayoutTestUtil.addTypeContentLayout(_group));

		Layout draftLayout = _corruptLayout(layout.fetchDraftLayout());

		layout = _upgrade(layout);

		draftLayout = _layoutLocalService.getLayout(draftLayout.getPlid());

		Assert.assertEquals(_company.getCompanyId(), layout.getCompanyId());
		Assert.assertEquals(
			_company.getCompanyId(), draftLayout.getCompanyId());
		Assert.assertEquals(
			0,
			_getResourcePermissionsCount(
				TestPropsValues.getCompanyId(), draftLayout.getPlid()));
		Assert.assertEquals(
			3,
			_getResourcePermissionsCount(
				_company.getCompanyId(), draftLayout.getPlid()));
		Assert.assertTrue(
			_hasResourcePermission(
				ActionKeys.VIEW, draftLayout, RoleConstants.OWNER));

		_assertLayoutFriendlyURLs(layout);
	}

	@Test
	public void testUpgradeCustomRoleResourcePermission() throws Exception {
		Layout layout = _corruptLayout(
			LayoutTestUtil.addTypePortletLayout(_group.getGroupId()));

		_role = RoleTestUtil.addRole(RoleConstants.TYPE_REGULAR);

		User user = _userLocalService.getGuestUser(_company.getCompanyId());

		_roleLocalService.addRole(
			null, user.getUserId(), null, 0, _role.getName(), null, null,
			RoleConstants.TYPE_REGULAR, null, null);

		_resourcePermissionLocalService.setResourcePermissions(
			TestPropsValues.getCompanyId(), Layout.class.getName(),
			ResourceConstants.SCOPE_INDIVIDUAL,
			String.valueOf(layout.getPlid()), _role.getRoleId(),
			new String[] {ActionKeys.VIEW});

		Assert.assertEquals(
			4,
			_getResourcePermissionsCount(
				TestPropsValues.getCompanyId(), layout.getPlid()));

		layout = _upgrade(layout);

		Assert.assertEquals(
			0,
			_getResourcePermissionsCount(
				TestPropsValues.getCompanyId(), layout.getPlid()));
		Assert.assertEquals(
			3,
			_getResourcePermissionsCount(
				_company.getCompanyId(), layout.getPlid()));
	}

	@Test
	public void testUpgradeExistingResourcePermission() throws Exception {
		Layout layout = _corruptLayout(
			LayoutTestUtil.addTypePortletLayout(_group.getGroupId()));

		Role role = _roleLocalService.getRole(
			_company.getCompanyId(), RoleConstants.SITE_MEMBER);

		_resourcePermissionLocalService.setResourcePermissions(
			_company.getCompanyId(), Layout.class.getName(),
			ResourceConstants.SCOPE_INDIVIDUAL,
			String.valueOf(layout.getPlid()), role.getRoleId(),
			new String[] {ActionKeys.UPDATE});

		Assert.assertFalse(
			_hasResourcePermission(
				ActionKeys.VIEW, layout, RoleConstants.SITE_MEMBER));

		layout = _upgrade(layout);

		Assert.assertEquals(
			0,
			_getResourcePermissionsCount(
				TestPropsValues.getCompanyId(), layout.getPlid()));
		Assert.assertEquals(
			3,
			_getResourcePermissionsCount(
				_company.getCompanyId(), layout.getPlid()));
		Assert.assertTrue(
			_hasResourcePermission(
				ActionKeys.UPDATE, layout, RoleConstants.SITE_MEMBER));
		Assert.assertTrue(
			_hasResourcePermission(
				ActionKeys.VIEW, layout, RoleConstants.SITE_MEMBER));
	}

	@Test
	public void testUpgradeLayoutOfSameCompany() throws Exception {
		Layout layout = LayoutTestUtil.addTypePortletLayout(
			_group.getGroupId());

		layout = _upgrade(layout);

		Assert.assertEquals(_company.getCompanyId(), layout.getCompanyId());
		Assert.assertEquals(
			3,
			_getResourcePermissionsCount(
				_company.getCompanyId(), layout.getPlid()));
		Assert.assertTrue(
			_hasResourcePermission(
				ActionKeys.VIEW, layout, RoleConstants.GUEST));

		_assertLayoutFriendlyURLs(layout);
	}

	private void _assertLayoutFriendlyURLs(Layout layout) {
		List<LayoutFriendlyURL> layoutFriendlyURLs =
			_layoutFriendlyURLLocalService.getLayoutFriendlyURLs(
				layout.getPlid());

		Assert.assertFalse(layoutFriendlyURLs.isEmpty());

		for (LayoutFriendlyURL layoutFriendlyURL : layoutFriendlyURLs) {
			Assert.assertEquals(
				_company.getCompanyId(), layoutFriendlyURL.getCompanyId());
		}
	}

	private Layout _corruptLayout(Layout layout) throws Exception {
		layout.setCompanyId(TestPropsValues.getCompanyId());

		layout = _layoutLocalService.updateLayout(layout);

		for (LayoutFriendlyURL layoutFriendlyURL :
				_layoutFriendlyURLLocalService.getLayoutFriendlyURLs(
					layout.getPlid())) {

			layoutFriendlyURL.setCompanyId(TestPropsValues.getCompanyId());

			_layoutFriendlyURLLocalService.updateLayoutFriendlyURL(
				layoutFriendlyURL);
		}

		_resourcePermissionLocalService.deleteResourcePermissions(
			_company.getCompanyId(), Layout.class.getName(),
			ResourceConstants.SCOPE_INDIVIDUAL, layout.getPlid());
		_resourceLocalService.addResources(
			TestPropsValues.getCompanyId(), _group.getGroupId(),
			TestPropsValues.getUserId(), Layout.class.getName(),
			layout.getPlid(), false, true, true);

		return layout;
	}

	private int _getResourcePermissionsCount(long companyId, long plid) {
		List<ResourcePermission> resourcePermissions =
			_resourcePermissionLocalService.getResourcePermissions(
				companyId, Layout.class.getName(),
				ResourceConstants.SCOPE_INDIVIDUAL, String.valueOf(plid));

		return resourcePermissions.size();
	}

	private boolean _hasResourcePermission(
			String actionId, Layout layout, String roleName)
		throws Exception {

		Role role = _roleLocalService.getRole(
			_company.getCompanyId(), roleName);

		return _resourcePermissionLocalService.hasResourcePermission(
			_company.getCompanyId(), Layout.class.getName(),
			ResourceConstants.SCOPE_INDIVIDUAL,
			String.valueOf(layout.getPlid()), role.getRoleId(), actionId);
	}

	private Layout _upgrade(Layout layout) throws Exception {
		UpgradeProcess upgradeProcess = new LayoutCompanyIdUpgradeProcess();

		upgradeProcess.upgrade();

		CacheRegistryUtil.clear();

		return _layoutLocalService.getLayout(layout.getPlid());
	}

	@DeleteAfterTestRun
	private Company _company;

	private Group _group;

	@Inject
	private GroupLocalService _groupLocalService;

	@Inject
	private LayoutFriendlyURLLocalService _layoutFriendlyURLLocalService;

	@Inject
	private LayoutLocalService _layoutLocalService;

	@Inject
	private ResourceLocalService _resourceLocalService;

	@Inject
	private ResourcePermissionLocalService _resourcePermissionLocalService;

	@DeleteAfterTestRun
	private Role _role;

	@Inject
	private RoleLocalService _roleLocalService;

	@Inject
	private UserLocalService _userLocalService;

}