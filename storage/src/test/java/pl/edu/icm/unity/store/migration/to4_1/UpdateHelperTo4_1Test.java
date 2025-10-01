package pl.edu.icm.unity.store.migration.to4_1;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import pl.edu.icm.unity.base.Constants;

import java.util.Optional;

class UpdateHelperTo4_1Test
{
	@Test
	void shouldRemoveLegacyCredentialPropertyWhenPresent()
	{
		ObjectNode authenticator = Constants.MAPPER.createObjectNode();
		authenticator.put("verificationMethod", "local-oauth-rp");
		authenticator.put("configuration", "unity.oauth2-local-rp.credential=legacy\n" +
				"unity.oauth2-local-rp.requiredScopes.1=sc1\n");

		Optional<ObjectNode> updated = UpdateHelperTo4_1.removeLocalOAuthCredential(authenticator);

		Assertions.assertThat(updated).isPresent();
		Assertions.assertThat(updated.get().get("configuration").asText())
				.doesNotContain("unity.oauth2-local-rp.credential")
				.contains("unity.oauth2-local-rp.requiredScopes.1=sc1");
	}

	@Test
	void shouldLeaveAuthenticatorUntouchedWhenPropertyMissing()
	{
		ObjectNode authenticator = Constants.MAPPER.createObjectNode();
		authenticator.put("verificationMethod", "local-oauth-rp");
		authenticator.put("configuration", "unity.oauth2-local-rp.requiredScopes.1=sc1\n");

		Optional<ObjectNode> updated = UpdateHelperTo4_1.removeLocalOAuthCredential(authenticator);

		Assertions.assertThat(updated).isEmpty();
	}

	@Test
	void shouldLeaveAuthenticatorUntouchedWhenDifferentVerifier()
	{
		ObjectNode authenticator = Constants.MAPPER.createObjectNode();
		authenticator.put("verificationMethod", "oauth-rp");
		authenticator.put("configuration", "unity.oauth2-local-rp.credential=legacy\n");

		Optional<ObjectNode> updated = UpdateHelperTo4_1.removeLocalOAuthCredential(authenticator);

		Assertions.assertThat(updated).isEmpty();
	}
}
