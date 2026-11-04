package io.imunity.engine.api.authn;

import java.util.Set;

/**
 * Retrieval capability describing credential presentation method independently of verification.
 * Plugins sharing a presentation contract should use the same namespaced identifier.
 */
public interface CredentialPresentationProvider
{
	Set<CredentialPresentationId> getSupportedPresentations();
}
