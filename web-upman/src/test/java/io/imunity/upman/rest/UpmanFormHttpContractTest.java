package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.apache.hc.core5.http.ContentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;

class UpmanFormHttpContractTest extends UpmanHttpContractTestBase
{
	private static final String PROJECTS = "/restupm/v1/projects";
	private static final String PROJECT = PROJECTS + "/contract";
	@Autowired
	private PublicRegistrationURLSupport publicUrls;

	@ParameterizedTest(name = "{0}")
	@MethodSource("forms")
	void shouldPreserveExplicitFormTreesAcrossCrud(FormCase form) throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		String path = PROJECT + "/" + form.path();
		String create = fixture(form.fixture());
		ObjectNode expected = (ObjectNode) m.readTree(create);
		if (form.path().equals("registrationForm"))
		{
			ObjectNode information = expected.putObject("FormInformation2ndStage");
			information.putNull("DefaultValue");
			information.putObject("Map");
		}

		assertEmpty(exchange("POST", path, create, ContentType.APPLICATION_JSON), 204);
		assertJson(exchange("GET", path), 200, expected);
		String link = form.path().equals("registrationForm")
				? publicUrls.getPublicRegistrationLink(registrationsMan.getForm("contract-form"))
				: publicUrls.getWellknownEnquiryLink("contract-form");
		assertJson(exchange("GET", path + "/link"), 200, m.createObjectNode().put("link", link));
		ObjectNode updated = expected.deepCopy();
		updated.put("Description", "Updated form ☃");
		for (String query : new String[] {"", "?ignoreRequests=false", "?ignoreRequests=true"})
		{
			assertEmpty(exchange("PUT", path + query, updated.toString(), ContentType.APPLICATION_JSON), 204);
			assertJson(exchange("GET", path), 200, updated);
		}
		assertEmpty(exchange("DELETE", path + "?dropRequests=false"), 204);
		assertThat(exchange("GET", path).status()).isEqualTo(404);
		assertEmpty(exchange("POST", path + "?autogenerate=false", create, ContentType.APPLICATION_JSON), 204);
		assertEmpty(exchange("DELETE", path + "?dropRequests=true"), 204);
		assertThat(exchange("GET", path).status()).isEqualTo(404);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("forms")
	void shouldIgnoreAbsentEmptyAndMalformedBodiesWhenGeneratingForms(FormCase form) throws Exception
	{
		exchange("POST", PROJECTS, fixture("project-create.json"), ContentType.APPLICATION_JSON);
		String path = PROJECT + "/" + form.path();

		assertEmpty(exchange("POST", path + "?autogenerate=true"), 415);
		for (String ignored : new String[] {null, "", "{malformed", "null"})
		{
			assertEmpty(exchange("POST", path + "?autogenerate=true", ignored, ContentType.APPLICATION_JSON), 204);
			HttpResult generated = exchange("GET", path);
			assertThat(generated.status()).as(generated.body()).isEqualTo(200);
			assertThat(generated.contentType()).startsWith("application/json");
			assertThat(m.readTree(generated.body()).path("Name").asText()).isNotBlank();
			assertEmpty(exchange("DELETE", path), 204);
		}
	}

	private static Stream<FormCase> forms()
	{
		return Stream.of(
				FormCase.builder().path("registrationForm").fixture("registration-form-create.json").build(),
				FormCase.builder().path("signUpEnquiry").fixture("enquiry-form-create.json").build(),
				FormCase.builder().path("membershipUpdateEnquiry").fixture("membership-form-create.json").build());
	}

	private record FormCase(String path, String fixture)
	{
		static Builder builder()
		{
			return new Builder();
		}

		@Override
		public String toString()
		{
			return path;
		}

		private static class Builder
		{
			private String path;
			private String fixture;

			Builder path(String path)
			{
				this.path = path;
				return this;
			}

			Builder fixture(String fixture)
			{
				this.fixture = fixture;
				return this;
			}

			FormCase build()
			{
				return new FormCase(path, fixture);
			}
		}
	}
}
