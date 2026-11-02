/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.object.web.internal.info.item.creator.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.info.field.InfoField;
import com.liferay.info.field.InfoFieldValue;
import com.liferay.info.field.type.RelationshipInfoFieldType;
import com.liferay.info.field.type.TextInfoFieldType;
import com.liferay.info.item.InfoItemFieldValues;
import com.liferay.info.item.creator.InfoItemCreator;
import com.liferay.object.constants.ObjectRelationshipConstants;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.model.ObjectField;
import com.liferay.object.model.ObjectRelationship;
import com.liferay.object.related.models.ObjectRelatedModelsProvider;
import com.liferay.object.related.models.ObjectRelatedModelsProviderRegistry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectRelationshipLocalService;
import com.liferay.object.test.util.ObjectRelationshipTestUtil;
import com.liferay.object.web.internal.info.item.BaseObjectEntryInfoItemTestCase;
import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.OrganizationTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.test.rule.Inject;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Carolina Barbosa
 */
@RunWith(Arquillian.class)
public class ObjectEntryInfoItemCreatorTest
	extends BaseObjectEntryInfoItemTestCase {

	@Test
	public void testCreateFromInfoItemFieldValues() throws Exception {
		InfoItemCreator<ObjectEntry> infoItemCreator =
			infoItemServiceRegistry.getFirstInfoItemService(
				InfoItemCreator.class, objectDefinition2.getClassName());

		String name1 = RandomTestUtil.randomString();
		String name2 = RandomTestUtil.randomString();

		objectEntry2 = infoItemCreator.createFromInfoItemFieldValues(
			0, geInfoItemFieldValues(name1, name2),
			WorkflowConstants.STATUS_APPROVED);

		assertObjectEntryValues(name1, name2);
	}

	@Test
	public void testCreateFromInfoItemFieldValuesWithManyToManyObjectRelationship()
		throws Exception {

		ObjectDefinition organizationObjectDefinition =
			_objectDefinitionLocalService.fetchObjectDefinitionByClassName(
				TestPropsValues.getCompanyId(), Organization.class.getName());

		ObjectRelationship objectRelationship =
			ObjectRelationshipTestUtil.addObjectRelationship(
				_objectRelationshipLocalService, objectDefinition2,
				organizationObjectDefinition,
				ObjectRelationshipConstants.DELETION_TYPE_DISASSOCIATE,
				StringUtil.randomId(),
				ObjectRelationshipConstants.TYPE_MANY_TO_MANY);

		_organizations.add(OrganizationTestUtil.addOrganization());
		_organizations.add(OrganizationTestUtil.addOrganization());

		InfoItemCreator<ObjectEntry> infoItemCreator =
			infoItemServiceRegistry.getFirstInfoItemService(
				InfoItemCreator.class, objectDefinition2.getClassName());

		ObjectEntry objectEntry = infoItemCreator.createFromInfoItemFieldValues(
			0,
			InfoItemFieldValues.builder(
			).infoFieldValue(
				new InfoFieldValue<>(
					InfoField.builder(
					).infoFieldType(
						TextInfoFieldType.INSTANCE
					).namespace(
						ObjectField.class.getSimpleName()
					).name(
						"name"
					).build(),
					RandomTestUtil.randomString())
			).infoFieldValue(
				new InfoFieldValue<>(
					InfoField.builder(
					).infoFieldType(
						RelationshipInfoFieldType.INSTANCE
					).namespace(
						ObjectField.class.getSimpleName()
					).name(
						ObjectRelationshipConstants.
							OBJECT_RELATIONSHIP_FIELD_NAME_PREFIX +
								objectRelationship.getName()
					).attribute(
						RelationshipInfoFieldType.MULTIPLE, true
					).build(),
					TransformUtil.transform(
						_organizations,
						organization -> String.valueOf(
							organization.getOrganizationId())))
			).build(),
			WorkflowConstants.STATUS_APPROVED);

		ObjectRelatedModelsProvider<Organization> objectRelatedModelsProvider =
			_objectRelatedModelsProviderRegistry.getObjectRelatedModelsProvider(
				organizationObjectDefinition.getClassName(),
				organizationObjectDefinition.getCompanyId(),
				ObjectRelationshipConstants.TYPE_MANY_TO_MANY);

		List<Organization> organizations =
			objectRelatedModelsProvider.getRelatedModels(
				0, objectRelationship.getObjectRelationshipId(), null, false,
				objectEntry.getObjectEntryId(), null, QueryUtil.ALL_POS,
				QueryUtil.ALL_POS, null);

		Assert.assertEquals(
			ListUtil.sort(
				TransformUtil.transform(
					_organizations, Organization::getOrganizationId)),
			ListUtil.sort(
				TransformUtil.transform(
					organizations, Organization::getOrganizationId)));
	}

	@Inject
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Inject
	private ObjectRelatedModelsProviderRegistry
		_objectRelatedModelsProviderRegistry;

	@Inject
	private ObjectRelationshipLocalService _objectRelationshipLocalService;

	@DeleteAfterTestRun
	private final List<Organization> _organizations = new ArrayList<>();

}