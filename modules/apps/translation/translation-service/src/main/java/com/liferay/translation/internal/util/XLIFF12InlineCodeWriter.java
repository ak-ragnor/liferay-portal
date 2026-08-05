/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.translation.internal.util;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Akhash Ramprakash
 */
public class XLIFF12InlineCodeWriter {

	public void write(Element element, String html) {
		if (Validator.isNull(html)) {
			element.addText(StringPool.BLANK);

			return;
		}

		Map<Integer, Integer> rids = new HashMap<>();

		for (HTMLInlineCodeToken htmlInlineCodeToken :
				HTMLInlineCodeTokenizer.tokenize(html)) {

			HTMLInlineCodeToken.Type type = htmlInlineCodeToken.getType();

			if (type == HTMLInlineCodeToken.Type.TEXT) {
				element.addText(htmlInlineCodeToken.getRawText());

				continue;
			}

			Element inlineCodeElement = null;

			if ((type == HTMLInlineCodeToken.Type.ENTITY) ||
				(type == HTMLInlineCodeToken.Type.STANDALONE)) {

				inlineCodeElement = element.addElement("ph");
			}
			else if (htmlInlineCodeToken.isIsolated()) {
				inlineCodeElement = element.addElement("it");
			}
			else if (type == HTMLInlineCodeToken.Type.OPENING_TAG) {
				inlineCodeElement = element.addElement("bpt");
			}
			else {
				inlineCodeElement = element.addElement("ept");
			}

			_id++;

			inlineCodeElement.addAttribute("id", String.valueOf(_id));

			if (htmlInlineCodeToken.isIsolated()) {
				if (type == HTMLInlineCodeToken.Type.OPENING_TAG) {
					inlineCodeElement.addAttribute("pos", "open");
				}
				else if (type == HTMLInlineCodeToken.Type.CLOSING_TAG) {
					inlineCodeElement.addAttribute("pos", "close");
				}
			}
			else if ((type == HTMLInlineCodeToken.Type.CLOSING_TAG) ||
					 (type == HTMLInlineCodeToken.Type.OPENING_TAG)) {

				Integer rid = rids.computeIfAbsent(
					htmlInlineCodeToken.getPairId(), pairId -> ++_rid);

				inlineCodeElement.addAttribute("rid", String.valueOf(rid));
			}

			inlineCodeElement.addText(htmlInlineCodeToken.getRawText());
		}
	}

	private int _id;
	private int _rid;

}