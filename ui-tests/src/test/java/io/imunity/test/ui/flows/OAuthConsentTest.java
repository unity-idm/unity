package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences;
import pl.edu.icm.unity.oauth.as.preferences.OAuthPreferences.OAuthClientSettings;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttribute;

@Tag("oauth")
class OAuthConsentTest extends OAuthWorkflowTestBase
{
	private static final String VERIFIER = "selenium-local-code-verifier-1605-0123456789abcdef";
	private static final String STATE = "selenium-client-state";

	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;
	@Autowired
	@Qualifier("insecure")
	private PreferencesManagement preferences;

	@Test
	void shouldReleaseSelectedValueAndDynamicGroupClaimToLocalClient() throws Exception
	{
		TestUser client = oauthClient();
		TestUser user = createUser("OAuth consent owner");
		AttributeType affiliation = new AttributeType("ui-affiliation", "string");
		affiliation.setMaxElements(2);
		types.addAttributeType(affiliation);
		attributes.setAttribute(user.entity(), StringAttribute.of("ui-affiliation", "/", List.of("staff", "student")));
		deployProtocol("""
				unity.oauth2.as.scopes.3.name=affiliation
				unity.oauth2.as.scopes.3.attributes.1=ui-affiliation
				unity.oauth2.as.scopes.4.name=memberships
				unity.oauth2.as.scopes.4.attributes.1=memberOf
				unity.oauth2.as.activeValue.1.singleValueAttributes.1=ui-affiliation
				""");
		String scopes = "openid profile affiliation memberships";
		driver.get(authorizationUrl(client, scopes));
		VaadinPage page = new VaadinPage(driver);
		page.signIn(user.username(), user.password());
		page.click(By.xpath("//vaadin-radio-button[contains(.,'student')]"));
		page.click(By.className("u-consentConfirmButton"));
		page.click(By.cssSelector("[id='ExposedAttributes.showDetails'] vaadin-details-summary"));
		assertThat(page.value(By.xpath("//vaadin-text-field[label[.='ui-affiliation']]"))).isEqualTo("student");
		screenshot("OAuth consent and selected attribute");
		page.click(By.className("u-consentConfirmButton"));
		Map<String, String> response = callback(driver);
		assertThat(response).containsEntry("state", STATE).containsKey("code").doesNotContainKey("error");
		LoopbackHttpClient http = new LoopbackHttpClient();
		LoopbackHttpClient.Result token = http.post(baseUrl + "/oauth/token", Map.of("grant_type", "authorization_code",
				"client_id", client.username(), "code", response.get("code"), "code_verifier", VERIFIER,
				"redirect_uri", callbackUrl()));
		assertThat(token.status()).as("authorization code exchange: %s", token.body()).isEqualTo(200);
		LoopbackHttpClient.Result userInfo = http.get(baseUrl + "/oauth/userinfo",
				Map.of("Authorization", "Bearer " + token.body().path("access_token").asText()));
		assertThat(userInfo.status()).isEqualTo(200);
		assertThat(userInfo.body().path("sub").asText()).isEqualTo(user.username());
		assertThat(userInfo.body().path("ui-affiliation").toString()).contains("student").doesNotContain("staff");
		assertThat(userInfo.body().path("memberOf").toString()).contains("/");
		assertThat(userInfo.body().has("sys:AuthorizationRole")).isFalse();
	}

