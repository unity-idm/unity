package io.imunity.test.ui.flows;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.oauth.as.token.access.DeviceCodeRepository;

@Tag("oauth")
class DeviceAuthorizationTest extends OAuthWorkflowTestBase
{
	@Autowired
	private TokensManagement tokens;

	@Test
	void shouldAuthorizeManualCodeAndReleaseOnlyRequestedUserData() throws Exception
	{
		TestUser client = oauthClient();
		TestUser user = createUser("Device authorization owner");
		deployDeviceFlow();
		LoopbackHttpClient http = new LoopbackHttpClient();
		JsonNode device = requestDevice(http, client);
		assertThat(poll(http, client, device).body().path("error").asText()).isEqualTo("authorization_pending");
		VaadinPage page = signIn(device.path("verification_uri").asText(), user);
		page.fill(testId("DeviceSignInView.code"), device.path("user_code").asText());
		page.click(testId("DeviceSignInView.submitCode"));
		page.visible(By.className("u-consentConfirmButton"));
		page.click(By.id("ExposedAttributes.showDetails"));
		assertThat(page.visible(By.className("u-consentMainColumn")).getText())
				.contains("Local Selenium client");
		assertThat(page.value(By.xpath("//vaadin-text-field[label[normalize-space(.)='Name']]")))
				.isEqualTo(user.displayName());
		screenshot("Device consent and released attributes");
		page.click(By.className("u-consentConfirmButton"));
		page.visible(By.className("u-final-info"));
		clearPollDelay(device);
		LoopbackHttpClient.Result authorized = poll(http, client, device);
		assertThat(authorized.status()).isEqualTo(200);
		String accessToken = authorized.body().path("access_token").asText();
		assertThat(accessToken).isNotBlank();
		LoopbackHttpClient.Result userInfo = http.get(baseUrl + "/oauth/userinfo",
				Map.of("Authorization", "Bearer " + accessToken));
		assertThat(userInfo.status()).isEqualTo(200);
		assertThat(userInfo.body().path("sub").asText()).isEqualTo(user.username());
		assertThat(userInfo.body().path("name").asText()).isEqualTo(user.displayName());
		assertThat(userInfo.body().has("sys:AuthorizationRole")).isFalse();
		assertThat(poll(http, client, device).body().has("access_token")).isFalse();
	}

	@Test
	void shouldConfirmPrefilledCodeAndReturnAccessDeniedAfterRefusal() throws Exception
	{
		TestUser client = oauthClient();
		TestUser user = createUser("Device denial owner");
		deployDeviceFlow();
		LoopbackHttpClient http = new LoopbackHttpClient();
		JsonNode device = requestDevice(http, client);
		VaadinPage page = signIn(device.path("verification_uri_complete").asText(), user);
		assertThat(page.visible(By.className("u-consentMainColumn")).getText())
				.contains(device.path("user_code").asText(), "Local Selenium client");
		screenshot("Device code confirmation");
		page.click(testId("DeviceSignInView.confirmCode"));
		page.click(By.className("u-consentDeclineButton"));
		page.visible(By.className("u-final-error"));
		LoopbackHttpClient.Result denied = poll(http, client, device);
		assertThat(denied.status()).isEqualTo(400);
		assertThat(denied.body().path("error").asText()).isEqualTo("access_denied");
		assertThat(denied.body().has("access_token")).isFalse();
	}

