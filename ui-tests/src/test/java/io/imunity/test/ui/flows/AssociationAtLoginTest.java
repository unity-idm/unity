package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;

@Tag("association")
class AssociationAtLoginTest extends SamlWorkflowTestBase
{
	@Autowired
	@Qualifier("insecure")
	private TranslationProfileManagement profiles;

	@ParameterizedTest(name = "{0}")
	@MethodSource("confirmations")
	void shouldLinkUnknownRemoteIdentityOnlyAfterConfirmingLocalAccount(Confirmation scenario) throws Exception
	{
		TestUser user = createUser("Association at login owner");
		var original = entities.getEntity(user.entity()).getIdentities();
		String remoteUsername = "ui-remote-" + user.username();
		deploySaml("", List.of());
		profiles.updateProfile(new TranslationProfile("ui-saml-input", "Unknown remote account", ProfileType.INPUT,
				List.of(new TranslationRule("true", new TranslationAction("mapIdentity", "userName",
						"'" + remoteUsername + "'", "ui-password", "UPDATE_OR_MATCH")))));
		deployAt("UserHomeUI", "/association-login", "", List.of("pwdSys", "ui-saml"));
		driver.get(baseUrl + "/association-login");
		VaadinPage page = new VaadinPage(driver);
		page.click(PROVIDER);
		waitForPageLoadByURL("/saml-idp/authentication");
		page.signIn(user.username(), user.password());
		page.click(By.className("u-consentConfirmButton"));
		page.click(By.id("UnknownUserDialog.associate"));
		String source = driver.getWindowHandle();
		page.click(By.id("Wizard.next"));
		waitForUi();
		if (driver.getWindowHandles().size() == 1)
			page.click(By.xpath("//vaadin-button[normalize-space(.)='Open sandbox window']"));
		waitFor(() -> driver.getWindowHandles().size() > 1);
		driver.switchTo().window(driver.getWindowHandles().stream().filter(handle -> !handle.equals(source))
				.findFirst().orElseThrow());
		page.signIn(user.username(), user.password());
		driver.switchTo().window(source);
		page.visible(By.id("Wizard.finish"));
		assertThat(entities.getEntity(user.entity()).getIdentities()).containsExactlyElementsOf(original);
		if (!scenario.confirm())
		{
			page.click(By.id("Wizard.cancel"));
			assertThat(entities.getEntity(user.entity()).getIdentities()).containsExactlyElementsOf(original);
			driver.navigate().refresh();
			assertThat(entities.getEntity(user.entity()).getIdentities()).containsExactlyElementsOf(original);
			return;
		}
		page.click(By.id("Wizard.finish"));
		waitForElementNotPresent(By.id("Wizard.finish"));
		assertThat(entities.getEntity(user.entity()).getIdentities()).hasSize(original.size() + 1)
				.anySatisfy(identity ->
				{
					assertThat(identity.getValue()).isEqualTo(remoteUsername);
					assertThat(identity.getRemoteIdp()).isEqualTo("https://localhost:2443/ui-saml-idp");
				});
		assertThat(entities.getEntity(new EntityParam(new IdentityTaV("userName", remoteUsername))).getId())
				.isEqualTo(user.entity().getEntityId());
		var fresh = newBrowser();
		VaadinPage login = new VaadinPage(fresh);
		fresh.get(baseUrl + "/association-login");
		login.click(PROVIDER);
		waitFor(() -> fresh.getCurrentUrl().contains("/saml-idp/authentication"));
		login.signIn(user.username(), user.password());
		login.click(By.className("u-consentConfirmButton"));
		assertThat(login.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		assertThat(fresh.findElements(By.id("UnknownUserDialog.associate"))).isEmpty();
	}

	private static Stream<Confirmation> confirmations()
	{
		return Stream.of(Confirmation.builder().confirm(false).build(), Confirmation.builder().confirm(true).build());
	}

	private record Confirmation(boolean confirm)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private boolean confirm;

			Builder confirm(boolean value)
			{
				confirm = value;
				return this;
			}

			Confirmation build()
			{
				return new Confirmation(confirm);
			}
		}
	}
}
