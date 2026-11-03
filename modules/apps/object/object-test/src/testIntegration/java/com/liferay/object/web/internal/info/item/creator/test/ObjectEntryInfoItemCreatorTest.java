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
import com.liferay.object.constants.ObjectActionKeys;
import com.liferay.object.constants.ObjectEntryFolderConstants;
import com.liferay.object.constants.ObjectRelationshipConstants;
import com.liferay.object.field.builder.TextObjectFieldBuilder;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.model.ObjectField;
import com.liferay.object.model.ObjectRelationship;
import com.liferay.object.related.models.ObjectRelatedModelsProvider;
import com.liferay.object.related.models.ObjectRelatedModelsProviderRegistry;
import com.liferay.object.related.models.test.util.ObjectEntryTestUtil;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectRelationshipLocalService;
import com.liferay.object.test.util.ObjectDefinitionTestUtil;
import com.liferay.object.test.util.ObjectRelationshipTestUtil;
import com.liferay.object.web.internal.info.item.BaseObjectEntryInfoItemTestCase;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.InfoFormException;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ResourcePermissionLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.OrganizationTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.RoleTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.test.util.UserTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.vulcan.util.LocalizedMapUtil;

import java.io.Serializable;

import java.util.Collections;
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

		_objectDefinition = _publishObjectDefinition();

		ObjectRelationship objectRelationship =
			ObjectRelationshipTestUtil.addObjectRelationship(
				_objectRelationshipLocalService, objectDefinition2,
				_objectDefinition,
				ObjectRelationshipConstants.DELETION_TYPE_DISASSOCIATE,
				StringUtil.randomId(),
				ObjectRelationshipConstants.TYPE_MANY_TO_MANY);

		ObjectEntry relatedObjectEntry = ObjectEntryTestUtil.addObjectEntry(
			0, _objectDefinition.getObjectDefinitionId(),
			HashMapBuilder.<String, Serializable>put(
				"name", RandomTestUtil.randomString()
			).build());

		InfoItemCreator<ObjectEntry> infoItemCreator =
			infoItemServiceRegistry.getFirstInfoItemService(
				InfoItemCreator.class, objectDefinition2.getClassName());

		int count = objectEntryLocalService.getObjectEntriesCount(
			objectDefinition2.getObjectDefinitionId());

		try {
			infoItemCreator.createFromInfoItemFieldValues(
				0,
				_getInfoItemFieldValues(
					objectRelationship,
					String.valueOf(RandomTestUtil.randomLong())),
				WorkflowConstants.STATUS_APPROVED);

			Assert.fail();
		}
		catch (InfoFormException infoFormException) {
			Assert.assertEquals(
				count,
				objectEntryLocalService.getObjectEntriesCount(
					objectDefinition2.getObjectDefinitionId()));
		}

		objectEntry2 = infoItemCreator.createFromInfoItemFieldValues(
			0,
			_getInfoItemFieldValues(
				objectRelationship,
				String.valueOf(relatedObjectEntry.getObjectEntryId())),
			WorkflowConstants.STATUS_APPROVED);

		List<ObjectEntry> objectEntries =
			objectEntryLocalService.getManyToManyObjectEntries(
				0, objectRelationship.getObjectRelationshipId(),
				objectEntry2.getObjectEntryId(), true, false, null,
				QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		Assert.assertEquals(objectEntries.toString(), 1, objectEntries.size());

		ObjectEntry objectEntry = objectEntries.get(0);

		Assert.assertEquals(
			relatedObjectEntry.getObjectEntryId(),
			objectEntry.getObjectEntryId());
	}

	@Test
	public void testCreateFromInfoItemFieldValuesWithManyToManyObjectRelationshipWithoutViewPermission()
		throws Exception {

		_objectDefinition = _publishObjectDefinition();

		ObjectRelationship objectRelationship =
			ObjectRelationshipTestUtil.addObjectRelationship(
				_objectRelationshipLocalService, objectDefinition2,
				_objectDefinition,
				ObjectRelationshipConstants.DELETION_TYPE_DISASSOCIATE,
				StringUtil.randomId(),
				ObjectRelationshipConstants.TYPE_MANY_TO_MANY);

		ObjectEntry relatedObjectEntry = objectEntryLocalService.addObjectEntry(
			0, TestPropsValues.getUserId(),
			_objectDefinition.getObjectDefinitionId(),
			ObjectEntryFolderConstants.PARENT_OBJECT_ENTRY_FOLDER_ID_DEFAULT,
			null,
			HashMapBuilder.<String, Serializable>put(
				"name", RandomTestUtil.randomString()
			).build(),
			new ServiceContext());

		Role role = RoleTestUtil.addRole(RoleConstants.TYPE_REGULAR);

		_resourcePermissionLocalService.addResourcePermission(
			TestPropsValues.getCompanyId(), objectDefinition2.getResourceName(),
			ResourceConstants.SCOPE_COMPANY,
			String.valueOf(TestPropsValues.getCompanyId()), role.getRoleId(),
			ObjectActionKeys.ADD_OBJECT_ENTRY);

		_user = UserTestUtil.addUser();

		_userLocalService.addRoleUser(role.getRoleId(), _user);

		PermissionChecker permissionChecker =
			PermissionThreadLocal.getPermissionChecker();

		PermissionThreadLocal.setPermissionChecker(
			PermissionCheckerFactoryUtil.create(_user));

		InfoItemCreator<ObjectEntry> infoItemCreator =
			infoItemServiceRegistry.getFirstInfoItemService(
				InfoItemCreator.class, objectDefinition2.getClassName());

		int count = objectEntryLocalService.getObjectEntriesCount(
			objectDefinition2.getObjectDefinitionId());

		try {
			try {
				infoItemCreator.createFromInfoItemFieldValues(
					0,
					_getInfoItemFieldValues(
						objectRelationship,
						String.valueOf(relatedObjectEntry.getObjectEntryId())),
					WorkflowConstants.STATUS_APPROVED);

				Assert.fail();
			}
			catch (InfoFormException infoFormException) {
				Assert.assertEquals(
					count,
					objectEntryLocalService.getObjectEntriesCount(
						objectDefinition2.getObjectDefinitionId()));
			}

			_resourcePermissionLocalService.setResourcePermissions(
				TestPropsValues.getCompanyId(),
				_objectDefinition.getClassName(),
				ResourceConstants.SCOPE_INDIVIDUAL,
				String.valueOf(relatedObjectEntry.getObjectEntryId()),
				role.getRoleId(), new String[] {ActionKeys.VIEW});

			objectEntry2 = infoItemCreator.createFromInfoItemFieldValues(
				0,
				_getInfoItemFieldValues(
					objectRelationship,
					String.valueOf(relatedObjectEntry.getObjectEntryId())),
				WorkflowConstants.STATUS_APPROVED);
		}
		finally {
			PermissionThreadLocal.setPermissionChecker(permissionChecker);
		}

		Assert.assertEquals(
			1,
			objectEntryLocalService.getManyToManyObjectEntriesCount(
				0, objectRelationship.getObjectRelationshipId(),
				objectEntry2.getObjectEntryId(), true, false, null));
	}

	@Test
	public void testCreateFromInfoItemFieldValuesWithManyToManySystemObjectRelationship()
		throws Exception {

		ObjectDefinition objectDefinition =
			_objectDefinitionLocalService.fetchSystemObjectDefinition(
				TestPropsValues.getCompanyId(), "Organization");

		ObjectRelationship objectRelationship =
			ObjectRelationshipTestUtil.addObjectRelationship(
				_objectRelationshipLocalService, objectDefinition2,
				objectDefinition,
				ObjectRelationshipConstants.DELETION_TYPE_DISASSOCIATE,
				StringUtil.randomId(),
				ObjectRelationshipConstants.TYPE_MANY_TO_MANY);

		InfoItemCreator<ObjectEntry> infoItemCreator =
			infoItemServiceRegistry.getFirstInfoItemService(
				InfoItemCreator.class, objectDefinition2.getClassName());

		int count = objectEntryLocalService.getObjectEntriesCount(
			objectDefinition2.getObjectDefinitionId());

		try {
			infoItemCreator.createFromInfoItemFieldValues(
				0,
				_getInfoItemFieldValues(
					objectRelationship,
					String.valueOf(RandomTestUtil.randomLong())),
				WorkflowConstants.STATUS_APPROVED);

			Assert.fail();
		}
		catch (InfoFormException infoFormException) {
			Assert.assertEquals(
				count,
				objectEntryLocalService.getObjectEntriesCount(
					objectDefinition2.getObjectDefinitionId()));
		}

		_organization = OrganizationTestUtil.addOrganization();

		objectEntry2 = infoItemCreator.createFromInfoItemFieldValues(
			0,
			_getInfoItemFieldValues(
				objectRelationship,
				String.valueOf(_organization.getOrganizationId())),
			WorkflowConstants.STATUS_APPROVED);

		ObjectRelatedModelsProvider<?> objectRelatedModelsProvider =
			_objectRelatedModelsProviderRegistry.getObjectRelatedModelsProvider(
				objectDefinition.getClassName(), TestPropsValues.getCompanyId(),
				ObjectRelationshipConstants.TYPE_MANY_TO_MANY);

		Assert.assertEquals(
			1,
			objectRelatedModelsProvider.getRelatedModelsCount(
				0, objectRelationship.getObjectRelationshipId(), null,
				objectEntry2.getObjectEntryId(), null));
	}

	private InfoItemFieldValues _getInfoItemFieldValues(
		ObjectRelationship objectRelationship, String relatedObjectEntryId) {

		return InfoItemFieldValues.builder(
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
				Collections.singletonList(relatedObjectEntryId))
		).build();
	}

	private ObjectDefinition _publishObjectDefinition() throws Exception {
		return ObjectDefinitionTestUtil.publishObjectDefinition(
			Collections.singletonList(
				new TextObjectFieldBuilder(
				).labelMap(
					LocalizedMapUtil.getLocalizedMap(
						RandomTestUtil.randomString())
				).name(
					"name"
				).build()),
			false);
	}

	@DeleteAfterTestRun
	private ObjectDefinition _objectDefinition;

	@Inject
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Inject
	private ObjectRelatedModelsProviderRegistry
		_objectRelatedModelsProviderRegistry;

	@Inject
	private ObjectRelationshipLocalService _objectRelationshipLocalService;

	@DeleteAfterTestRun
	private Organization _organization;

	@Inject
	private ResourcePermissionLocalService _resourcePermissionLocalService;

	@DeleteAfterTestRun
	private User _user;

	@Inject
	private UserLocalService _userLocalService;

}