/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.headless.commerce.admin.catalog.resource.v1_0.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.commerce.product.model.CPDefinition;
import com.liferay.commerce.product.service.CPDefinitionLocalService;
import com.liferay.commerce.product.test.util.CPTestUtil;
import com.liferay.commerce.product.type.simple.constants.SimpleCPTypeConstants;
import com.liferay.headless.commerce.admin.catalog.dto.v1_0.Product;
import com.liferay.object.field.builder.TextObjectFieldBuilder;
import com.liferay.object.field.util.ObjectFieldUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectField;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectFieldLocalService;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.vulcan.extension.ExtensionProvider;
import com.liferay.portal.vulcan.util.LocalizedMapUtil;

import java.io.Serializable;

import java.util.Map;

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
public class ProductExtensionProviderTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		ObjectDefinition objectDefinition =
			_objectDefinitionLocalService.fetchObjectDefinitionByClassName(
				TestPropsValues.getCompanyId(), CPDefinition.class.getName());

		_objectField = ObjectFieldUtil.addCustomObjectField(
			new TextObjectFieldBuilder(
			).userId(
				TestPropsValues.getUserId()
			).objectDefinitionId(
				objectDefinition.getObjectDefinitionId()
			).labelMap(
				LocalizedMapUtil.getLocalizedMap(_OBJECT_FIELD_NAME)
			).name(
				_OBJECT_FIELD_NAME
			).build());

		_cpDefinition = CPTestUtil.addCPDefinitionFromCatalog(
			CPTestUtil.getSystemCommerceCatalog(
				TestPropsValues.getCompanyId()
			).getGroupId(),
			SimpleCPTypeConstants.NAME, true, true);
	}

	@After
	public void tearDown() throws Exception {
		_objectFieldLocalService.deleteObjectField(_objectField);

		_cpDefinitionLocalService.deleteCPDefinition(_cpDefinition);
	}

	@Test
	public void testRoundTripExtensionField() throws Exception {
		Product product = new Product() {
			{
				id = _cpDefinition.getCPDefinitionId();
				productId = _cpDefinition.getCProductId();
			}
		};

		_assertRoundTrip(product);
	}

	@Test
	public void testRoundTripExtensionFieldWithMapEntity() throws Exception {
		_assertRoundTrip(
			HashMapBuilder.<String, Object>put(
				"id", _cpDefinition.getCPDefinitionId()
			).put(
				"productId", _cpDefinition.getCProductId()
			).build());
	}

	private void _assertRoundTrip(Object entity) throws Exception {
		_extensionProvider.setExtendedProperties(
			TestPropsValues.getCompanyId(), TestPropsValues.getUserId(),
			Product.class.getName(), entity,
			HashMapBuilder.<String, Serializable>put(
				_OBJECT_FIELD_NAME, _OBJECT_FIELD_VALUE
			).build());

		Map<String, Serializable> extendedProperties =
			_extensionProvider.getExtendedProperties(
				TestPropsValues.getCompanyId(), TestPropsValues.getUserId(),
				Product.class.getName(), entity);

		Assert.assertEquals(
			_OBJECT_FIELD_VALUE, extendedProperties.get(_OBJECT_FIELD_NAME));
	}

	private static final String _OBJECT_FIELD_NAME =
		"x" + RandomTestUtil.randomString();

	private static final String _OBJECT_FIELD_VALUE =
		RandomTestUtil.randomString();

	private CPDefinition _cpDefinition;

	@Inject
	private CPDefinitionLocalService _cpDefinitionLocalService;

	@Inject(
		filter = "component.name=com.liferay.object.rest.internal.vulcan.extension.v1_0.ObjectEntryExtensionProvider"
	)
	private ExtensionProvider _extensionProvider;

	@Inject
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	private ObjectField _objectField;

	@Inject
	private ObjectFieldLocalService _objectFieldLocalService;

}