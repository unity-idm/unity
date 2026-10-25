package io.imunity.rest.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.core.util.Json;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class AdminOpenApiTest
{
	@Test
	void shouldDescribeEveryAdminRouteWithStableIdsAndExplicitResponses() throws Exception
	{
		var api = AdminOpenApi.export();
		Set<String> routes = new HashSet<>();
		Set<String> ids = new HashSet<>();
		api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
			routes.add(method.name() + " " + path);
			assertThat(ids.add(operation.getOperationId())).as(path).isTrue();
			assertThat(operation.getSummary()).isNotBlank();
			assertThat(operation.getDescription()).isNotBlank();
			assertThat(operation.getResponses()).doesNotContainKey("default").containsKeys("400", "401", "403", "500");
			assertThat(operation.getResponses().keySet()).anyMatch(status -> status.startsWith("2"));
		}));
		try (var input = getClass().getResourceAsStream("routes.txt"))
		{
			assertThat(routes).hasSize(80).containsExactlyInAnyOrderElementsOf(
					new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
		}
	}

	@Test
	void shouldDescribeFlattenedMillisecondInvitationsAndRequestStates() throws Exception
	{
		var api = Json.mapper().readTree(Json.pretty(AdminOpenApi.export()));

		assertThat(api.at("/components/schemas/ResolvedEndpoint/$ref").asText())
				.isEqualTo("#/components/schemas/RestResolvedEndpoint");
		assertThat(api.at("/components/schemas/RestResolvedEndpoint/properties/endpoint/$ref").asText())
				.isEqualTo("#/components/schemas/RestEndpoint");
		assertThat(api.at("/components/schemas/RestInvitationWithCode/anyOf").size()).isEqualTo(3);
		assertThat(api.at("/components/schemas/AdminRegistrationInvitation/allOf/1/properties/lastSentTime/type")
				.asText()).isEqualTo("integer");
		assertThat(api.at("/components/schemas/RestInvitationParam/properties/expiration/description").asText())
				.contains("milliseconds");
		assertThat(api.at("/components/schemas/RestRegistrationRequestState/properties/Timestamp/type").asText())
				.isEqualTo("integer");
		assertThat(api.at("/components/schemas/RestRegistrationRequestState/properties").has("request")).isFalse();
		assertThat(api.at("/components/schemas/RestRegistrationRequestState/required").toString())
				.doesNotContain("CreatedEntityId");
	}
}
