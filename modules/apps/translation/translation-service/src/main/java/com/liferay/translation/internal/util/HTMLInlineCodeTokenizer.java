/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.translation.internal.util;

import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Akhash Ramprakash
 */
public class HTMLInlineCodeTokenizer {

	public static List<HTMLInlineCodeToken> tokenize(String html) {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _scan(html);

		_pair(htmlInlineCodeTokens);

		return htmlInlineCodeTokens;
	}

	private static void _addTextToken(
		String html, List<HTMLInlineCodeToken> htmlInlineCodeTokens, int start,
		int end) {

		if (start >= end) {
			return;
		}

		htmlInlineCodeTokens.add(
			new HTMLInlineCodeToken(
				html.substring(start, end), null,
				HTMLInlineCodeToken.Type.TEXT));
	}

	private static boolean _isHexDigit(char c) {
		if (((c >= '0') && (c <= '9')) || ((c >= 'A') && (c <= 'F')) ||
			((c >= 'a') && (c <= 'f'))) {

			return true;
		}

		return false;
	}

	private static void _pair(List<HTMLInlineCodeToken> htmlInlineCodeTokens) {
		Deque<HTMLInlineCodeToken> openingHTMLInlineCodeTokens =
			new LinkedList<>();

		int pairId = 0;

		for (HTMLInlineCodeToken htmlInlineCodeToken : htmlInlineCodeTokens) {
			if (htmlInlineCodeToken.getType() ==
					HTMLInlineCodeToken.Type.OPENING_TAG) {

				openingHTMLInlineCodeTokens.push(htmlInlineCodeToken);

				continue;
			}

			if (htmlInlineCodeToken.getType() !=
					HTMLInlineCodeToken.Type.CLOSING_TAG) {

				continue;
			}

			HTMLInlineCodeToken openingHTMLInlineCodeToken =
				openingHTMLInlineCodeTokens.peek();

			if ((openingHTMLInlineCodeToken != null) &&
				Objects.equals(
					openingHTMLInlineCodeToken.getTagName(),
					htmlInlineCodeToken.getTagName())) {

				openingHTMLInlineCodeTokens.pop();

				pairId++;

				openingHTMLInlineCodeToken.setPairId(pairId);
				htmlInlineCodeToken.setPairId(pairId);
			}
			else {
				htmlInlineCodeToken.setIsolated(true);
			}
		}

		for (HTMLInlineCodeToken openingHTMLInlineCodeToken :
				openingHTMLInlineCodeTokens) {

			openingHTMLInlineCodeToken.setIsolated(true);
		}
	}

	private static List<HTMLInlineCodeToken> _scan(String html) {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = new ArrayList<>();

		int index = 0;
		int textStart = 0;

		while (index < html.length()) {
			char c = html.charAt(index);

			if ((c != CharPool.AMPERSAND) && (c != CharPool.LESS_THAN)) {
				index++;

				continue;
			}

			HTMLInlineCodeToken htmlInlineCodeToken = null;

			if (c == CharPool.AMPERSAND) {
				htmlInlineCodeToken = _scanEntity(html, index);
			}
			else {
				htmlInlineCodeToken = _scanMarkup(html, index);
			}

			if (htmlInlineCodeToken == null) {
				index++;

				continue;
			}

			_addTextToken(html, htmlInlineCodeTokens, textStart, index);

			htmlInlineCodeTokens.add(htmlInlineCodeToken);

			String rawText = htmlInlineCodeToken.getRawText();

			index += rawText.length();

			textStart = index;
		}

		_addTextToken(html, htmlInlineCodeTokens, textStart, html.length());

		return htmlInlineCodeTokens;
	}

	private static HTMLInlineCodeToken _scanClosingTag(String html, int index) {
		String tagName = _scanTagName(html, index + 2);

		if (tagName == null) {
			return null;
		}

		int end = index + 2 + tagName.length();

		while ((end < html.length()) &&
			   Character.isWhitespace(html.charAt(end))) {

			end++;
		}

		if ((end >= html.length()) ||
			(html.charAt(end) != CharPool.GREATER_THAN)) {

			return null;
		}

		return new HTMLInlineCodeToken(
			html.substring(index, end + 1), StringUtil.toLowerCase(tagName),
			HTMLInlineCodeToken.Type.CLOSING_TAG);
	}

