/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.userimport;

import java.util.function.Function;

import io.imunity.rest.api.types.userimport.RestImportResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce.ImportResult;
import pl.edu.icm.unity.rest.mappers.OneWayMapperTestBase;

public class ImportResultMapperTest extends OneWayMapperTestBase<ImportResult, RestImportResult>
{

	private AuthenticationResult authenticationResult = RemoteAuthenticationResult.successful(
			new RemotelyAuthenticatedPrincipal("idp", "profile"), new AuthenticatedEntity(1L, "sub", "cred"));

	@Override
	protected ImportResult getAPIObject()
	{

		return new ImportResult("key", authenticationResult);
	}

	@Override
	protected RestImportResult getRestObject()
	{
		return RestImportResult.builder()
				.withAuthenticationResult(authenticationResult.toStringFull())
				.withStatus("success")
				.withImporterKey("key")
				.build();
	}

	@Override
	protected Function<ImportResult, RestImportResult> getMapper()
	{
		return ImportResultMapper::map;
	}

}
