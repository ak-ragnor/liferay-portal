/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.upgrade.v7_4_x;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.ArrayUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Akhash Ramprakash
 */
public class LayoutCompanyIdUpgradeProcess extends UpgradeProcess {

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select Layout.plid, Layout.companyId, Group_.companyId ",
					"as groupCompanyId from Layout inner join Group_ on ",
					"Group_.groupId = Layout.groupId where ",
					"Layout.ctCollectionId = 0 and Group_.ctCollectionId = 0 ",
					"and Layout.companyId <> Group_.companyId"));

			ResultSet resultSet = preparedStatement.executeQuery()) {

			while (resultSet.next()) {
				_upgradeLayout(
					resultSet.getLong("companyId"),
					resultSet.getLong("groupCompanyId"),
					resultSet.getLong("plid"));
			}
		}
	}

	private long _getSystemRoleId(long companyId, long roleId)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select Role_.name, SystemRole.roleId as systemRoleId ",
					"from Role_ left join Role_ SystemRole on ",
					"SystemRole.ctCollectionId = 0 and SystemRole.companyId = ",
					"? and SystemRole.name = Role_.name where ",
					"Role_.ctCollectionId = 0 and Role_.roleId = ?"))) {

			preparedStatement.setLong(1, companyId);
			preparedStatement.setLong(2, roleId);

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				if (!resultSet.next()) {
					return 0;
				}

				String name = resultSet.getString("name");

				if (!ArrayUtil.contains(RoleConstants.SYSTEM_ROLES, name) &&
					!ArrayUtil.contains(
						RoleConstants.SYSTEM_SITE_ROLES, name)) {

					return 0;
				}

				return resultSet.getLong("systemRoleId");
			}
		}
	}

	private void _upgradeLayout(long companyId, long groupCompanyId, long plid)
		throws Exception {

		runSQL(
			StringBundler.concat(
				"update Layout set companyId = ", groupCompanyId,
				" where ctCollectionId = 0 and plid = ", plid));
		runSQL(
			StringBundler.concat(
				"update LayoutFriendlyURL set companyId = ", groupCompanyId,
				" where ctCollectionId = 0 and plid = ", plid));

		_upgradeResourcePermissions(companyId, groupCompanyId, plid);
	}

	private void _upgradeResourcePermission(
			long actionIds, long groupCompanyId, long plid,
			long resourcePermissionId, long roleId)
		throws Exception {

		if (roleId != 0) {
			try (PreparedStatement preparedStatement =
					connection.prepareStatement(
						StringBundler.concat(
							"select resourcePermissionId, actionIds from ",
							"ResourcePermission where ctCollectionId = 0 and ",
							"companyId = ? and name = ? and scope = ? and ",
							"primKey = ? and roleId = ?"))) {

				preparedStatement.setLong(1, groupCompanyId);
				preparedStatement.setString(2, Layout.class.getName());
				preparedStatement.setInt(3, ResourceConstants.SCOPE_INDIVIDUAL);
				preparedStatement.setString(4, String.valueOf(plid));
				preparedStatement.setLong(5, roleId);

				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					if (!resultSet.next()) {
						runSQL(
							StringBundler.concat(
								"update ResourcePermission set companyId = ",
								groupCompanyId, ", roleId = ", roleId,
								" where ctCollectionId = 0 and ",
								"resourcePermissionId = ",
								resourcePermissionId));

						return;
					}

					actionIds |= resultSet.getLong("actionIds");

					runSQL(
						StringBundler.concat(
							"update ResourcePermission set actionIds = ",
							actionIds, ", viewActionId = ",
							((actionIds % 2) == 1) ? "[$TRUE$]" : "[$FALSE$]",
							" where ctCollectionId = 0 and ",
							"resourcePermissionId = ",
							resultSet.getLong("resourcePermissionId")));
				}
			}
		}

		runSQL(
			StringBundler.concat(
				"delete from ResourcePermission where ctCollectionId = 0 and ",
				"resourcePermissionId = ", resourcePermissionId));
	}

	private void _upgradeResourcePermissions(
			long companyId, long groupCompanyId, long plid)
		throws Exception {

		try (PreparedStatement preparedStatement = connection.prepareStatement(
				StringBundler.concat(
					"select resourcePermissionId, roleId, actionIds from ",
					"ResourcePermission where ctCollectionId = 0 and ",
					"companyId = ? and name = ? and scope = ? and primKey = ",
					"?"))) {

			preparedStatement.setLong(1, companyId);
			preparedStatement.setString(2, Layout.class.getName());
			preparedStatement.setInt(3, ResourceConstants.SCOPE_INDIVIDUAL);
			preparedStatement.setString(4, String.valueOf(plid));

			try (ResultSet resultSet = preparedStatement.executeQuery()) {
				while (resultSet.next()) {
					_upgradeResourcePermission(
						resultSet.getLong("actionIds"), groupCompanyId, plid,
						resultSet.getLong("resourcePermissionId"),
						_getSystemRoleId(
							groupCompanyId, resultSet.getLong("roleId")));
				}
			}
		}
	}

}