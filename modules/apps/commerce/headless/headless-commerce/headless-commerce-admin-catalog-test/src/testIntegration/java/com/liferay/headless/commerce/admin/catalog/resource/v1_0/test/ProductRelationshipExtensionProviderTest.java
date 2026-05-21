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
import com.liferay.object.constants.ObjectDefinitionConstants;
import com.liferay.object.constants.ObjectEntryFolderConstants;
import com.liferay.object.constants.ObjectRelationshipConstants;
import com.liferay.object.field.util.ObjectFieldUtil;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.model.ObjectField;
import com.liferay.object.model.ObjectRelationship;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalServiceUtil;
import com.liferay.object.service.ObjectRelationshipLocalServiceUtil;
import com.liferay.object.test.util.ObjectDefinitionTestUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.portal.vulcan.extension.ExtensionProvider;
import com.liferay.portal.vulcan.fields.NestedFieldsContext;
import com.liferay.portal.vulcan.fields.NestedFieldsContextThreadLocal;
import com.liferay.portal.vulcan.util.LocalizedMapUtil;

import java.io.Serializable;

import java.util.Collections;
import java.util.List;
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
public class ProductRelationshipExtensionProviderTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_customObjectDefinition = _publishCustomObjectDefinition();

		_customObjectEntry = _addObjectEntry();

		_cpDefinition = CPTestUtil.addCPDefinitionFromCatalog(
			CPTestUtil.getSystemCommerceCatalog(
				TestPropsValues.getCompanyId()
			).getGroupId(),
			SimpleCPTypeConstants.NAME, true, true);

		ObjectDefinition cpDefinitionObjectDefinition =
			_objectDefinitionLocalService.fetchObjectDefinitionByClassName(
				TestPropsValues.getCompanyId(), CPDefinition.class.getName());

		_objectRelationship =
			ObjectRelationshipLocalServiceUtil.addObjectRelationship(
				null, TestPropsValues.getUserId(),
				_customObjectDefinition.getObjectDefinitionId(),
				cpDefinitionObjectDefinition.getObjectDefinitionId(), 0,
				ObjectRelationshipConstants.DELETION_TYPE_PREVENT, false,
				LocalizedMapUtil.getLocalizedMap(RandomTestUtil.randomString()),
				StringUtil.randomId(), false,
				ObjectRelationshipConstants.TYPE_MANY_TO_MANY, null);

		ObjectRelationshipLocalServiceUtil.
			addObjectRelationshipMappingTableValues(
				TestPropsValues.getUserId(),
				_objectRelationship.getObjectRelationshipId(),
				_customObjectEntry.getPrimaryKey(),
				_cpDefinition.getCProductId(),
				ServiceContextTestUtil.getServiceContext());

		_originalNestedFieldsContext =
			NestedFieldsContextThreadLocal.getNestedFieldsContext();
	}

	@After
	public void tearDown() throws Exception {
		NestedFieldsContextThreadLocal.setNestedFieldsContext(
			_originalNestedFieldsContext);

		ObjectRelationshipLocalServiceUtil.
			deleteObjectRelationshipMappingTableValues(
				_objectRelationship.getObjectRelationshipId(),
				_customObjectEntry.getPrimaryKey(),
				_cpDefinition.getCProductId());

		ObjectRelationshipLocalServiceUtil.deleteObjectRelationship(
			_objectRelationship);

		_cpDefinitionLocalService.deleteCPDefinition(_cpDefinition);

		_objectDefinitionLocalService.deleteObjectDefinition(
			_customObjectDefinition.getObjectDefinitionId());
	}

	@Test
	public void testGetExtendedPropertiesReturnsRelatedEntries()
		throws Exception {

		Product product = new Product() {
			{
				id = _cpDefinition.getCPDefinitionId();
				productId = _cpDefinition.getCProductId();
			}
		};

		NestedFieldsContextThreadLocal.setNestedFieldsContext(
			new NestedFieldsContext(
				1, Collections.singletonList(_objectRelationship.getName())));

		Map<String, Serializable> extendedProperties =
			_extensionProvider.getExtendedProperties(
				TestPropsValues.getCompanyId(), TestPropsValues.getUserId(),
				Product.class.getName(), product);

		Assert.assertEquals(
			extendedProperties.toString(), 1, extendedProperties.size());
		Assert.assertNotNull(
			extendedProperties.get(_objectRelationship.getName()));
	}

	private ObjectEntry _addObjectEntry() throws Exception {
		return ObjectEntryLocalServiceUtil.addObjectEntry(
			0, TestPropsValues.getUserId(),
			_customObjectDefinition.getObjectDefinitionId(),
			ObjectEntryFolderConstants.PARENT_OBJECT_ENTRY_FOLDER_ID_DEFAULT,
			null,
			HashMapBuilder.<String, Serializable>put(
				_OBJECT_FIELD_NAME, _OBJECT_FIELD_VALUE
			).build(),
			ServiceContextTestUtil.getServiceContext());
	}

	private ObjectDefinition _publishCustomObjectDefinition() throws Exception {
		List<ObjectField> objectFields = Collections.singletonList(
			ObjectFieldUtil.createObjectField(
				"Text", "String", true, true, null,
				RandomTestUtil.randomString(), _OBJECT_FIELD_NAME, false));

		ObjectDefinition objectDefinition =
			_objectDefinitionLocalService.addCustomObjectDefinition(
				null, TestPropsValues.getUserId(), 0, null, true, false, true,
				false, true, false, false, false, false, null,
				LocalizedMapUtil.getLocalizedMap(RandomTestUtil.randomString()),
				ObjectDefinitionTestUtil.getRandomName(), null, null,
				LocalizedMapUtil.getLocalizedMap(RandomTestUtil.randomString()),
				true, ObjectDefinitionConstants.SCOPE_COMPANY,
				ObjectDefinitionConstants.STORAGE_TYPE_DEFAULT,
				Collections.emptyList(), objectFields, Collections.emptyList(),
				new ServiceContext());

		return _objectDefinitionLocalService.publishCustomObjectDefinition(
			TestPropsValues.getUserId(),
			objectDefinition.getObjectDefinitionId());
	}

	private static final String _OBJECT_FIELD_NAME =
		"x" + RandomTestUtil.randomString();

	private static final String _OBJECT_FIELD_VALUE =
		RandomTestUtil.randomString();

	private CPDefinition _cpDefinition;

	@Inject
	private CPDefinitionLocalService _cpDefinitionLocalService;

	private ObjectDefinition _customObjectDefinition;
	private ObjectEntry _customObjectEntry;

	@Inject(
		filter = "component.name=com.liferay.object.rest.internal.vulcan.extension.v1_0.ObjectRelationshipExtensionProvider"
	)
	private ExtensionProvider _extensionProvider;

	@Inject
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	private ObjectRelationship _objectRelationship;
	private NestedFieldsContext _originalNestedFieldsContext;

}