/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.translation.internal.exporter;

import com.liferay.info.field.InfoField;
import com.liferay.info.field.InfoFieldValue;
import com.liferay.info.item.ClassPKInfoItemIdentifier;
import com.liferay.info.item.InfoItemFieldValues;
import com.liferay.info.item.InfoItemIdentifier;
import com.liferay.info.item.InfoItemReference;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.petra.string.StringUtil;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.translation.exporter.TranslationInfoItemFieldValuesExporter;
import com.liferay.translation.info.field.TranslationInfoFieldChecker;
import com.liferay.translation.internal.util.XLIFF20InlineCodeWriter;
import com.liferay.translation.internal.util.XLIFFExporterUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Alejandro Tardín
 */
@Component(
	property = "content.type=application/xliff+xml",
	service = TranslationInfoItemFieldValuesExporter.class
)
public class XLIFF20InfoFormTranslationExporter
	implements TranslationInfoItemFieldValuesExporter {

	@Override
	public InputStream exportInfoItemFieldValues(
			InfoItemFieldValues infoItemFieldValues, Locale sourceLocale,
			Locale targetLocale)
		throws IOException {

		Document document = SAXReaderUtil.createDocument();

		Element xliffElement = document.addElement(
			"xliff", "urn:oasis:names:tc:xliff:document:2.0");

		xliffElement.addAttribute(
			"srcLang", LocaleUtil.toBCP47LanguageId(sourceLocale));
		xliffElement.addAttribute(
			"trgLang", LocaleUtil.toBCP47LanguageId(targetLocale));
		xliffElement.addAttribute("version", "2.0");

		Element fileElement = xliffElement.addElement("file");

		InfoItemReference infoItemReference =
			infoItemFieldValues.getInfoItemReference();

		InfoItemIdentifier infoItemIdentifier =
			infoItemReference.getInfoItemIdentifier();

		if (!(infoItemIdentifier instanceof ClassPKInfoItemIdentifier)) {
			return null;
		}

		ClassPKInfoItemIdentifier classPKInfoItemIdentifier =
			(ClassPKInfoItemIdentifier)
				infoItemReference.getInfoItemIdentifier();

		String className = StringUtil.replace(
			infoItemReference.getClassName(), CharPool.POUND,
			CharPool.UNDERLINE);

		fileElement.addAttribute(
			"id",
			className + StringPool.COLON +
				classPKInfoItemIdentifier.getClassPK());

		Map<String, List<InfoFieldValue<Object>>> infoFieldValuesMap =
			new LinkedHashMap<>();

		for (InfoFieldValue<Object> infoFieldValue :
				infoItemFieldValues.getInfoFieldValues()) {

			InfoField infoField = infoFieldValue.getInfoField();

			if (_translationInfoFieldChecker.isTranslatable(infoField)) {
				List<InfoFieldValue<Object>> infoFieldValuesList =
					infoFieldValuesMap.computeIfAbsent(
						infoField.getUniqueId(), uniqueId -> new ArrayList<>());

				infoFieldValuesList.add(infoFieldValue);
			}
		}

		for (Map.Entry<String, List<InfoFieldValue<Object>>> entry :
				infoFieldValuesMap.entrySet()) {

			Element unitElement = fileElement.addElement("unit");

			unitElement.addAttribute("id", entry.getKey());

			List<InfoFieldValue<Object>> infoFieldValues = entry.getValue();

			XLIFF20InlineCodeWriter xliff20InlineCodeWriter = null;

			InfoFieldValue<Object> firstInfoFieldValue = infoFieldValues.get(0);

			if (XLIFFExporterUtil.isProtectedHTMLInfoField(
					firstInfoFieldValue.getInfoField())) {

				xliff20InlineCodeWriter = new XLIFF20InlineCodeWriter(
					unitElement);
			}

			for (InfoFieldValue<Object> infoFieldValue : infoFieldValues) {
				_addInfoFieldValue(
					infoFieldValue, unitElement, sourceLocale, targetLocale,
					xliff20InlineCodeWriter);
			}

			if (xliff20InlineCodeWriter != null) {
				xliff20InlineCodeWriter.finish();
			}
		}

		if (FeatureFlagManagerUtil.isEnabled("LPP-65073")) {
			String xml = document.asXML();

			return new ByteArrayInputStream(xml.getBytes());
		}

		String formattedString = document.formattedString();

		return new ByteArrayInputStream(formattedString.getBytes());
	}

	@Override
	public String getMimeType() {
		return "application/xliff+xml";
	}

	private void _addInfoFieldValue(
		InfoFieldValue<Object> infoFieldValue, Element unitElement,
		Locale sourceLocale, Locale targetLocale,
		XLIFF20InlineCodeWriter xliff20InlineCodeWriter) {

		Element segmentElement = unitElement.addElement("segment");

		Element sourceElement = segmentElement.addElement("source");

		if (xliff20InlineCodeWriter != null) {
			xliff20InlineCodeWriter.write(
				sourceElement,
				_getStringValue(infoFieldValue.getValue(sourceLocale)));
		}
		else {
			sourceElement.addCDATA(
				_getStringValue(infoFieldValue.getValue(sourceLocale)));
		}

		Element targetElement = segmentElement.addElement("target");

		BiConsumer<Element, String> inlineCodeWriter = null;

		if (xliff20InlineCodeWriter != null) {
			inlineCodeWriter = xliff20InlineCodeWriter::write;
		}

		XLIFFExporterUtil.addTargetValue(
			targetElement, infoFieldValue, targetLocale, inlineCodeWriter);
	}

	private String _getStringValue(Object value) {
		if (value == null) {
			return null;
		}

		return value.toString();
	}

	@Reference
	private TranslationInfoFieldChecker _translationInfoFieldChecker;

}