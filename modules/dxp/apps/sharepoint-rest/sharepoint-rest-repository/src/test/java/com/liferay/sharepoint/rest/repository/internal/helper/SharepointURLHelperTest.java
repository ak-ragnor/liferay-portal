/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.sharepoint.rest.repository.internal.helper;

import com.liferay.document.library.kernel.model.DLVersionNumberIncrease;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.test.rule.LiferayUnitTestRule;
import com.liferay.sharepoint.rest.repository.internal.document.library.repository.external.model.SharepointFileEntry;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Akhash Ramprakash
 */
public class SharepointURLHelperTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetCheckInFileURL() {
		SharepointURLHelper sharepointURLHelper = new SharepointURLHelper(
			"http://sharepoint", RandomTestUtil.randomString());

		Assert.assertEquals(
			"http://sharepoint/_api/web/GetFileByServerRelativePath(" +
				"decodedUrl='/Documents/image.png')/CheckIn(comment='notes'," +
					"checkintype=2)",
			sharepointURLHelper.getCheckInFileURL(
				"/Documents/image.png", DLVersionNumberIncrease.NONE, "notes"));
	}

	@Test
	public void testGetFileVersionContentURL() {
		SharepointURLHelper sharepointURLHelper = new SharepointURLHelper(
			"http://sharepoint", RandomTestUtil.randomString());

		SharepointFileEntry sharepointFileEntry = new SharepointFileEntry(
			"/Documents/image.png", RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), RandomTestUtil.nextDate(),
			RandomTestUtil.nextDate(), RandomTestUtil.randomLong(),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			RandomTestUtil.randomLong(), sharepointURLHelper);

		Assert.assertEquals(
			"http://sharepoint/_api/web/GetFileByServerRelativePath(" +
				"decodedUrl='/Documents/image.png')/Versions(512)/$value",
			sharepointURLHelper.getFileVersionContentURL(
				sharepointFileEntry, "512"));
	}

	@Test
	public void testGetListURL() {
		SharepointURLHelper sharepointURLHelper = new SharepointURLHelper(
			"http://sharepoint/sites/site", RandomTestUtil.randomString());

		Assert.assertEquals(
			"http://sharepoint/sites/site/_api/web/GetList('/sites/site" +
				"/Shared%20Documents')?$select=EnableMinorVersions",
			sharepointURLHelper.getListURL("Shared Documents"));
	}

}