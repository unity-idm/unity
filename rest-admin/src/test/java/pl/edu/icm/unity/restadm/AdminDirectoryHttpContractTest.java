package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.imunity.rest.api.types.basic.RestExternalizedAttribute;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityScheduledOperation;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;

class AdminDirectoryHttpContractTest extends AdminTransportHttpTestBase
{
	@Test
	void shouldPreserveEntityNamesNullsAndMillisecondTimestamps() throws Exception
	{
		var information = new EntityInformation(42);
		information.setScheduledOperationTime(new Date(2_000_000_000_123L));
		information.setScheduledOperation(EntityScheduledOperation.REMOVE);
		when(identitiesMan.getEntity(any())).thenReturn(
				new Entity(List.of(), information, new CredentialInfo("password", Map.of())));
		String entity = fixture("entity.json");

		assertJson("/v1/entity/42", entity);
		assertJson("/v1/resolve/userName/Unicode%20%C5%BC", entity);
		assertJson("/v1/entity/42/2f-optin", "false");
	}

	@Test
	void shouldPreserveGroupMetadataAndEmptyContents() throws Exception
	{
		var contents = new GroupContents();
		contents.setGroup(new Group("/parent/żółć"));
		contents.setMembers(List.of());
		contents.setSubGroups(List.of("/parent/żółć/child"));
		when(groupsMan.getContents(anyString(), anyInt())).thenReturn(contents);
		String group = fixture("group.json");

		assertJson("/v1/group/parent%2F%C5%BC%C3%B3%C5%82%C4%87/meta", group);
		assertJson("/v1/group/parent%2F%C5%BC%C3%B3%C5%82%C4%87",
				"{\"group\":" + group + ",\"members\":[],\"subGroups\":[\"/parent/żółć/child\"]}");
		assertJson("/v1/group/parent/statements", "[]");
	}

	@Test
	void shouldPreserveExternalizedAttributePropertiesAndMapContainers() throws Exception
	{
		var attribute = RestExternalizedAttribute.builder().withName("name").withValueSyntax("string")
				.withGroupPath("/").withValues(List.of("Żółć", "snow ☃")).withDirect(true)
				.withCreationTs(new Date(123)).withUpdateTs(new Date(456)).build();
		when(attributesService.getAttributes(any(), anyString(), anyBoolean(), any(), anyBoolean()))
				.thenReturn(List.of(attribute));
		when(attributesService.getAllDirectAttributes(any())).thenReturn(Map.of("/", List.of(attribute)));
		String attributes = fixture("attributes.json");

		assertJson("/v1/entity/42/attributes", attributes);
		assertJson("/v1/entity/42/groups/direct/attributes", "{\"/\":" + attributes + "}");
		assertJson("/v1/entity/42/groups", "[]");
		assertJson("/v1/multi-group-members-attributes", "{}");
	}

	@Test
	void shouldRetainGroupRequestMediaAndLegacyParseErrorMapping() throws Exception
	{
		for (String media : new String[] {"application/json", "text/plain", null})
		{
			var response = exchange("POST", "/v1/group", "{\"path\":\"/test\"}", media);
			assertThat(response.statusCode()).as(response.body()).isEqualTo(204);
			assertThat(response.body()).isEmpty();
		}
		var invalid = exchange("POST", "/v1/group", "{", "application/json");
		assertThat(invalid.statusCode()).as(invalid.body()).isEqualTo(500);
		assertThat(invalid.body()).isEmpty();
	}

	@Test
	void shouldKeepAttributeAndCredentialBodiesWithTheirSpecificErrors() throws Exception
	{
		var attribute = exchange("PUT", "/v1/entity/42/attribute", "{", "application/json");
		assertThat(attribute.statusCode()).as(attribute.body()).isEqualTo(400);
		assertThat(json.readTree(attribute.body()).path("error").asText()).isEqualTo("JSONParsingException");
		var credentials = exchange("PUT", "/v1/entity/42/credential/password", "[]", "application/json");
		assertThat(credentials.statusCode()).as(credentials.body()).isEqualTo(400);
		assertThat(json.readTree(credentials.body()).path("error").asText()).isEqualTo("JSONParsingException");
		var updated = exchange("PUT", "/v1/entity/42/credential-adm/password", "literal secret", "application/json");
		assertThat(updated.statusCode()).as(updated.body()).isEqualTo(204);
		verify(entityCredMan).setEntityCredential(any(), anyString(), org.mockito.ArgumentMatchers.eq("literal secret"));
	}
}