	private static HTMLInlineCodeToken _scanEntity(String html, int index) {
		int end = index + 1;

		if (end >= html.length()) {
			return null;
		}

		char c = html.charAt(end);

		if (c == CharPool.POUND) {
			end++;

			if ((end < html.length()) &&
				((html.charAt(end) == 'X') || (html.charAt(end) == 'x'))) {

				end++;

				int digitsStart = end;

				while ((end < html.length()) && _isHexDigit(html.charAt(end))) {
					end++;
				}

				if (end == digitsStart) {
					return null;
				}
			}
			else {
				int digitsStart = end;

				while ((end < html.length()) && (html.charAt(end) >= '0') &&
					   (html.charAt(end) <= '9')) {

					end++;
				}

				if (end == digitsStart) {
					return null;
				}
			}
		}
		else if (Character.isLetter(c)) {
			end++;

			while ((end < html.length()) &&
				   Character.isLetterOrDigit(html.charAt(end))) {

				end++;
			}
		}
		else {
			return null;
		}

		if ((end >= html.length()) ||
			(html.charAt(end) != CharPool.SEMICOLON)) {

			return null;
		}

		return new HTMLInlineCodeToken(
			html.substring(index, end + 1), null,
			HTMLInlineCodeToken.Type.ENTITY);
	}

	private static HTMLInlineCodeToken _scanMarkup(String html, int index) {
		if (html.startsWith("<!--", index)) {
			return _scanStandalone(html, index, "-->");
		}

		if (html.startsWith("<![CDATA[", index)) {
			return _scanStandalone(html, index, "]]>");
		}

		if (html.startsWith("<!", index)) {
			return _scanStandalone(html, index, ">");
		}

		if (html.startsWith("<?", index)) {
			return _scanStandalone(html, index, "?>");
		}

		if (html.startsWith("</", index)) {
			return _scanClosingTag(html, index);
		}

		return _scanTag(html, index);
	}

	private static HTMLInlineCodeToken _scanRawTextElement(
		String html, int index, int contentStart, String tagName) {

		String lowerCaseHTML = StringUtil.toLowerCase(html);

		int closingTagIndex = lowerCaseHTML.indexOf(
			"</" + tagName, contentStart);

		if (closingTagIndex == -1) {
			return null;
		}

		int end = html.indexOf(CharPool.GREATER_THAN, closingTagIndex);

		if (end == -1) {
			return null;
		}

		return new HTMLInlineCodeToken(
			html.substring(index, end + 1), tagName,
			HTMLInlineCodeToken.Type.STANDALONE);
	}

	private static HTMLInlineCodeToken _scanStandalone(
		String html, int index, String endString) {

		int end = html.indexOf(endString, index);

		if (end == -1) {
			return null;
		}

		return new HTMLInlineCodeToken(
			html.substring(index, end + endString.length()), null,
			HTMLInlineCodeToken.Type.STANDALONE);
	}

	private static HTMLInlineCodeToken _scanTag(String html, int index) {
		String tagName = _scanTagName(html, index + 1);

		if (tagName == null) {
			return null;
		}

		int end = _scanTagEnd(html, index + 1 + tagName.length());

		if (end == -1) {
			return null;
		}

		String lowerCaseTagName = StringUtil.toLowerCase(tagName);

		if (lowerCaseTagName.equals("script") ||
			lowerCaseTagName.equals("style")) {

			HTMLInlineCodeToken htmlInlineCodeToken = _scanRawTextElement(
				html, index, end, lowerCaseTagName);

			if (htmlInlineCodeToken != null) {
				return htmlInlineCodeToken;
			}

			if (html.charAt(end - 2) != CharPool.FORWARD_SLASH) {
				return null;
			}
		}

		String rawText = html.substring(index, end);

		if ((html.charAt(end - 2) == CharPool.FORWARD_SLASH) ||
			_voidElementNames.contains(lowerCaseTagName)) {

			return new HTMLInlineCodeToken(
				rawText, lowerCaseTagName, HTMLInlineCodeToken.Type.STANDALONE);
		}

		return new HTMLInlineCodeToken(
			rawText, lowerCaseTagName, HTMLInlineCodeToken.Type.OPENING_TAG);
	}

	private static int _scanTagEnd(String html, int index) {
		while (index < html.length()) {
			char c = html.charAt(index);

			if (c == CharPool.GREATER_THAN) {
				return index + 1;
			}

			if ((c == CharPool.APOSTROPHE) || (c == CharPool.QUOTE)) {
				int quoteEnd = html.indexOf(c, index + 1);

				if (quoteEnd == -1) {
					return -1;
				}

				index = quoteEnd + 1;

				continue;
			}

			index++;
		}

		return -1;
	}

	private static String _scanTagName(String html, int index) {
		if ((index >= html.length()) ||
			!Character.isLetter(html.charAt(index))) {

			return null;
		}

		int end = index + 1;

		while (end < html.length()) {
			char c = html.charAt(end);

			if (Character.isLetterOrDigit(c) || (c == CharPool.COLON) ||
				(c == CharPool.DASH)) {

				end++;
			}
			else {
				break;
			}
		}

		return html.substring(index, end);
	}

	private static final Set<String> _voidElementNames = SetUtil.fromArray(
		"area", "base", "br", "col", "embed", "hr", "img", "input", "link",
		"meta", "param", "source", "track", "wbr");

}