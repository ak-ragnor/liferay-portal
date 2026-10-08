/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.dynamic.data.mapping.form.web.internal.portlet.action;

import com.liferay.bulk.selection.BulkSelection;
import com.liferay.bulk.selection.BulkSelectionAction;
import com.liferay.bulk.selection.BulkSelectionFactory;
import com.liferay.bulk.selection.BulkSelectionRunner;
import com.liferay.dynamic.data.mapping.constants.DDMPortletKeys;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecord;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceRecordService;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.WebKeys;

import jakarta.portlet.ActionRequest;
import jakarta.portlet.ActionResponse;

import java.util.Collections;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Leonardo Barros
 */
@Component(
	property = {
		"jakarta.portlet.name=" + DDMPortletKeys.DYNAMIC_DATA_MAPPING_FORM_ADMIN,
		"mvc.command.name=/dynamic_data_mapping_form/delete_form_instance_record"
	},
	service = MVCActionCommand.class
)
public class DeleteFormInstanceRecordMVCActionCommand
	extends BaseMVCActionCommand {

	@Override
	protected void doProcessAction(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		if (ParamUtil.getBoolean(actionRequest, "selectAll")) {
			_deleteFormInstanceRecords(actionRequest, actionResponse);

			return;
		}

		long[] deleteFormInstanceRecordIds = null;

		long formInstanceRecordId = ParamUtil.getLong(
			actionRequest, "formInstanceRecordId");

		if (formInstanceRecordId > 0) {
			deleteFormInstanceRecordIds = new long[] {formInstanceRecordId};
		}
		else {
			deleteFormInstanceRecordIds = StringUtil.split(
				ParamUtil.getString(
					actionRequest, "deleteFormInstanceRecordIds"),
				0L);
		}

		for (long deleteFormInstanceRecordId : deleteFormInstanceRecordIds) {
			_ddmFormInstanceRecordService.deleteFormInstanceRecord(
				deleteFormInstanceRecordId);
		}
	}

	private void _deleteFormInstanceRecords(
			ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		BulkSelection<DDMFormInstanceRecord> bulkSelection =
			_ddmFormInstanceRecordBulkSelectionFactory.create(
				HashMapBuilder.put(
					"createDate",
					new String[] {String.valueOf(System.currentTimeMillis())}
				).put(
					"formInstanceId",
					new String[] {
						ParamUtil.getString(actionRequest, "formInstanceId")
					}
				).build());

		ThemeDisplay themeDisplay = (ThemeDisplay)actionRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		_bulkSelectionRunner.run(
			themeDisplay.getUser(), bulkSelection,
			_deleteDDMFormInstanceRecordBulkSelectionAction,
			Collections.emptyMap());

		sendRedirect(actionRequest, actionResponse);

		SessionMessages.add(
			actionRequest, "requestProcessed",
			_language.get(
				themeDisplay.getLocale(), "deletion-started-for-all-entries"));
	}

	@Reference
	private BulkSelectionRunner _bulkSelectionRunner;

	@Reference(
		target = "(model.class.name=com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecord)"
	)
	private BulkSelectionFactory<DDMFormInstanceRecord>
		_ddmFormInstanceRecordBulkSelectionFactory;

	@Reference
	private DDMFormInstanceRecordService _ddmFormInstanceRecordService;

	@Reference(
		target = "(bulk.selection.action.key=delete.ddm.form.instance.record)"
	)
	private BulkSelectionAction<DDMFormInstanceRecord>
		_deleteDDMFormInstanceRecordBulkSelectionAction;

	@Reference
	private Language _language;

}