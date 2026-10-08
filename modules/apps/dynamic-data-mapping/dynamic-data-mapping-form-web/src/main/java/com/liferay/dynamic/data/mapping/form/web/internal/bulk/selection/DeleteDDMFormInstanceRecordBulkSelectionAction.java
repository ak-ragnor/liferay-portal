/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.dynamic.data.mapping.form.web.internal.bulk.selection;

import com.liferay.bulk.selection.BulkSelection;
import com.liferay.bulk.selection.BulkSelectionAction;
import com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecord;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceRecordLocalService;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermission;

import java.io.Serializable;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Akhash Ramprakash
 */
@Component(
	property = "bulk.selection.action.key=delete.ddm.form.instance.record",
	service = BulkSelectionAction.class
)
public class DeleteDDMFormInstanceRecordBulkSelectionAction
	implements BulkSelectionAction<DDMFormInstanceRecord> {

	@Override
	public void execute(
			User user, BulkSelection<DDMFormInstanceRecord> bulkSelection,
			Map<String, Serializable> inputMap)
		throws Exception {

		PermissionChecker permissionChecker =
			PermissionCheckerFactoryUtil.create(user);

		bulkSelection.forEach(
			ddmFormInstanceRecord -> {
				try {
					if (!_ddmFormInstanceRecordModelResourcePermission.contains(
							permissionChecker, ddmFormInstanceRecord,
							ActionKeys.DELETE)) {

						return;
					}

					_ddmFormInstanceRecordLocalService.deleteFormInstanceRecord(
						ddmFormInstanceRecord);
				}
				catch (Exception exception) {
					if (_log.isWarnEnabled()) {
						_log.warn(exception);
					}
				}
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DeleteDDMFormInstanceRecordBulkSelectionAction.class);

	@Reference
	private DDMFormInstanceRecordLocalService
		_ddmFormInstanceRecordLocalService;

	@Reference(
		target = "(model.class.name=com.liferay.dynamic.data.mapping.model.DDMFormInstanceRecord)"
	)
	private ModelResourcePermission<DDMFormInstanceRecord>
		_ddmFormInstanceRecordModelResourcePermission;

}