package pl.edu.icm.unity.store.migration.to4_1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.Constants;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

class JsonDumpUpdateFromV20Test
{
	private final ObjectMapper objectMapper = Constants.MAPPER;

	@Test
	void shouldRemoveLegacyCredentialFromDump() throws Exception
	{
		ObjectNode root = objectMapper.createObjectNode();
		ObjectNode contents = objectMapper.createObjectNode();
		ArrayNode authenticators = objectMapper.createArrayNode();

		ObjectNode authenticatorWrapper = objectMapper.createObjectNode();
		ObjectNode authenticator = objectMapper.createObjectNode();
		authenticator.put("verificationMethod", "local-oauth-rp");
		authenticator.put("configuration", "unity.oauth2-local-rp.credential=legacy\n"
			+ "unity.oauth2-local-rp.requiredScopes.1=sc1\n");
		authenticatorWrapper.set("obj", authenticator);
		authenticators.add(authenticatorWrapper);
		contents.set("authenticator", authenticators);
		root.set("contents", contents);

		JsonDumpUpdateFromV20 updater = new JsonDumpUpdateFromV20(objectMapper);
		InputStream updatedStream = updater.update(new ByteArrayInputStream(objectMapper.writeValueAsBytes(root)));

		ObjectNode updatedRoot = (ObjectNode) objectMapper.readTree(updatedStream);
		String configuration = updatedRoot.with("contents").withArray("authenticator").get(0).get("obj")
			.get("configuration").asText();

		Assertions.assertThat(configuration).doesNotContain("unity.oauth2-local-rp.credential")
			.contains("unity.oauth2-local-rp.requiredScopes.1=sc1");
	}
}
