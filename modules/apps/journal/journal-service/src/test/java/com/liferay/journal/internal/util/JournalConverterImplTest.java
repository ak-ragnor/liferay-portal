/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.journal.internal.util;

import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMFormFieldOptions;
import com.liferay.dynamic.data.mapping.storage.Field;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.portal.xml.SAXReaderImpl;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Jürgen Kappler
 */
public class JournalConverterImplTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testUpdateContentDynamicElement() {
		_testUpdateContentDynamicElement(StringPool.BLANK, null);

		String value = RandomTestUtil.randomString();

		_testUpdateContentDynamicElement(value, value);

		_testUpdateContentDynamicElementWithOptions();
	}

	private DDMFormField _createDDMFormField(
		String dataType, boolean localizable, String name, String type) {

		DDMFormField ddmFormField = new DDMFormField(name, type);

		ddmFormField.setDataType(dataType);
		ddmFormField.setLocalizable(localizable);

		return ddmFormField;
	}

	private void _testUpdateContentDynamicElement(
		String expectedValue, String value) {

		DDMFormField ddmFormField = _createDDMFormField(
			"string", true, "field1", "text");

		SAXReaderImpl saxReaderImpl = new SAXReaderImpl();

		Document document = saxReaderImpl.createDocument();

		Element rootElement = document.addElement("root");

		Field field = new Field(
			RandomTestUtil.randomLong(), RandomTestUtil.randomString(), value);

		ReflectionTestUtil.invoke(
			new JournalConverterImpl(), "_updateContentDynamicElement",
			new Class<?>[] {
				int.class, DDMFormField.class, Element.class, Field.class
			},
			0, ddmFormField, rootElement, field);

		Assert.assertEquals(expectedValue, rootElement.getStringValue());
	}

	private void _testUpdateContentDynamicElementWithOptions() {
		JournalConverterImpl journalConverterImpl = new JournalConverterImpl();

		ReflectionTestUtil.setFieldValue(
			journalConverterImpl, "_jsonFactory", new JSONFactoryImpl());

		SAXReaderImpl saxReaderImpl = new SAXReaderImpl();

		Document document = saxReaderImpl.createDocument();

		Element rootElement = document.addElement("root");

		boolean[] multipleFlags = {true, false};
		String[] fieldNames = {"field2", "field3"};
		List<String> optionReferences = new ArrayList<>();

		for (int i = 0; i < multipleFlags.length; i++) {
			boolean multiple = multipleFlags[i];

			String fieldName = fieldNames[i];

			DDMFormField ddmFormField = _createDDMFormField(
				"string", true, fieldName, "select");

			ddmFormField.setMultiple(multiple);

			DDMFormFieldOptions ddmFormFieldOptions = new DDMFormFieldOptions();

			String value = RandomTestUtil.randomString();

			String optionReference = RandomTestUtil.randomString();

			optionReferences.add(optionReference);

			ddmFormFieldOptions.addOption(value);
			ddmFormFieldOptions.addOptionLabel(
				value, LocaleUtil.US, RandomTestUtil.randomString());
			ddmFormFieldOptions.addOptionReference(value, optionReference);

			ddmFormField.setDDMFormFieldOptions(ddmFormFieldOptions);

			ReflectionTestUtil.invoke(
				journalConverterImpl, "_updateContentDynamicElement",
				new Class<?>[] {
					int.class, DDMFormField.class, Element.class, Field.class
				},
				0, ddmFormField, rootElement,
				new Field(
					RandomTestUtil.randomLong(), ddmFormField.getName(),
					HashMapBuilder.<Locale, List<Serializable>>put(
						LocaleUtil.US,
						() -> ListUtil.fromArray(
							JSONUtil.put(
								value
							).toString())
					).build(),
					LocaleUtil.US));
		}

		List<Element> dynamicContentElements = rootElement.elements(
			"dynamic-content");

		Assert.assertEquals(
			dynamicContentElements.toString(), 2,
			dynamicContentElements.size());

		for (int i = 0; i < dynamicContentElements.size(); i++) {
			Element dynamicContentElement = dynamicContentElements.get(i);

			List<Element> optionElements = dynamicContentElement.elements(
				"option");
			List<Element> optionReferenceElements =
				dynamicContentElement.elements("option-reference");

			Assert.assertEquals(
				optionElements.toString(), 1, optionElements.size());
			Assert.assertEquals(
				optionReferenceElements.toString(), 1,
				optionReferenceElements.size());

			Element optionReferenceElement = optionReferenceElements.get(0);

			Assert.assertEquals(
				optionReferences.get(i), optionReferenceElement.getText());
		}
	}

}