/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.dynamic.data.mapping.form.field.type.internal.image;

import com.liferay.dynamic.data.mapping.form.field.type.BaseDDMFormFieldType;
import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldType;
import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldTypeSettings;
import com.liferay.dynamic.data.mapping.form.field.type.constants.DDMFormFieldTypeConstants;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Validator;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Carlos Lancha
 */
@Component(
	property = {
		"ddm.form.field.type.data.domain=image",
		"ddm.form.field.type.description=image-field-type-description",
		"ddm.form.field.type.display.order:Integer=8",
		"ddm.form.field.type.group=basic", "ddm.form.field.type.icon=picture",
		"ddm.form.field.type.label=image-field-type-label",
		"ddm.form.field.type.name=" + DDMFormFieldTypeConstants.IMAGE,
		"ddm.form.field.type.scope=document-library,forms,journal"
	},
	service = DDMFormFieldType.class
)
public class ImageDDMFormFieldType extends BaseDDMFormFieldType {

	@Override
	public Class<? extends DDMFormFieldTypeSettings>
		getDDMFormFieldTypeSettings() {

		return ImageDDMFormFieldTypeSettings.class;
	}

	@Override
	public String getESModule() {
		return "{ImagePicker} from dynamic-data-mapping-form-field-type";
	}

	@Override
	public String getName() {
		return DDMFormFieldTypeConstants.IMAGE;
	}

	@Override
	public boolean isPredefinedValueEmpty(String value) {
		if (super.isPredefinedValueEmpty(value)) {
			return true;
		}

		try {
			JSONObject jsonObject = _jsonFactory.createJSONObject(value);

			for (String key : jsonObject.keySet()) {
				if (Validator.isNotNull(jsonObject.getString(key))) {
					return false;
				}
			}

			return true;
		}
		catch (JSONException jsonException) {
			if (_log.isDebugEnabled()) {
				_log.debug(jsonException);
			}

			return false;
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ImageDDMFormFieldType.class);

	@Reference
	private JSONFactory _jsonFactory;

}