	@Test
	void shouldReturnDenialAndPreserveClientStateWithoutGrant() throws Exception
	{
		TestUser client = oauthClient();
		TestUser user = createUser("OAuth denial owner");
		deployProtocol("");
		driver.get(authorizationUrl(client, "openid profile"));
		VaadinPage page = new VaadinPage(driver);
		page.signIn(user.username(), user.password());
		screenshot("OAuth consent before denial");
		page.click(By.className("u-consentDeclineButton"));
		assertThat(callback(driver)).containsEntry("error", "access_denied").containsEntry("state", STATE)
				.doesNotContainKey("code");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rememberedDecisions")
	void shouldHonorRememberedDecisionAndRestoreConsentAfterHomeRevocation(RememberedDecision scenario) throws Exception
	{
		TestUser client = oauthClient();
		attributes.setAttribute(client.entity(),
				EnumAttribute.of("sys:oauth:clientType", CLIENT_GROUP, "CONFIDENTIAL"));
		TestUser user = createUser("Trusted application owner");
		deployProtocol("");
		OAuthPreferences remembered = new OAuthPreferences();
		OAuthClientSettings decision = new OAuthClientSettings();
		decision.setDoNotAsk(true);
		decision.setDefaultAccept(scenario.accept());
		decision.setEffectiveRequestedScopes(Set.of("openid", "profile"));
		decision.setTimestamp(Instant.now());
		remembered.setSPSettings(client.username(), decision);
		preferences.setPreference(user.entity(), OAuthPreferences.ID,
				remembered.getSerializedConfiguration().toString());
		driver.get(authorizationUrl(client, "openid profile"));
		new VaadinPage(driver).signIn(user.username(), user.password());
		Map<String, String> response = callback(driver);
		if (scenario.accept())
			assertThat(response).containsKey("code").doesNotContainKey("error");
		else
			assertThat(response).containsEntry("error", "access_denied").doesNotContainKey("code");

		WebDriver home = newBrowser();
		VaadinPage page = new VaadinPage(home);
		page.login(baseUrl, "/home", user.username(), user.password());
		home.get(baseUrl + "/home/trusted-applications");
		page.click(By.cssSelector("vaadin-accordion-heading"));
		screenshot(home, "Remembered OAuth application");
		if (scenario.accept())
		{
			page.click(By.xpath("//span[contains(.,'technical information')]"));
			assertThat(page.visible(By.id("technical-info-content")).getText()).contains("OAuth");
			page.click(By.xpath("//vaadin-button[normalize-space(.)='Revoke access']"));
			page.click(By.cssSelector("vaadin-confirm-dialog [slot='confirm-button']"));
			waitFor(() -> home.findElements(By.cssSelector("vaadin-confirm-dialog")).stream()
					.noneMatch(element -> element.isDisplayed()));
		} else
			page.click(By.xpath("//vaadin-button[normalize-space(.)='Unblock access']"));
		home.navigate().refresh();
		assertThat(home.findElement(By.tagName("body")).getText()).doesNotContain("Local Selenium client");
		driver.get(authorizationUrl(client, "openid profile"));
		assertThat(new VaadinPage(driver).visible(By.className("u-consentMainColumn")).getText())
				.contains("Local Selenium client");
		new VaadinPage(driver).click(By.className("u-consentDeclineButton"));
		assertThat(callback(driver)).containsEntry("error", "access_denied");
	}

	private void deployProtocol(String settings) throws Exception
	{
		deployAt("OAuth2Token", "/oauth", CONFIGURATION + settings, List.of("pwdSys"));
		deployAt("OAuth2Authz", "/authorize", CONFIGURATION + settings);
	}

	private String authorizationUrl(TestUser client, String scope) throws Exception
	{
		String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
				MessageDigest.getInstance("SHA-256").digest(VERIFIER.getBytes(StandardCharsets.US_ASCII)));
		return baseUrl + "/authorize/oauth2-authz?" + Map.of("response_type", "code", "client_id", client.username(),
				"redirect_uri", callbackUrl(), "scope", scope, "state", STATE,
				"code_challenge_method", "S256", "code_challenge", challenge).entrySet().stream()
				.map(entry -> LoopbackHttpClient.encode(entry.getKey()) + "="
						+ LoopbackHttpClient.encode(entry.getValue()))
				.collect(Collectors.joining("&"));
	}

	private String callbackUrl()
	{
		return baseUrl + "/unitygw/pub/registration/oauth-callback";
	}

	private Map<String, String> callback(WebDriver browser)
	{
		waitFor(() -> browser.getCurrentUrl().startsWith(callbackUrl() + "?"));
		return Arrays.stream(URI.create(browser.getCurrentUrl()).getRawQuery().split("&"))
				.map(entry -> entry.split("=", 2)).collect(Collectors.toMap(entry -> decode(entry[0]),
						entry -> entry.length == 2 ? decode(entry[1]) : ""));
	}

	private static String decode(String value)
	{
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private static Stream<RememberedDecision> rememberedDecisions()
	{
		return Stream.of(RememberedDecision.builder().accept(true).build(),
				RememberedDecision.builder().accept(false).build());
	}

	private record RememberedDecision(boolean accept)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private boolean accept;

			Builder accept(boolean value)
			{
				accept = value;
				return this;
			}

			RememberedDecision build()
			{
				return new RememberedDecision(accept);
			}
		}
	}
}
