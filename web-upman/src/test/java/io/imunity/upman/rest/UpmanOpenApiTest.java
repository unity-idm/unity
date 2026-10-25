package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;

class UpmanOpenApiTest
{
	@Test
	void shouldDescribeEveryContractRouteWithUniqueOperationIdsAndExplicitSuccess() throws Exception
	{
		OpenAPI api = UpmanOpenApi.export();
		Set<String> routes = new HashSet<>();
		Set<String> ids = new HashSet<>();
		api.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
			routes.add(method + " " + path + " " + operation.getOperationId());
			assertThat(ids.add(operation.getOperationId())).as(operation.getOperationId()).isTrue();
			assertThat(operation.getResponses()).doesNotContainKey("default");
			assertThat(operation.getResponses().keySet()).anyMatch(code -> code.matches("2[0-9]{2}"));
			if (operation.getRequestBody() != null)
				operation.getRequestBody().getContent().values().forEach(content ->
						assertThat(content.getSchema().getType()).isNotEqualTo("string"));
			operation.getResponses().forEach((status, response) -> {
				if (status.equals("204"))
					assertThat(response.getContent()).isNull();
				else if (status.startsWith("2"))
					response.getContent().values().forEach(content ->
							assertThat(content.getSchema().getType()).isNotEqualTo("string"));
			});
		}));
		try (var input = getClass().getResourceAsStream("contract/routes.txt"))
		{
			assertThat(routes).containsExactlyInAnyOrderElementsOf(
					new String(input.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
		}
	}

	@Test
	void shouldLeaveGlobalModelConvertersUnchanged()
	{
		var before = List.copyOf(ModelConverters.getInstance().getConverters());

		UpmanOpenApi.export();

		assertThat(ModelConverters.getInstance().getConverters()).containsExactlyElementsOf(before);
	}
}
