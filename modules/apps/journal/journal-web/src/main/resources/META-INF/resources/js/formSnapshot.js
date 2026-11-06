/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useCallback, useEffect, useRef} from 'react';

const DISPLAY_PAGE_FIELD_NAMES = ['assetDisplayPageId', 'displayPageType'];

export function getFormSnapshot(form, namespace, ignoredNames = []) {
	const excludedNames = new Set([
		`${namespace}formDate`,
		`${namespace}languageId`,
		...ignoredNames.map((name) => `${namespace}${name}`),
	]);

	const values = new Map();

	for (const element of form.elements) {
		if (
			element.name &&
			element.name.startsWith(namespace) &&
			!excludedNames.has(element.name)
		) {
			values.set(element.name, (element.value ?? '').trim());
		}
	}

	return values;
}

export function isFormSnapshotDifferent(snapshot1, snapshot2) {
	if (snapshot1.size !== snapshot2.size) {
		return true;
	}

	for (const [name, value] of snapshot1) {
		if (snapshot2.get(name) !== value) {
			return true;
		}
	}

	return false;
}

export function useFormChanges(namespace) {
	const formChangesRef = useRef(null);

	useEffect(() => {
		const form = document.getElementById(`${namespace}fm1`);

		if (!form) {
			return;
		}

		const formChanges = trackFormChanges(form, namespace);

		formChangesRef.current = formChanges;

		return () => {
			formChanges.detach();

			formChangesRef.current = null;
		};
	}, [namespace]);

	const hasFormChanges = useCallback(
		() => formChangesRef.current?.hasChanges() ?? true,
		[]
	);

	const markFormSaved = useCallback(() => {
		formChangesRef.current?.markSaved();
	}, []);

	return {hasFormChanges, markFormSaved};
}

function trackFormChanges(form, namespace) {
	const getSnapshot = () =>
		getFormSnapshot(form, namespace, DISPLAY_PAGE_FIELD_NAMES);

	let savedSnapshot = getSnapshot();

	const addRenderedFields = () => {
		const snapshot = getSnapshot();

		for (const [name, value] of snapshot) {
			if (!savedSnapshot.has(name)) {
				savedSnapshot.set(name, value);
			}
		}
	};

	const mutationObserver = new MutationObserver(addRenderedFields);

	mutationObserver.observe(form, {childList: true, subtree: true});

	return {
		detach() {
			mutationObserver.disconnect();
		},
		hasChanges() {
			if (mutationObserver.takeRecords().length) {
				addRenderedFields();
			}

			return isFormSnapshotDifferent(getSnapshot(), savedSnapshot);
		},
		markSaved() {
			savedSnapshot = getSnapshot();
		},
	};
}
