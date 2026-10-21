package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.accounts.UserAccountTestBase;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;

@Tag("admin-directory")
class IdentityEditorTest extends UserAccountTestBase
{
	@Autowired
	private IdentityEditorRegistry editors;

	@ParameterizedTest(name = "{0}")
	@MethodSource("identityCases")
	void shouldAddReloadAndRemoveIdentityWithoutChangingOtherAccounts(IdentityCase scenario) throws Exception
	{
		TestUser user = createUser("Identity owner");
		TestUser control = createUser("Identity control");
		List<Identity> original = entities.getEntity(user.entity()).getIdentities();
		List<Identity> unrelated = entities.getEntity(control.entity()).getIdentities();
		String value = value(scenario);
		VaadinPage page = openEditor(user, scenario);
		fill(scenario, value);
		page.click(testId("IdentityCreationDialog", "DialogWithActionFooter.confirm"));
		waitForElementNotPresent(testId("IdentityCreationDialog"));
		assertMapping(user, scenario, value);
		assertThat(entities.getEntity(user.entity()).getIdentities()).hasSize(original.size() + 1).containsAll(original);
		assertThat(entities.getEntity(control.entity()).getIdentities()).containsExactlyElementsOf(unrelated);
		if (scenario.passwordLogin())
			assertAccepted(user, value);
		else
			assertAccepted(user, user.username());

		driver.navigate().refresh();
		new DirectoryPage(driver).selectGroup("/");
		DirectoryWidgetPage directory = new DirectoryWidgetPage(driver);
		directory.expandEntity(user.entity());
		String storedValue = entities.getEntity(user.entity()).getIdentities().stream()
				.filter(identity -> identity.getTypeId().equals(scenario.type()) && !original.contains(identity))
				.map(Identity::getValue).findFirst().orElseThrow();
		page.visible(By.cssSelector("[data-testid='IdentitiesTreeGrid.entityActions'][data-entity-id='"
				+ user.entity().getEntityId() + "'][data-identity-value='" + storedValue + "']"));
		directory.removeIdentity(user.entity(), storedValue);
		assertThat(entities.getEntity(user.entity()).getIdentities()).containsExactlyInAnyOrderElementsOf(original);
		assertThatThrownBy(() -> entities.getEntity(new EntityParam(new IdentityParam(scenario.type(), value))))
				.isInstanceOf(UnknownIdentityException.class);
		assertThat(entities.getEntity(control.entity()).getIdentities()).containsExactlyElementsOf(unrelated);
		if (scenario.passwordLogin())
			assertRejected(user, value);
		assertAccepted(user, user.username());
		assertAccepted(control, control.username());
	}

	@ParameterizedTest(name = "invalid {0}")
	@MethodSource("identityCases")
	void shouldRejectInvalidIdentityAndAllowCorrectedValue(IdentityCase scenario) throws Exception
	{
		TestUser user = createUser("Invalid identity owner");
		List<Identity> original = entities.getEntity(user.entity()).getIdentities();
		VaadinPage page = openEditor(user, scenario);
		fill(scenario, scenario.invalid());
		page.click(testId("IdentityCreationDialog", "DialogWithActionFooter.confirm"));
		waitFor(() -> driver.findElements(By.cssSelector("[data-testid='IdentityCreationDialog'] [invalid]"))
				.stream().anyMatch(WebElement::isDisplayed));
		assertThat(page.visible(testId("IdentityCreationDialog")).isDisplayed()).isTrue();
		assertThat(entities.getEntity(user.entity()).getIdentities()).containsExactlyElementsOf(original);

		String corrected = value(scenario);
		fill(scenario, corrected);
		page.click(testId("IdentityCreationDialog", "DialogWithActionFooter.confirm"));
		waitForElementNotPresent(testId("IdentityCreationDialog"));
		assertMapping(user, scenario, corrected);
		assertThat(entities.getEntity(user.entity()).getIdentities()).hasSize(original.size() + 1).containsAll(original);
		assertAccepted(user, scenario.passwordLogin() ? corrected : user.username());
	}

