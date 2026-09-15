/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.journal.internal.upgrade.v1_1_0.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.document.library.test.util.DLAppTestUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.TestInfo;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.registry.UpgradeStepRegistrator;
import com.liferay.portal.upgrade.test.util.UpgradeTestUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Akhash Ramprakash
 */
@RunWith(Arquillian.class)
public class DocumentLibraryTypeContentUpgradeProcessTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();

		_fileEntry = DLAppTestUtil.addFileEntry(_group.getGroupId());
	}

	@Test
	@TestInfo("LPD-104003")
	public void testConvertDocumentLibraryTypeContent() throws Exception {
		_assertDocumentLibraryValue(
			_convertContent(
				"document_library",
				StringBundler.concat(
					"/c/document_library/get_file?uuid=", _fileEntry.getUuid(),
					"&groupId=", _fileEntry.getGroupId())));
	}

	@Test
	@TestInfo("LPD-104003")
	public void testConvertImageGalleryTypeContent() throws Exception {
		_assertDocumentLibraryValue(
			_convertContent(
				"image_gallery", _getImageGalleryURL(_fileEntry.getUuid())));
	}

	@Test
	@TestInfo("LPD-104003")
	public void testConvertImageGalleryTypeContentWithMissingFileEntry()
		throws Exception {

		Element dynamicElementElement = _convertContent(
			"image_gallery",
			_getImageGalleryURL(RandomTestUtil.randomString()));

		Assert.assertEquals(
			"document_library", dynamicElementElement.attributeValue("type"));

		Element dynamicContentElement = dynamicElementElement.element(
			"dynamic-content");

		Assert.assertEquals(StringPool.BLANK, dynamicContentElement.getText());
	}

	private void _assertDocumentLibraryValue(Element dynamicElementElement)
		throws Exception {

		Assert.assertEquals(
			"document_library", dynamicElementElement.attributeValue("type"));

		Element dynamicContentElement = dynamicElementElement.element(
			"dynamic-content");

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(
			dynamicContentElement.getText());

		Assert.assertEquals(
			_fileEntry.getGroupId(), jsonObject.getLong("groupId"));
		Assert.assertEquals(
			_fileEntry.getTitle(), jsonObject.getString("title"));
		Assert.assertEquals("document", jsonObject.getString("type"));
		Assert.assertEquals(_fileEntry.getUuid(), jsonObject.getString("uuid"));
	}

	private Element _convertContent(String type, String url) throws Exception {
		UpgradeProcess upgradeProcess = UpgradeTestUtil.getUpgradeStep(
			_upgradeStepRegistrator, _CLASS_NAME);

		String content = ReflectionTestUtil.invoke(
			upgradeProcess, "_convertContent", new Class<?>[] {String.class},
			StringBundler.concat(
				"<?xml version=\"1.0\"?><root available-locales=\"en_US\" ",
				"default-locale=\"en_US\"><dynamic-element ",
				"index-type=\"none\" name=\"", RandomTestUtil.randomString(),
				"\" type=\"", type,
				"\"><dynamic-content language-id=\"en_US\"><![CDATA[", url,
				"]]></dynamic-content></dynamic-element></root>"));

		Document document = SAXReaderUtil.read(content);

		Element rootElement = document.getRootElement();

		return rootElement.element("dynamic-element");
	}

	private String _getImageGalleryURL(String uuid) {
		return StringBundler.concat(
			"/image/image_gallery?uuid=", uuid, "&groupId=",
			_fileEntry.getGroupId(), "&t=", RandomTestUtil.randomLong());
	}

	private static final String _CLASS_NAME =
		"com.liferay.journal.internal.upgrade.v1_1_0." +
			"DocumentLibraryTypeContentUpgradeProcess";

	private FileEntry _fileEntry;

	@DeleteAfterTestRun
	private Group _group;

	@Inject(
		filter = "(&(component.name=com.liferay.journal.internal.upgrade.registry.JournalServiceUpgradeStepRegistrator))"
	)
	private UpgradeStepRegistrator _upgradeStepRegistrator;

}