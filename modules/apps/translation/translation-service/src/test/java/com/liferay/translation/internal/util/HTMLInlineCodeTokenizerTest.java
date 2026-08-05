/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.translation.internal.util;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Akhash Ramprakash
 */
public class HTMLInlineCodeTokenizerTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testTokenizeBareAmpersandAndLessThan() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"a < b && c > d");

		Assert.assertEquals(
			htmlInlineCodeTokens.toString(), 1, htmlInlineCodeTokens.size());

		_assertToken(
			htmlInlineCodeTokens.get(0), HTMLInlineCodeToken.Type.TEXT,
			"a < b && c > d");
	}

	@Test
	public void testTokenizeCDATASection() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"a<![CDATA[literal <b> & text]]>b");

		Assert.assertEquals(
			htmlInlineCodeTokens.toString(), 3, htmlInlineCodeTokens.size());

		_assertToken(
			htmlInlineCodeTokens.get(1), HTMLInlineCodeToken.Type.STANDALONE,
			"<![CDATA[literal <b> & text]]>");
	}

	@Test
	public void testTokenizeComment() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"a<!-- comment with <b> tags -->b");

		Assert.assertEquals(
			htmlInlineCodeTokens.toString(), 3, htmlInlineCodeTokens.size());

		_assertToken(
			htmlInlineCodeTokens.get(1), HTMLInlineCodeToken.Type.STANDALONE,
			"<!-- comment with <b> tags -->");
	}

	@Test
	public void testTokenizeDoctype() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"<!DOCTYPE html><p>text</p>");

		_assertToken(
			htmlInlineCodeTokens.get(0), HTMLInlineCodeToken.Type.STANDALONE,
			"<!DOCTYPE html>");
	}

	@Test
	public void testTokenizeEntities() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"&amp; &#160; &#x27; &notterminated and &amp");

		_assertToken(
			htmlInlineCodeTokens.get(0), HTMLInlineCodeToken.Type.ENTITY,
			"&amp;");
		_assertToken(
			htmlInlineCodeTokens.get(2), HTMLInlineCodeToken.Type.ENTITY,
			"&#160;");
		_assertToken(
			htmlInlineCodeTokens.get(4), HTMLInlineCodeToken.Type.ENTITY,
			"&#x27;");
		_assertToken(
			htmlInlineCodeTokens.get(5), HTMLInlineCodeToken.Type.TEXT,
			" &notterminated and &amp");
	}

	@Test
	public void testTokenizeNestedTags() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"<p>Hello <b>world</b> &amp; more</p>");

		HTMLInlineCodeToken openingBHTMLInlineCodeToken =
			htmlInlineCodeTokens.get(2);

		HTMLInlineCodeToken openingPHTMLInlineCodeToken =
			htmlInlineCodeTokens.get(0);

		_assertToken(
			openingPHTMLInlineCodeToken, HTMLInlineCodeToken.Type.OPENING_TAG,
			"<p>");

		_assertToken(
			htmlInlineCodeTokens.get(1), HTMLInlineCodeToken.Type.TEXT,
			"Hello ");
		_assertToken(
			openingBHTMLInlineCodeToken, HTMLInlineCodeToken.Type.OPENING_TAG,
			"<b>");
		_assertToken(
			htmlInlineCodeTokens.get(3), HTMLInlineCodeToken.Type.TEXT,
			"world");

		HTMLInlineCodeToken closingBHTMLInlineCodeToken =
			htmlInlineCodeTokens.get(4);

		_assertToken(
			closingBHTMLInlineCodeToken, HTMLInlineCodeToken.Type.CLOSING_TAG,
			"</b>");

		Assert.assertEquals(
			openingBHTMLInlineCodeToken.getPairId(),
			closingBHTMLInlineCodeToken.getPairId());

		_assertToken(
			htmlInlineCodeTokens.get(6), HTMLInlineCodeToken.Type.ENTITY,
			"&amp;");

		HTMLInlineCodeToken closingPHTMLInlineCodeToken =
			htmlInlineCodeTokens.get(8);

		_assertToken(
			closingPHTMLInlineCodeToken, HTMLInlineCodeToken.Type.CLOSING_TAG,
			"</p>");

		Assert.assertEquals(
			openingPHTMLInlineCodeToken.getPairId(),
			closingPHTMLInlineCodeToken.getPairId());

		Assert.assertNotEquals(
			openingPHTMLInlineCodeToken.getPairId(),
			openingBHTMLInlineCodeToken.getPairId());
	}

	@Test
	public void testTokenizeOverlappingTags() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"<b><i></b></i>");

		HTMLInlineCodeToken openingBHTMLInlineCodeToken =
			htmlInlineCodeTokens.get(0);
		HTMLInlineCodeToken closingBHTMLInlineCodeToken =
			htmlInlineCodeTokens.get(2);

		Assert.assertTrue(openingBHTMLInlineCodeToken.isIsolated());
		Assert.assertTrue(closingBHTMLInlineCodeToken.isIsolated());

		HTMLInlineCodeToken openingIHTMLInlineCodeToken =
			htmlInlineCodeTokens.get(1);
		HTMLInlineCodeToken closingIHTMLInlineCodeToken =
			htmlInlineCodeTokens.get(3);

		Assert.assertFalse(openingIHTMLInlineCodeToken.isIsolated());
		Assert.assertEquals(
			openingIHTMLInlineCodeToken.getPairId(),
			closingIHTMLInlineCodeToken.getPairId());
	}

	@Test
	public void testTokenizeQuotedAttributes() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"<a href=\"x?a>b\" title='y>z'>link</a>");

		Assert.assertEquals(
			htmlInlineCodeTokens.toString(), 3, htmlInlineCodeTokens.size());

		_assertToken(
			htmlInlineCodeTokens.get(0), HTMLInlineCodeToken.Type.OPENING_TAG,
			"<a href=\"x?a>b\" title='y>z'>");
	}

	@Test
	public void testTokenizeScriptBlock() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"<p>before</p><script type=\"text/javascript\">if (a < b) { " +
				"alert(\"hi\"); }</script>after");

		_assertToken(
			htmlInlineCodeTokens.get(3), HTMLInlineCodeToken.Type.STANDALONE,
			"<script type=\"text/javascript\">if (a < b) { alert(\"hi\"); }" +
				"</script>");
		_assertToken(
			htmlInlineCodeTokens.get(4), HTMLInlineCodeToken.Type.TEXT,
			"after");
	}

	@Test
	public void testTokenizeSelectWithOptions() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"<select name=\"country\"><option value=\"br\">Brazil</option>" +
				"<option value=\"us\">United States</option></select>");

		HTMLInlineCodeToken openingSelectHTMLInlineCodeToken =
			htmlInlineCodeTokens.get(0);

		Assert.assertFalse(openingSelectHTMLInlineCodeToken.isIsolated());

		HTMLInlineCodeToken closingSelectHTMLInlineCodeToken =
			htmlInlineCodeTokens.get(7);

		Assert.assertEquals(
			openingSelectHTMLInlineCodeToken.getPairId(),
			closingSelectHTMLInlineCodeToken.getPairId());
	}

	@Test
	public void testTokenizeStyleBlock() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"<style>.a > .b { color: red; }</style>text");

		_assertToken(
			htmlInlineCodeTokens.get(0), HTMLInlineCodeToken.Type.STANDALONE,
			"<style>.a > .b { color: red; }</style>");
	}

	@Test
	public void testTokenizeUnterminatedTag() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"<p>unclosed <b");

		HTMLInlineCodeToken openingPHTMLInlineCodeToken =
			htmlInlineCodeTokens.get(0);

		Assert.assertTrue(openingPHTMLInlineCodeToken.isIsolated());

		_assertToken(
			htmlInlineCodeTokens.get(1), HTMLInlineCodeToken.Type.TEXT,
			"unclosed <b");
	}

	@Test
	public void testTokenizeVoidAndSelfClosingTags() {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens = _tokenize(
			"Line<br>break <img src=\"a.png\"/> done");

		_assertToken(
			htmlInlineCodeTokens.get(1), HTMLInlineCodeToken.Type.STANDALONE,
			"<br>");
		_assertToken(
			htmlInlineCodeTokens.get(3), HTMLInlineCodeToken.Type.STANDALONE,
			"<img src=\"a.png\"/>");
	}

	private void _assertToken(
		HTMLInlineCodeToken htmlInlineCodeToken, HTMLInlineCodeToken.Type type,
		String rawText) {

		Assert.assertEquals(type, htmlInlineCodeToken.getType());
		Assert.assertEquals(rawText, htmlInlineCodeToken.getRawText());
	}

	private List<HTMLInlineCodeToken> _tokenize(String html) {
		List<HTMLInlineCodeToken> htmlInlineCodeTokens =
			HTMLInlineCodeTokenizer.tokenize(html);

		StringBundler sb = new StringBundler(htmlInlineCodeTokens.size());

		for (HTMLInlineCodeToken htmlInlineCodeToken : htmlInlineCodeTokens) {
			sb.append(htmlInlineCodeToken.getRawText());
		}

		Assert.assertEquals(html, sb.toString());

		return htmlInlineCodeTokens;
	}

}