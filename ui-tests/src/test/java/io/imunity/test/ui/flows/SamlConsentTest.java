package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.stdext.attr.StringAttribute;

@Tag("saml")
class SamlConsentTest extends SamlWorkflowTestBase
{
	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;
	@Autowired
	@Qualifier("insecure")
	private PreferencesManagement preferences;
	@Autowired
	private SessionManagement sessions;
	@Autowired
	private UnityServerConfiguration configuration;

	@Test
	void shouldPersistChosenSamlIdentityAndReuseSsoUntilLogout() throws Exception
	{
		configuration.setProperty(UnityServerConfiguration.LOGOUT_MODE, "internalAndAsyncPeers");
		TestUser user = createUser("SAML identity selection owner");
		String alias = "saml-alias-" + user.entity().getEntityId();
		entities.addIdentity(new IdentityParam("userName", alias), user.entity());
		types.addAttributeType(new AttributeType("ui-selected-identity", "string"));
		deploySaml("", List.of(new TranslationRule("true", new TranslationAction("mapAttribute",
				"ui-selected-identity", "/", "id", "CREATE_OR_UPDATE"))), "unity:userName");
		driver.get(baseUrl + "/saml-home");
		VaadinPage page = new VaadinPage(driver);
		page.click(PROVIDER);
		page.signIn(user.username(), user.password());
		page.visible(By.className("u-consentConfirmButton"));
		page.select(By.cssSelector("vaadin-combo-box"), alias);
		page.check(By.className("u-consent-screen-checkbox"), true);
		screenshot("SAML identity selection");
		page.click(By.className("u-consentConfirmButton"));
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		assertThat(attributes.getAttributes(user.entity(), "/", "ui-selected-identity")).singleElement()
				.satisfies(attribute -> assertThat(attribute.getValues()).containsExactly(alias));
		SamlPreferences remembered = SamlPreferences.getPreferences(preferences, user.entity());
		assertThat(remembered.getSPSettings(SP).getSelectedIdentity()).isEqualTo(alias);

		sessions.removeSession(sessions.getOwnedSession(user.entity(), "ui").getId(), true);
		driver.get(baseUrl + "/saml-home");
		var next = page.visible(By.cssSelector("[id='MainHeader.loggedAs'], .u-idpAuthentication-ui-saml-1"));
		if (!"MainHeader.loggedAs".equals(next.getDomAttribute("id")))
			page.click(PROVIDER);
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		assertThat(driver.findElements(By.className("u-passwordField"))).isEmpty();
		page.click(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"));
		page.visible(PROVIDER);
		page.click(PROVIDER);
		page.visible(By.className("u-passwordUsernameField"));
		assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
	}

	@Test
	void shouldReleaseChosenValueAndAuthenticateCorrectLocalSpUser() throws Exception
	{
		TestUser user = createUser("SAML consent owner");
		AttributeType choices = new AttributeType("ui-affiliation", "string");
		choices.setMaxElements(2);
		types.addAttributeType(choices);
		types.addAttributeType(new AttributeType("ui-selected-affiliation", "string"));
		attributes.setAttribute(user.entity(), StringAttribute.of("ui-affiliation", "/", List.of("staff", "student")));
		deploySaml("unity.saml.activeValue.1.singleValueAttributes.1=ui-affiliation\n", List.of(
				new TranslationRule("true", new TranslationAction("mapAttribute", "ui-selected-affiliation", "/",
						"attr['ui-affiliation']", "CREATE_OR_UPDATE"))));
		driver.get(baseUrl + "/saml-home");
		VaadinPage page = new VaadinPage(driver);
		page.click(PROVIDER);
		page.signIn(user.username(), user.password());
		page.click(By.xpath("//vaadin-radio-button[contains(.,'student')]"));
		page.click(By.className("u-consentConfirmButton"));
		page.click(By.cssSelector("vaadin-details-summary"));
		By selected = By.xpath("//vaadin-vertical-layout[span[.='ui-affiliation']]//vaadin-text-field");
		assertThat(page.value(selected)).isEqualTo("student");
		screenshot("SAML consent and selected attribute");
		page.click(By.className("u-consentConfirmButton"));
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		assertThat(attributes.getAttributes(user.entity(), "/", "ui-selected-affiliation")).singleElement()
				.satisfies(attribute -> assertThat(attribute.getValues()).containsExactly("student"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("decisions")
	void shouldHonorRememberedSamlChoiceAndRestoreConsentAfterHomeAction(Decision scenario) throws Exception
	{
		TestUser user = createUser("SAML remembered consent owner");
		deploySaml("", List.of());
		SamlPreferences remembered = new SamlPreferences();
		SPSettings choice = new SPSettings();
		choice.setDoNotAsk(true);
		choice.setDefaultAccept(scenario.accept());
		choice.setTimestamp(Instant.now());
		remembered.setSPSettings(SP, choice);
		preferences.setPreference(user.entity(), SamlPreferences.ID,
				remembered.getSerializedConfiguration().toString());
		driver.get(baseUrl + "/saml-home");
		VaadinPage page = new VaadinPage(driver);
		page.click(PROVIDER);
		page.signIn(user.username(), user.password());
		if (scenario.accept())
		{
			assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
			driver.get(baseUrl + "/saml-home/trusted-applications");
		} else
		{
			page.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
			assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
			page.login(baseUrl, "/home", user.username(), user.password());
			driver.get(baseUrl + "/home/trusted-applications");
		}
		page.click(By.cssSelector("vaadin-accordion-heading"));
		screenshot("Remembered SAML application");
		if (scenario.accept())
		{
			page.click(By.xpath("//span[contains(.,'technical information')]"));
			assertThat(page.visible(By.id("technical-info-content")).getText()).contains("SAML");
			page.click(By.xpath("//vaadin-button[normalize-space(.)='Revoke access']"));
			page.click(By.cssSelector("vaadin-confirm-dialog [slot='confirm-button']"));
			waitFor(() -> driver.findElements(By.cssSelector("vaadin-confirm-dialog")).stream()
					.noneMatch(element -> element.isDisplayed()));
		} else
			page.click(By.xpath("//vaadin-button[normalize-space(.)='Unblock access']"));
		driver.navigate().refresh();
		assertThat(driver.findElement(By.tagName("body")).getText()).doesNotContain("Local Selenium SP");
		var fresh = newBrowser();
		VaadinPage login = new VaadinPage(fresh);
		fresh.get(baseUrl + "/saml-home");
		login.click(PROVIDER);
		login.signIn(user.username(), user.password());
		login.click(By.className("u-consentDeclineButton"));
		login.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(fresh.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
	}

	private static Stream<Decision> decisions()
	{
		return Stream.of(Decision.builder().accept(true).build(), Decision.builder().accept(false).build());
	}

	private record Decision(boolean accept)
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

			Decision build()
			{
				return new Decision(accept);
			}
		}
	}
}