	@Test
	void shouldClearWrongManualCodeAndAllowRetryWithCorrectCode() throws Exception
	{
		TestUser client = oauthClient();
		TestUser user = createUser("Device retry owner");
		deployDeviceFlow();
		LoopbackHttpClient http = new LoopbackHttpClient();
		JsonNode device = requestDevice(http, client);
		VaadinPage page = signIn(device.path("verification_uri").asText(), user);
		page.fill(testId("DeviceSignInView.code"), "WRONG-CODE");
		page.click(testId("DeviceSignInView.submitCode"));
		page.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(page.value(testId("DeviceSignInView.code"))).isEmpty();
		assertThat(poll(http, client, device).body().path("error").asText()).isEqualTo("authorization_pending");
		screenshot("Invalid device code retry");
		page.fill(testId("DeviceSignInView.code"), device.path("user_code").asText());
		page.click(testId("DeviceSignInView.submitCode"));
		page.click(By.className("u-consentDeclineButton"));
		page.visible(By.className("u-final-error"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("unusableCodes")
	void shouldRejectUnusablePrefilledCodeWithoutIssuingTokens(CodeState scenario) throws Exception
	{
		TestUser client = oauthClient();
		TestUser user = createUser("Device error owner");
		deployDeviceFlow();
		LoopbackHttpClient http = new LoopbackHttpClient();
		JsonNode device = requestDevice(http, client);
		Token stored = tokens.getTokenById(DeviceCodeRepository.INTERNAL_DEVICE_TOKEN,
				device.path("device_code").asText());
		if (scenario.state().equals("expired"))
			tokens.updateToken(stored.getType(), stored.getValue(), Date.from(Instant.now().minusSeconds(1)),
					stored.getContents());
		else if (scenario.state().equals("processed"))
		{
			DeviceCodeToken parsed = DeviceCodeToken.getInstanceFromJson(stored.getContents());
			parsed.setDeviceCodeStatus(DeviceCodeStatus.DENIED);
			tokens.updateToken(stored.getType(), stored.getValue(), stored.getExpires(), parsed.getSerialized());
		} else
		{
			var endpoint = endpoints.getEndpoints().stream()
					.filter(item -> item.getContextAddress().equals("/device")).findFirst().orElseThrow();
			EndpointConfiguration previous = endpoint.getConfiguration();
			endpoints.updateEndpoint(endpoint.getName(), new EndpointConfiguration(previous.getDisplayedName(),
					previous.getDescription(), previous.getAuthenticationOptions(),
					previous.getConfiguration().replace("deviceGrantEnabled=true", "deviceGrantEnabled=false"),
					previous.getRealm()));
		}
		VaadinPage page = signIn(device.path("verification_uri_complete").asText(), user);
		assertThat(page.visible(By.className("u-final-error")).getText()).containsIgnoringCase(scenario.message());
		assertThat(driver.findElements(By.className("u-consentConfirmButton"))).isEmpty();
		assertThat(poll(http, client, device).body().has("access_token")).isFalse();
		screenshot("Unusable device code");
	}

	private VaadinPage signIn(String url, TestUser user)
	{
		driver.get(url);
		VaadinPage page = new VaadinPage(driver);
		page.signIn(user.username(), user.password());
		return page;
	}

	private JsonNode requestDevice(LoopbackHttpClient http, TestUser client) throws Exception
	{
		LoopbackHttpClient.Result issued = http.post(baseUrl + "/oauth/device_authorization",
				Map.of("client_id", client.username(), "scope", "openid profile"));
		assertThat(issued.status()).as("device authorization response: %s", issued.body()).isEqualTo(200);
		assertThat(issued.body().path("device_code").asText()).isNotBlank();
		return issued.body();
	}

	private LoopbackHttpClient.Result poll(LoopbackHttpClient http, TestUser client, JsonNode device) throws Exception
	{
		return http.post(baseUrl + "/oauth/token", Map.of("client_id", client.username(),
				"grant_type", "urn:ietf:params:oauth:grant-type:device_code",
				"device_code", device.path("device_code").asText()));
	}

	private void clearPollDelay(JsonNode device) throws Exception
	{
		Token stored = tokens.getTokenById(DeviceCodeRepository.INTERNAL_DEVICE_TOKEN,
				device.path("device_code").asText());
		DeviceCodeToken parsed = DeviceCodeToken.getInstanceFromJson(stored.getContents());
		parsed.setLastPolledAt(Instant.now().minusSeconds(60));
		tokens.updateToken(stored.getType(), stored.getValue(), stored.getExpires(), parsed.getSerialized());
	}

	private static Stream<CodeState> unusableCodes()
	{
		return Stream.of(CodeState.builder().state("expired").message("invalid").build(),
				CodeState.builder().state("processed").message("already").build(),
				CodeState.builder().state("disabled").message("not enabled").build());
	}

	private record CodeState(String state, String message)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private String state;
			private String message;

			Builder state(String value)
			{
				state = value;
				return this;
			}

			Builder message(String value)
			{
				message = value;
				return this;
			}

			CodeState build()
			{
				return new CodeState(state, message);
			}
		}
	}
}
