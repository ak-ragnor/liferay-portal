/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.dynamic.data.mapping.internal.bulk.selection.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.bulk.selection.BulkSelectionAction;
import com.liferay.bulk.selection.BulkSelectionFactory;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormInstance;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecord;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceReport;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceRecordLocalService;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceReportLocalService;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.test.util.DDMFormInstanceRecordTestUtil;
import com.liferay.dynamic.data.mapping.test.util.DDMFormInstanceTestUtil;
import com.liferay.dynamic.data.mapping.test.util.DDMFormTestUtil;
import com.liferay.dynamic.data.mapping.test.util.DDMFormValuesTestUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Akhash Ramprakash
 */
@RunWith(Arquillian.class)
public class DeleteDDMFormInstanceRecordBulkSelectionActionTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testExecuteWhenDeleteFails() throws Exception {
		_group = GroupTestUtil.addGroup();

		DDMForm ddmForm = DDMFormTestUtil.createDDMForm();

		ddmForm.addDDMFormField(
			DDMFormTestUtil.createNumericDDMFormField(
				"number", "number", "integer", false, false, false,
				StringPool.BLANK, StringPool.BLANK, StringPool.BLANK));

		DDMFormInstance ddmFormInstance =
			DDMFormInstanceTestUtil.addDDMFormInstance(
				ddmForm, _group, TestPropsValues.getUserId());

		long[] formInstanceRecordIds = new long[5];

		for (int i = 0; i < formInstanceRecordIds.length; i++) {
			DDMFormValues ddmFormValues =
				DDMFormValuesTestUtil.createDDMFormValues(ddmForm);

			ddmFormValues.addDDMFormFieldValue(
				DDMFormValuesTestUtil.createUnlocalizedDDMFormFieldValue(
					"number", String.valueOf(i + 1)));

			DDMFormInstanceRecord ddmFormInstanceRecord =
				DDMFormInstanceRecordTestUtil.addDDMFormInstanceRecord(
					ddmFormInstance, ddmFormValues, _group,
					TestPropsValues.getUserId());

			formInstanceRecordIds[i] =
				ddmFormInstanceRecord.getFormInstanceRecordId();
		}

		ServiceRegistration<ModelListener<DDMFormInstanceRecord>>
			serviceRegistration = _registerModelListener(
				formInstanceRecordIds[1], formInstanceRecordIds[3]);

		try {
			_bulkSelectionAction.execute(
				TestPropsValues.getUser(),
				_bulkSelectionFactory.create(
					HashMapBuilder.put(
						"createDate",
						new String[] {
							String.valueOf(System.currentTimeMillis())
						}
					).put(
						"formInstanceId",
						new String[] {
							String.valueOf(ddmFormInstance.getFormInstanceId())
						}
					).build()),
				new HashMap<>());
		}
		finally {
			serviceRegistration.unregister();
		}

		for (int i = 0; i < formInstanceRecordIds.length; i++) {
			DDMFormInstanceRecord ddmFormInstanceRecord =
				_ddmFormInstanceRecordLocalService.fetchFormInstanceRecord(
					formInstanceRecordIds[i]);

			if ((i == 1) || (i == 3)) {
				Assert.assertNotNull(ddmFormInstanceRecord);
			}
			else {
				Assert.assertNull(ddmFormInstanceRecord);
			}
		}

		DDMFormInstanceReport ddmFormInstanceReport =
			_ddmFormInstanceReportLocalService.
				getFormInstanceReportByFormInstanceId(
					ddmFormInstance.getFormInstanceId());

		JSONObject dataJSONObject = JSONFactoryUtil.createJSONObject(
			ddmFormInstanceReport.getData());

		JSONObject numberJSONObject = dataJSONObject.getJSONObject("number");

		JSONObject summaryJSONObject = numberJSONObject.getJSONObject(
			"summary");

		Assert.assertEquals(2, dataJSONObject.getInt("totalItems"));
		Assert.assertEquals(2, numberJSONObject.getInt("totalEntries"));
		Assert.assertEquals("4", summaryJSONObject.getString("max"));
		Assert.assertEquals(1, summaryJSONObject.getInt("maxCount"));
		Assert.assertEquals("2", summaryJSONObject.getString("min"));
		Assert.assertEquals(1, summaryJSONObject.getInt("minCount"));
		Assert.assertEquals("6", summaryJSONObject.getString("sum"));
	}

	private ServiceRegistration<ModelListener<DDMFormInstanceRecord>>
		_registerModelListener(long... formInstanceRecordIds) {

		Bundle bundle = FrameworkUtil.getBundle(
			DeleteDDMFormInstanceRecordBulkSelectionActionTest.class);

		BundleContext bundleContext = bundle.getBundleContext();

		return bundleContext.registerService(
			(Class<ModelListener<DDMFormInstanceRecord>>)
				(Class<?>)ModelListener.class,
			new BaseModelListener<DDMFormInstanceRecord>() {

				@Override
				public void onBeforeRemove(
						DDMFormInstanceRecord ddmFormInstanceRecord)
					throws ModelListenerException {

					if (ArrayUtil.contains(
							formInstanceRecordIds,
							ddmFormInstanceRecord.getFormInstanceRecordId())) {

						throw new ModelListenerException();
					}
				}

			},
			null);
	}

	@Inject(
		filter = "bulk.selection.action.key=delete.ddm.form.instance.record"
	)
	private BulkSelectionAction<DDMFormInstanceRecord> _bulkSelectionAction;

	@Inject(
		filter = "model.class.name=com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecord"
	)
	private BulkSelectionFactory<DDMFormInstanceRecord> _bulkSelectionFactory;

	@Inject
	private DDMFormInstanceRecordLocalService
		_ddmFormInstanceRecordLocalService;

	@Inject
	private DDMFormInstanceReportLocalService
		_ddmFormInstanceReportLocalService;

	@DeleteAfterTestRun
	private Group _group;

}