	private VaadinPage openEditor(TestUser user, IdentityCase scenario)
	{
		assertThat(editors.getSupportedTypes()).containsExactlyInAnyOrderElementsOf(
				identityCases().map(IdentityCase::type).toList());
		AccountAdministrationPage account = new AccountAdministrationPage(driver);
		account.open(baseUrl);
		account.action(user.entity(), "Create identity");
		VaadinPage page = new VaadinPage(driver);
		page.select(testId("IdentityCreationDialog", "IdentityCreationDialog.identityType"), scenario.type());
		return page;
	}

	private void fill(IdentityCase scenario, String value)
	{
		VaadinPage page = new VaadinPage(driver);
		By field = By.cssSelector("[data-testid='IdentityCreationDialog'] "
				+ (scenario.type().equals("email") ? "vaadin-custom-field" : "vaadin-text-field")
				+ "[data-testid='IdentityCreationDialog.value']");
		page.fill(field, value);
		if (scenario.type().equals("email"))
			page.check(By.cssSelector("[data-testid='IdentityCreationDialog.value'] vaadin-checkbox"), true);
	}

	private void assertMapping(TestUser user, IdentityCase scenario, String value) throws Exception
	{
		assertThat(entities.getEntity(new EntityParam(new IdentityParam(scenario.type(), value))).getId())
				.isEqualTo(user.entity().getEntityId());
		assertThat(entities.getEntity(user.entity()).getIdentities()).anySatisfy(identity ->
		{
			assertThat(identity.getTypeId()).isEqualTo(scenario.type());
			assertThat(identity.getEntityId()).isEqualTo(user.entity().getEntityId());
			assertThat(identity.getValue()).contains(scenario.type().equals("x500Name")
					? value.substring(3, value.indexOf(',')) : value);
		});
	}

	private static String value(IdentityCase scenario)
	{
		String suffix = UUID.randomUUID().toString();
		return switch (scenario.type())
		{
			case "email" -> "alias-" + suffix + "@example.org";
			case "x500Name" -> "CN=Alias-" + suffix + ",O=Selenium";
			default -> "alias-" + suffix;
		};
	}

	private void assertAccepted(TestUser user, String identity)
	{
		VaadinPage home = new VaadinPage(newBrowser());
		home.login(baseUrl, "/home", identity, user.password());
		assertThat(home.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
	}

	private void assertRejected(TestUser user, String identity)
	{
		WebDriver browser = newBrowser();
		VaadinPage home = new VaadinPage(browser);
		browser.get(baseUrl + "/home");
		home.signIn(identity, user.password());
		home.visible(By.cssSelector("vaadin-notification-card[theme~='error']"));
		assertThat(browser.getCurrentUrl()).contains("/home/authentication");
		assertThat(browser.findElements(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"))).isEmpty();
	}

	private static Stream<IdentityCase> identityCases()
	{
		return Stream.of(IdentityCase.builder("userName").invalid("").passwordLogin().build(),
				IdentityCase.builder("email").invalid("invalid-email").passwordLogin().build(),
				IdentityCase.builder("identifier").invalid("").build(),
				IdentityCase.builder("x500Name").invalid("invalid-distinguished-name").build());
	}

	record IdentityCase(String type, String invalid, boolean passwordLogin)
	{
		static Builder builder(String type)
		{
			return new Builder(type);
		}

		@Override
		public String toString()
		{
			return type;
		}

		static final class Builder
		{
			private final String type;
			private String invalid;
			private boolean passwordLogin;

			private Builder(String type)
			{
				this.type = type;
			}

			Builder invalid(String invalid)
			{
				this.invalid = invalid;
				return this;
			}

			Builder passwordLogin()
			{
				passwordLogin = true;
				return this;
			}

			IdentityCase build()
			{
				return new IdentityCase(type, invalid, passwordLogin);
			}
		}
	}
}
