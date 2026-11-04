package io.imunity.engine.api.authn;

import java.util.Objects;

/**
 * Stable, plugin-defined identifier of a credential presentation contract.
 */
public record CredentialPresentationId(String value)
{
	public CredentialPresentationId
	{
		Objects.requireNonNull(value);
	}
}
