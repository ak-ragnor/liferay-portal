/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.journal.internal.upgrade.v6_1_10;

import com.liferay.dynamic.data.mapping.form.field.type.constants.DDMFormFieldTypeConstants;
import com.liferay.dynamic.data.mapping.model.DDMFieldAttribute;
import com.liferay.journal.internal.upgrade.helper.JournalArticleImageUpgradeHelper;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.jdbc.AutoBatchPreparedStatementUtil;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONSerializer;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.util.Validator;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Akhash Ramprakash
 */
public class DocumentLibraryDDMFieldAttributeUpgradeProcess
	extends UpgradeProcess {

	public DocumentLibraryDDMFieldAttributeUpgradeProcess(
		JournalArticleImageUpgradeHelper journalArticleImageUpgradeHelper) {

		_journalArticleImageUpgradeHelper = journalArticleImageUpgradeHelper;
	}

	@Override
	protected void doUpgrade() throws Exception {
		try (PreparedStatement preparedStatement1 = connection.prepareStatement(
				StringBundler.concat(
					"select DDMFieldAttribute.fieldAttributeId, ",
					"DDMFieldAttribute.companyId, DDMFieldAttribute.fieldId, ",
					"DDMFieldAttribute.storageId, ",
					"DDMFieldAttribute.languageId, ",
					"DDMFieldAttribute.largeAttributeValue, ",
					"DDMFieldAttribute.smallAttributeValue from ",
					"DDMFieldAttribute inner join DDMField on ",
					"DDMField.ctCollectionId = ",
					"DDMFieldAttribute.ctCollectionId and DDMField.fieldId = ",
					"DDMFieldAttribute.fieldId where DDMField.fieldType = ? ",
					"and (DDMFieldAttribute.attributeName is null or ",
					"DDMFieldAttribute.attributeName = '') and ",
					"DDMFieldAttribute.ctCollectionId = 0"));
			PreparedStatement preparedStatement2 =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection,
					"delete from DDMFieldAttribute where fieldAttributeId = " +
						"? and ctCollectionId = 0");
			PreparedStatement preparedStatement3 =
				AutoBatchPreparedStatementUtil.concurrentAutoBatch(
					connection,
					StringBundler.concat(
						"insert into DDMFieldAttribute (mvccVersion, ",
						"ctCollectionId, fieldAttributeId, companyId, ",
						"fieldId, storageId, attributeName, languageId, ",
						"largeAttributeValue, smallAttributeValue) values (0, ",
						"0, ?, ?, ?, ?, ?, ?, ?, ?)"))) {

			preparedStatement1.setString(
				1, DDMFormFieldTypeConstants.DOCUMENT_LIBRARY);

			try (ResultSet resultSet = preparedStatement1.executeQuery()) {
				while (resultSet.next()) {
					String url = resultSet.getString("largeAttributeValue");

					if (Validator.isBlank(url)) {
						url = resultSet.getString("smallAttributeValue");
					}

					if (Validator.isBlank(url) ||
						!_journalArticleImageUpgradeHelper.isDocumentLibraryURL(
							url)) {

						continue;
					}

					preparedStatement2.setLong(
						1, resultSet.getLong("fieldAttributeId"));

					preparedStatement2.addBatch();

					long companyId = resultSet.getLong("companyId");
					long fieldId = resultSet.getLong("fieldId");
					long storageId = resultSet.getLong("storageId");
					String languageId = resultSet.getString("languageId");

					JSONObject jsonObject =
						_journalArticleImageUpgradeHelper.
							getDocumentLibraryJSONObject(url);

					if (jsonObject == null) {
						_addDDMFieldAttribute(
							preparedStatement3, companyId, fieldId, storageId,
							StringPool.BLANK, languageId, StringPool.BLANK);

						continue;
					}

					for (String key : jsonObject.keySet()) {
						_addDDMFieldAttribute(
							preparedStatement3, companyId, fieldId, storageId,
							key, languageId,
							_jsonSerializer.serialize(jsonObject.get(key)));
					}
				}
			}

			preparedStatement2.executeBatch();

			preparedStatement3.executeBatch();
		}
	}

	private void _addDDMFieldAttribute(
			PreparedStatement preparedStatement, long companyId, long fieldId,
			long storageId, String attributeName, String languageId,
			String attributeValue)
		throws Exception {

		preparedStatement.setLong(
			1, increment(DDMFieldAttribute.class.getName()));
		preparedStatement.setLong(2, companyId);
		preparedStatement.setLong(3, fieldId);
		preparedStatement.setLong(4, storageId);
		preparedStatement.setString(5, attributeName);
		preparedStatement.setString(6, languageId);

		byte[] bytes = attributeValue.getBytes();

		if (bytes.length > _SMALL_ATTRIBUTE_VALUE_MAX_LENGTH) {
			preparedStatement.setString(7, attributeValue);
			preparedStatement.setString(8, null);
		}
		else {
			preparedStatement.setString(7, null);
			preparedStatement.setString(8, attributeValue);
		}

		preparedStatement.addBatch();
	}

	private static final int _SMALL_ATTRIBUTE_VALUE_MAX_LENGTH = 255;

	private final JournalArticleImageUpgradeHelper
		_journalArticleImageUpgradeHelper;
	private final JSONSerializer _jsonSerializer =
		JSONFactoryUtil.createJSONSerializer();

}