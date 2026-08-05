/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.translation.internal.util;

/**
 * @author Akhash Ramprakash
 */
public class HTMLInlineCodeToken {

	public HTMLInlineCodeToken(String rawText, String tagName, Type type) {
		_rawText = rawText;
		_tagName = tagName;
		_type = type;
	}

	public int getPairId() {
		return _pairId;
	}

	public String getRawText() {
		return _rawText;
	}

	public String getTagName() {
		return _tagName;
	}

	public Type getType() {
		return _type;
	}

	public boolean isIsolated() {
		return _isolated;
	}

	public void setIsolated(boolean isolated) {
		_isolated = isolated;
	}

	public void setPairId(int pairId) {
		_pairId = pairId;
	}

	public enum Type {

		CLOSING_TAG, ENTITY, OPENING_TAG, STANDALONE, TEXT

	}

	private boolean _isolated;
	private int _pairId;
	private final String _rawText;
	private final String _tagName;
	private final Type _type;

}