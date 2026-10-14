/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {openToast} from 'frontend-js-components-web';
import {fetch, sessionStorage} from 'frontend-js-web';
import {useEffect} from 'react';

interface BulkStatusProps {
	bulkInProgress: boolean;
	deletionStarted: boolean;
	pathModule: string;
}

export default function BulkStatus({
	bulkInProgress,
	deletionStarted,
	pathModule,
}: BulkStatusProps) {
	useEffect(() => {
		if (
			sessionStorage.getItem(
				'deletionCompleted',
				sessionStorage.TYPES.NECESSARY
			)
		) {
			sessionStorage.removeItem('deletionCompleted');

			openToast({
				message: Liferay.Language.get(
					'deletion-completed-successfully'
				),
			});
		}

		if (deletionStarted) {
			openToast({
				autoClose: false,
				message: Liferay.Language.get(
					'deletion-started-for-all-entries'
				),
				type: 'info',
			});
		}

		if (!bulkInProgress && !deletionStarted) {
			return;
		}

		const intervalId = setInterval(() => {
			fetch(`${pathModule}/bulk/v1.0/status`)
				.then((response) => response.json())
				.then(({actionInProgress}) => {
					if (actionInProgress) {
						return;
					}

					clearInterval(intervalId);

					sessionStorage.setItem(
						'deletionCompleted',
						'true',
						sessionStorage.TYPES.NECESSARY
					);

					window.location.reload();
				})
				.catch(() => {
					clearInterval(intervalId);

					openToast({
						message: Liferay.Language.get(
							'an-unexpected-error-occurred'
						),
						type: 'danger',
					});
				});
		}, 1000);

		return () => clearInterval(intervalId);
	}, [bulkInProgress, deletionStarted, pathModule]);

	return null;
}
