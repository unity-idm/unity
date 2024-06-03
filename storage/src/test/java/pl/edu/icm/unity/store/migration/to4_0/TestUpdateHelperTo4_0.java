/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.migration.to4_0;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TestUpdateHelperTo4_0
{
	@Test
	void shouldNotEditNotHomeUIEndpoint()
	{
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();
		root.set("typeId", new TextNode("Console"));

		Optional<TextNode> configuration = UpdateHelperTo4_0.updateHomeUIConfiguration(root);
		assertThat(configuration).isEmpty();
	}

	@Test
	void shouldAddTrustedDevicesTabToDisabledPropertiesWhenCredentialTabIsSet()
	{
		String properties = """
		unity.userhome.disabledComponents.1=credentialTab
		unity.userhome.disabledComponents.2=userDetailsTab
		unity.userhome.disabledComponents.3=preferencesTab
		unity.userhome.disabledComponents.4=accountRemoval
		""".replaceAll("\n", System.lineSeparator());
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();
		ObjectNode configurationNode = mapper.createObjectNode();
		configurationNode.set("configuration", new TextNode(properties));
		root.set("name", new TextNode("Home"));
		root.set("typeId", new TextNode("UserHomeUI"));
		root.set("configuration", configurationNode);

		Optional<TextNode> configuration = UpdateHelperTo4_0.updateHomeUIConfiguration(root);
		assertThat(configuration).isPresent();
		assertThat(configuration.get().asText()).contains("unity.userhome.disabledComponents.1=credentialTab");
		assertThat(configuration.get().asText()).contains("unity.userhome.disabledComponents.2=userDetailsTab");
		assertThat(configuration.get().asText()).contains("unity.userhome.disabledComponents.3=preferencesTab");
		assertThat(configuration.get().asText()).contains("unity.userhome.disabledComponents.4=accountRemoval");
		assertThat(configuration.get().asText()).contains("unity.userhome.disabledComponents.10=trustedDevices");
	}

	@Test
	void shouldNotAddTrustedDevicesTabToDisabledPropertiesWhenCredentialTabIsComment()
	{
		String properties = """
		#unity.userhome.disabledComponents.1=credentialTab
		unity.userhome.disabledComponents.2=userDetailsTab
		unity.userhome.disabledComponents.3=preferencesTab
		unity.userhome.disabledComponents.4=accountRemoval
		""".replaceAll("\n", System.lineSeparator());
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();
		ObjectNode configurationNode = mapper.createObjectNode();
		configurationNode.set("configuration", new TextNode(properties));
		root.set("name", new TextNode("Home"));
		root.set("typeId", new TextNode("UserHomeUI"));
		root.set("configuration", configurationNode);

		Optional<TextNode> configuration = UpdateHelperTo4_0.updateHomeUIConfiguration(root);
		assertThat(configuration).isEmpty();
	}

	@Test
	void shouldNotAddTrustedDevicesTabToDisabledPropertiesWhenCredentialTabDoesNotExist()
	{
		String properties = """
		unity.userhome.disabledComponents.2=userDetailsTab
		unity.userhome.disabledComponents.3=preferencesTab
		unity.userhome.disabledComponents.4=accountRemoval
		""".replaceAll("\n", System.lineSeparator());
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();
		ObjectNode configurationNode = mapper.createObjectNode();
		configurationNode.set("configuration", new TextNode(properties));
		root.set("name", new TextNode("Home"));
		root.set("typeId", new TextNode("UserHomeUI"));
		root.set("configuration", configurationNode);

		Optional<TextNode> configuration = UpdateHelperTo4_0.updateHomeUIConfiguration(root);
		assertThat(configuration).isEmpty();
	}

	@Test
	void shouldRemoveUserInfo()
	{
		String properties = """
		unity.userhome.disabledComponents.2=userDetailsTab
		unity.userhome.disabledComponents.3=preferencesTab
		unity.userhome.disabledComponents.4=accountRemoval
		unity.userhome.disabledComponents.5=userInfo
		""".replaceAll("\n", System.lineSeparator());
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();
		ObjectNode configurationNode = mapper.createObjectNode();
		configurationNode.set("configuration", new TextNode(properties));
		root.set("name", new TextNode("Home"));
		root.set("typeId", new TextNode("UserHomeUI"));
		root.set("configuration", configurationNode);

		Optional<TextNode> configuration = UpdateHelperTo4_0.updateHomeUIConfiguration(root);
		assertThat(configuration).isPresent();
		assertThat(configuration.get().asText()).contains("unity.userhome.disabledComponents.2=userDetailsTab");
		assertThat(configuration.get().asText()).contains("unity.userhome.disabledComponents.3=preferencesTab");
		assertThat(configuration.get().asText()).contains("unity.userhome.disabledComponents.4=accountRemoval");
		assertThat(configuration.get().asText()).doesNotContain("unity.userhome.disabledComponents.5=userInfo");
	}

	@Test
	void shouldRemoveIdentitiesManagement()
	{
		String properties = """
		unity.userhome.disabledComponents.2=userDetailsTab
		unity.userhome.disabledComponents.3=preferencesTab
		unity.userhome.disabledComponents.4=accountRemoval
		unity.userhome.disabledComponents.5=identitiesManagement
		""".replaceAll("\n", System.lineSeparator());
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();
		ObjectNode configurationNode = mapper.createObjectNode();
		configurationNode.set("configuration", new TextNode(properties));
		root.set("name", new TextNode("Home"));
		root.set("typeId", new TextNode("UserHomeUI"));
		root.set("configuration", configurationNode);

		Optional<TextNode> configuration = UpdateHelperTo4_0.updateHomeUIConfiguration(root);
		assertThat(configuration).isPresent();
		assertThat(configuration.get().asText()).contains("unity.userhome.disabledComponents.2=userDetailsTab");
		assertThat(configuration.get().asText()).contains("unity.userhome.disabledComponents.3=preferencesTab");
		assertThat(configuration.get().asText()).contains("unity.userhome.disabledComponents.4=accountRemoval");
		assertThat(configuration.get().asText()).doesNotContain("unity.userhome.disabledComponents.5=identitiesManagement");
	}
	
	@Test
	void shouldUpdateOAuthAuthenticatorDefaultIcons()
	{
		String properties = """
				unity.oauth2.client.providers.local.iconUrl=file:../common/img/other/logo-hand.png
				""".replaceAll("\n", System.lineSeparator());
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = mapper.createObjectNode();

		root.set("name", new TextNode("OAuth"));
		root.set("verificationMethod", new TextNode("oauth2"));
		root.set("configuration", new TextNode(properties));

		ObjectNode auth = UpdateHelperTo4_0.updateOAuthAuthenticatorIcons(root).get();
		assertThat(auth.get("configuration")
				.asText()).contains("unity.oauth2.client.providers.local.iconUrl=assets/img/other/logo-square.png");

	}

}