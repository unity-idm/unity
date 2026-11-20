package pl.edu.icm.unity.restadm;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.api.Test;

class AdminPolicyHttpContractTest extends AdminHttpContractTestBase
{
	private static final String PATH = "/restadm/v1/policy-documents";

	@Test
	void shouldPreservePolicyLifecycleAndRevisionFlags() throws Exception
	{
		HttpResult created = exchange("POST", PATH, fixture("policy-create.json"), ContentType.APPLICATION_JSON);
		long id = m.readTree(created.body()).path("id").asLong();
		assertJson(created, 200, "{\"id\":" + id + "}");
		String expected = fixture("policy-create.json").strip().replaceFirst("\\{", "{\"id\":" + id + ",\"revision\":1,");
		assertJson(exchange("GET", PATH + "/" + id), 200, expected);
		assertJson(exchange("GET", PATH), 200, "[" + expected + "]");
		for (boolean increment : new boolean[] {false, true})
		{
			assertEmpty(exchange("PUT", PATH + "/" + id + "?incrementRevision=" + increment,
					fixture("policy-create.json"), ContentType.TEXT_PLAIN), 204);
			assertJson(exchange("GET", PATH + "/" + id), 200,
					increment ? expected.replace("\"revision\":1", "\"revision\":2") : expected);
		}
		assertEmpty(exchange("DELETE", PATH + "/" + id), 204);
		assertThat(exchange("GET", PATH + "/" + id).status()).isEqualTo(404);
	}

	@Test
	void shouldAcceptLegacyRequestMediaTypes() throws Exception
	{
		int suffix = 0;
		for (ContentType media : new ContentType[] {ContentType.APPLICATION_JSON, ContentType.TEXT_PLAIN, null})
		{
			HttpResult result = exchange("POST", PATH,
					fixture("policy-create.json").replace("terms", "terms" + suffix++), media);
			assertThat(result.status()).as(result.body()).isEqualTo(200);
			assertThat(result.contentType()).startsWith("application/json");
		}
	}

	@Test
	void shouldPreserveDeserializationErrorsAndRequiredFlagPrecedence() throws Exception
	{
		for (String invalid : new String[] {"{", "", "{\"unknown\":true}", "{\"mandatory\":{}}"})
			assertJson(exchange("POST", PATH, invalid, ContentType.APPLICATION_JSON), 400,
					"""
					{"message":"Can't perform JSON deserialization","error":"BadRequestException"}
					""");
		assertJson(exchange("PUT", PATH + "/123", "{", ContentType.APPLICATION_JSON), 400, """
				{"message":"Query param incrementRevision is mandatory","error":"BadRequestException"}
				""");
		assertJson(exchange("POST", PATH, "{}", ContentType.APPLICATION_JSON), 400,
				"{\"message\":\"Name is required\",\"error\":\"BadRequestException\"}");
		assertEmpty(exchange("POST", PATH, "null", ContentType.APPLICATION_JSON), 500);
		assertJson(exchange("GET", PATH), 200, "[]");
	}

	@Test
	void shouldReportEachMissingPolicyFieldInLegacyOrder() throws Exception
	{
		for (String field : new String[] {"name", "displayedName", "mandatory", "contentType", "content"})
		{
			var document = (com.fasterxml.jackson.databind.node.ObjectNode) m.readTree(fixture("policy-create.json"));
			document.remove(field);
			String label = Character.toUpperCase(field.charAt(0)) + field.substring(1);
			assertJson(exchange("POST", PATH, document.toString(), ContentType.APPLICATION_JSON), 400,
					"{\"message\":\"" + label + " is required\",\"error\":\"BadRequestException\"}");
		}
	}
}
