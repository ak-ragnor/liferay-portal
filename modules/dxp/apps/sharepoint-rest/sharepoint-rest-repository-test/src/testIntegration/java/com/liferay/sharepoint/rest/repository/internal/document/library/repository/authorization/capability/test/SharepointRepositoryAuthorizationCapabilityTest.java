/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.sharepoint.rest.repository.internal.document.library.repository.authorization.capability.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.document.library.constants.DLPortletKeys;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.document.library.repository.authorization.oauth2.OAuth2AuthorizationException;
import com.liferay.layout.test.util.LayoutTestUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.test.util.ConfigurationTestUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.Repository;
import com.liferay.portal.kernel.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.constants.MVCRenderConstants;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ClassNameLocalService;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.RepositoryLocalService;
import com.liferay.portal.kernel.struts.StrutsAction;
import com.liferay.portal.kernel.test.AssertUtils;
import com.liferay.portal.kernel.test.TestInfo;
import com.liferay.portal.kernel.test.portlet.MockLiferayPortletRenderRequest;
import com.liferay.portal.kernel.test.portlet.MockLiferayPortletRenderResponse;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.theme.PortletDisplay;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodePropertiesBuilder;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;

import jakarta.portlet.PortletPreferences;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Yash Gupta
 */
@RunWith(Arquillian.class)
public class SharepointRepositoryAuthorizationCapabilityTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();

		String name = RandomTestUtil.randomString();

		_pid = ConfigurationTestUtil.createFactoryConfiguration(
			_FACTORY_PID,
			HashMapDictionaryBuilder.<String, Object>put(
				"clientId", RandomTestUtil.randomString()
			).put(
				"clientSecret", RandomTestUtil.randomString()
			).put(
				"name", name
			).put(
				"scope", RandomTestUtil.randomString()
			).put(
				"tenantId", RandomTestUtil.randomString()
			).build());

		_repository = _repositoryLocalService.addRepository(
			null, TestPropsValues.getUserId(), _group.getGroupId(),
			_classNameLocalService.getClassNameId(
				"com.liferay.sharepoint.rest.repository.internal.document." +
					"library.repository.external.SharepointExtRepository" +
						name),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			RandomTestUtil.randomString(), StringPool.BLANK,
			RandomTestUtil.randomString(),
			UnicodePropertiesBuilder.create(
				true
			).put(
				"library-path", RandomTestUtil.randomString()
			).put(
				"site-absolute-url", RandomTestUtil.randomString()
			).build(),
			false,
			ServiceContextTestUtil.getServiceContext(
				_group.getGroupId(), TestPropsValues.getUserId()));
	}

	@After
	public void tearDown() throws Exception {
		_repositoryLocalService.deleteRepository(_repository);

		ConfigurationTestUtil.deleteFactoryConfiguration(_pid, _FACTORY_PID);
	}

	@Test
	@TestInfo("LPD-112524")
	public void testAuthorize() throws Exception {
		_testAuthorize();
		_testAuthorizeWhenUserIsNotSignedIn();
	}

	private Layout _addLayout(Folder rootFolder) throws Exception {
		Layout layout = LayoutTestUtil.addTypePortletLayout(_group);

		PortletPreferences portletPreferences =
			PortletPreferencesFactoryUtil.getStrictPortletSetup(
				layout, DLPortletKeys.DOCUMENT_LIBRARY);

		portletPreferences.setValue(
			"rootFolderExternalReferenceCode",
			rootFolder.getExternalReferenceCode());

		portletPreferences.store();

		return layout;
	}

	private String _getRedirectedURL(
		MockLiferayPortletRenderResponse mockLiferayPortletRenderResponse) {

		MockHttpServletResponse mockHttpServletResponse =
			(MockHttpServletResponse)
				mockLiferayPortletRenderResponse.getHttpServletResponse();

		return mockHttpServletResponse.getRedirectedUrl();
	}

	private ThemeDisplay _getThemeDisplay(Layout layout, boolean signedIn)
		throws Exception {

		ThemeDisplay themeDisplay = new ThemeDisplay();

		PortletDisplay portletDisplay = themeDisplay.getPortletDisplay();

		portletDisplay.setId(DLPortletKeys.DOCUMENT_LIBRARY);

		themeDisplay.setCompany(
			_companyLocalService.getCompany(_group.getCompanyId()));
		themeDisplay.setLayout(layout);
		themeDisplay.setPermissionChecker(
			PermissionThreadLocal.getPermissionChecker());
		themeDisplay.setRealUser(TestPropsValues.getUser());
		themeDisplay.setScopeGroupId(_group.getGroupId());
		themeDisplay.setSignedIn(signedIn);
		themeDisplay.setSiteGroupId(_group.getGroupId());
		themeDisplay.setUser(TestPropsValues.getUser());

		return themeDisplay;
	}

	private String _render(
			Layout layout, MockHttpServletRequest mockHttpServletRequest,
			MockLiferayPortletRenderResponse mockLiferayPortletRenderResponse,
			boolean signedIn)
		throws Exception {

		MockLiferayPortletRenderRequest mockLiferayPortletRenderRequest =
			new MockLiferayPortletRenderRequest(mockHttpServletRequest);

		mockHttpServletRequest.setAttribute(
			JavaConstants.JAKARTA_PORTLET_REQUEST,
			mockLiferayPortletRenderRequest);

		mockHttpServletRequest.setAttribute(
			JavaConstants.JAKARTA_PORTLET_RESPONSE,
			mockLiferayPortletRenderResponse);
		mockHttpServletRequest.setAttribute(
			WebKeys.THEME_DISPLAY, _getThemeDisplay(layout, signedIn));
		mockHttpServletRequest.setAttribute(
			WebKeys.USER_ID, TestPropsValues.getUserId());

		return _dlViewMVCRenderCommand.render(
			mockLiferayPortletRenderRequest, mockLiferayPortletRenderResponse);
	}

	private void _testAuthorize() throws Exception {
		MockHttpServletRequest mockHttpServletRequest =
			new MockHttpServletRequest();
		MockLiferayPortletRenderResponse mockLiferayPortletRenderResponse =
			new MockLiferayPortletRenderResponse();

		Assert.assertEquals(
			MVCRenderConstants.MVC_PATH_VALUE_SKIP_DISPATCH,
			_render(
				_addLayout(
					_dlAppLocalService.getMountFolder(
						_repository.getRepositoryId())),
				mockHttpServletRequest, mockLiferayPortletRenderResponse,
				true));
		Assert.assertTrue(
			StringUtil.startsWith(
				_getRedirectedURL(mockLiferayPortletRenderResponse),
				"https://login.microsoftonline.com"));

		MockHttpServletRequest callbackMockHttpServletRequest =
			new MockHttpServletRequest();

		callbackMockHttpServletRequest.setParameter(
			"code", RandomTestUtil.randomString());
		callbackMockHttpServletRequest.setSession(
			mockHttpServletRequest.getSession());

		AssertUtils.assertFailure(
			OAuth2AuthorizationException.InvalidState.class,
			"The resource owner returned an invalid state value: The " +
				"Sharepoint server returned an invalid state",
			() -> _strutsAction.execute(
				callbackMockHttpServletRequest, new MockHttpServletResponse()));
	}

	private void _testAuthorizeWhenUserIsNotSignedIn() throws Exception {
		MockLiferayPortletRenderResponse mockLiferayPortletRenderResponse =
			new MockLiferayPortletRenderResponse();

		try {
			_render(
				_addLayout(
					_dlAppLocalService.getMountFolder(
						_repository.getRepositoryId())),
				new MockHttpServletRequest(), mockLiferayPortletRenderResponse,
				false);

			Assert.fail();
		}
		catch (SystemException systemException) {
			Assert.assertTrue(
				systemException.getCause() instanceof PrincipalException);
		}

		Assert.assertNull(_getRedirectedURL(mockLiferayPortletRenderResponse));
	}

	private static final String _FACTORY_PID =
		"com.liferay.sharepoint.rest.repository.internal.configuration." +
			"SharepointRepositoryConfiguration";

	@Inject
	private ClassNameLocalService _classNameLocalService;

	@Inject
	private CompanyLocalService _companyLocalService;

	@Inject
	private DLAppLocalService _dlAppLocalService;

	@Inject(
		filter = "component.name=com.liferay.document.library.web.internal.portlet.action.DLViewMVCRenderCommand"
	)
	private MVCRenderCommand _dlViewMVCRenderCommand;

	@DeleteAfterTestRun
	private Group _group;

	private String _pid;
	private Repository _repository;

	@Inject
	private RepositoryLocalService _repositoryLocalService;

	@Inject(filter = "path=/document_library/sharepoint/oauth2")
	private StrutsAction _strutsAction;

}