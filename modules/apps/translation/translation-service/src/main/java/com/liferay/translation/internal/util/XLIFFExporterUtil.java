/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.translation.internal.util;

import com.liferay.info.field.InfoField;
import com.liferay.info.field.InfoFieldValue;
import com.liferay.info.field.type.HTMLInfoFieldType;
import com.liferay.info.localized.InfoLocalizedValue;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.xml.Element;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @author Akhash Ramprakash
 */
public class XLIFFExporterUtil {

	public static void addTargetValue(
		Element element, InfoFieldValue<Object> infoFieldValue,
		Locale targetLocale) {

		addTargetValue(element, infoFieldValue, targetLocale, null);
	}

	public static void addTargetValue(
		Element element, InfoFieldValue<Object> infoFieldValue,
		Locale targetLocale, BiConsumer<Element, String> inlineCodeWriter) {

		String targetStringValue = _getTargetStringValue(
			infoFieldValue, targetLocale);

		if (targetStringValue == null) {
			element.addText(StringPool.BLANK);
		}
		else if (inlineCodeWriter != null) {
			inlineCodeWriter.accept(element, targetStringValue);
		}
		else {
			element.addCDATA(targetStringValue);
		}
	}

	public static boolean isProtectedHTMLInfoField(InfoField<?> infoField) {
		if (FeatureFlagManagerUtil.isEnabled("LPP-65073") &&
			Objects.equals(
				infoField.getInfoFieldType(), HTMLInfoFieldType.INSTANCE)) {

			return true;
		}

		return false;
	}

	private static String _getTargetStringValue(
		InfoFieldValue<Object> infoFieldValue, Locale targetLocale) {

		Object value = infoFieldValue.getValue();

		if (value instanceof InfoLocalizedValue) {
			InfoLocalizedValue<?> infoLocalizedValue =
				(InfoLocalizedValue<?>)value;

			Set<Locale> availableLocales =
				infoLocalizedValue.getAvailableLocales();

			if (!availableLocales.contains(targetLocale)) {
				return null;
			}
		}

		Object targetValue = infoFieldValue.getValue(targetLocale);

		if (targetValue == null) {
			return null;
		}

		return targetValue.toString();
	}

}