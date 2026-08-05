/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.translation.internal.util;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Element;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Akhash Ramprakash
 */
public class XLIFF20InlineCodeWriter {

	public XLIFF20InlineCodeWriter(Element unitElement) {
		_unitElement = unitElement;

		_originalDataElement = unitElement.addElement("originalData");
	}

	public void finish() {
		if (_dataIds.isEmpty()) {
			_unitElement.remove(_originalDataElement);
		}
	}

	public void write(Element element, String html) {
		if (Validator.isNull(html)) {
			element.addText(StringPool.BLANK);

			return;
		}

		List<HTMLInlineCodeToken> htmlInlineCodeTokens =
			HTMLInlineCodeTokenizer.tokenize(html);

		Map<Integer, String> closingRawTexts = new HashMap<>();

		for (HTMLInlineCodeToken htmlInlineCodeToken : htmlInlineCodeTokens) {
			if ((htmlInlineCodeToken.getType() ==
					HTMLInlineCodeToken.Type.CLOSING_TAG) &&
				!htmlInlineCodeToken.isIsolated()) {

				closingRawTexts.put(
					htmlInlineCodeToken.getPairId(),
					htmlInlineCodeToken.getRawText());
			}
		}

		Deque<Element> elements = new LinkedList<>();

		elements.push(element);

		for (HTMLInlineCodeToken htmlInlineCodeToken : htmlInlineCodeTokens) {
			Element currentElement = elements.peek();

			HTMLInlineCodeToken.Type type = htmlInlineCodeToken.getType();

			if (type == HTMLInlineCodeToken.Type.TEXT) {
				currentElement.addText(htmlInlineCodeToken.getRawText());

				continue;
			}

			if ((type == HTMLInlineCodeToken.Type.CLOSING_TAG) &&
				!htmlInlineCodeToken.isIsolated()) {

				elements.pop();

				continue;
			}

			if ((type == HTMLInlineCodeToken.Type.OPENING_TAG) &&
				!htmlInlineCodeToken.isIsolated()) {

				Element pcElement = currentElement.addElement("pc");

				pcElement.addAttribute(
					"dataRefEnd",
					_getDataId(
						closingRawTexts.get(htmlInlineCodeToken.getPairId())));
				pcElement.addAttribute(
					"dataRefStart",
					_getDataId(htmlInlineCodeToken.getRawText()));

				_id++;

				pcElement.addAttribute("id", String.valueOf(_id));

				elements.push(pcElement);

				continue;
			}

			Element phElement = currentElement.addElement("ph");

			phElement.addAttribute(
				"dataRef", _getDataId(htmlInlineCodeToken.getRawText()));

			_id++;

			phElement.addAttribute("id", String.valueOf(_id));
		}
	}

	private String _getDataId(String rawText) {
		String dataId = _dataIds.get(rawText);

		if (dataId != null) {
			return dataId;
		}

		dataId = "d" + (_dataIds.size() + 1);

		_dataIds.put(rawText, dataId);

		Element dataElement = _originalDataElement.addElement("data");

		dataElement.addAttribute("id", dataId);
		dataElement.addText(rawText);

		return dataId;
	}

	private final Map<String, String> _dataIds = new HashMap<>();
	private int _id;
	private final Element _originalDataElement;
	private final Element _unitElement;

}