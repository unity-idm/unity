/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.userimport;

import io.imunity.rest.api.types.userimport.RestImportResult;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce.ImportResult;

public class ImportResultMapper
{
	public static RestImportResult map(ImportResult importResult)
	{
		return RestImportResult.builder()
				.withStatus(importResult.authenticationResult.getStatus()
						.name())
				.withImporterKey(importResult.importerKey)
				.withAuthenticationResult(importResult.authenticationResult.toStringFull())
				.build();
	}
}
