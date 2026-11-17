package io.imunity.test.ui.home;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import io.imunity.test.ui.UiFixtureInitializer;
import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.EndUserTestBase;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.session.SessionManagement;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.utils.SensitiveAttributeMetadataProvider;

@Tag("home")
@ContextConfiguration(initializers = AdditionalAuthenticationTest.ShortGracePeriod.class)
@TestPropertySource(properties = "unityConfig=target/ui-fixtures/reauthentication.conf")
class AdditionalAuthenticationTest extends EndUserTestBase
{
	private static final String ATTRIBUTE = "ui-sensitive-profile";
	private static final By VALUE = By.cssSelector("[data-testid='ProfileView.attribute']"
			+ "[data-attribute-name='" + ATTRIBUTE + "'] vaadin-text-field");
	private static final By DIALOG = By.cssSelector("vaadin-dialog[opened]");

	@Autowired
	@Qualifier("insecure")
	private AttributeTypeManagement types;
	@Autowired
	private SessionManagement sessions;

	@Test
	void shouldPreserveSensitiveValueAndCloseAdditionalAuthenticationOnEscape() throws Exception
	{
		TestUser user = sensitiveProfileOwner();
		VaadinPage page = openProtectedEdit(user);
		page.visible(By.className("u-passwordField")).findElement(By.tagName("input")).sendKeys(Keys.ESCAPE);
		assertThat(storedValue(user)).isEqualTo("Original value");
		waitFor(() -> driver.findElements(DIALOG).stream().noneMatch(element -> element.isDisplayed()));
		driver.navigate().refresh();
		assertThat(page.value(VALUE)).isEqualTo("Original value");
	}

	@Test
	void shouldSaveSensitiveValueAfterCompletingAdditionalAuthentication() throws Exception
	{
		TestUser user = sensitiveProfileOwner();
		VaadinPage page = openProtectedEdit(user);
		page.fill(By.className("u-passwordField"), user.password());
		page.click(By.className("u-passwordSignInButton"));
		waitForElementNotPresent(testId("ProfileView.save"));
		assertThat(storedValue(user)).isEqualTo("Protected replacement");
		driver.navigate().refresh();
		assertThat(page.value(VALUE)).isEqualTo("Protected replacement");
	}

	private TestUser sensitiveProfileOwner() throws Exception
	{
		AttributeType sensitive = new AttributeType(ATTRIBUTE, "string");
		sensitive.setSelfModificable(true);
		sensitive.setMetadata(Map.of(SensitiveAttributeMetadataProvider.NAME, ""));
		sensitive.setMaxElements(1);
		types.addAttributeType(sensitive);
		TestUser user = createUser("Sensitive profile owner");
		attributes.setAttribute(user.entity(), StringAttribute.of(ATTRIBUTE, "/", "Original value"));
		deployHomeAt("/sensitive-home", """
				unity.userhome.attributes.1.attribute=ui-sensitive-profile
				unity.userhome.attributes.1.group=/
				unity.userhome.attributes.1.editable=true
				""");
		return user;
	}

	private VaadinPage openProtectedEdit(TestUser user) throws Exception
	{
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/sensitive-home", user.username(), user.password());
		LoginSession session = sessions.getOwnedSession(user.entity(), "ui");
		waitFor(() -> Instant.now().isAfter(session.getAuthenticationTime().plusSeconds(3)));
		driver.get(baseUrl + "/sensitive-home/profile");
		page.fill(VALUE, "Protected replacement");
		page.click(testId("ProfileView.save"));
		page.visible(DIALOG);
		screenshot("Additional authentication dialog");
		return page;
	}

	public static class ShortGracePeriod implements ApplicationContextInitializer<ConfigurableApplicationContext>
	{
		@Override
		public void initialize(ConfigurableApplicationContext context)
		{
			new UiFixtureInitializer().initialize(context);
			try
			{
				Files.writeString(Path.of("target/ui-fixtures/reauthentication.conf"),
						Files.readString(Path.of("target/ui-fixtures/unityServer.conf"))
								+ "\nunityServer.core.reAuthenticationGraceTime=2\n");
			} catch (java.io.IOException e)
			{
				throw new java.io.UncheckedIOException(e);
			}
		}
	}

	private String storedValue(TestUser user) throws Exception
	{
		return attributes.getAttributes(user.entity(), "/", ATTRIBUTE).iterator().next().getValues().getFirst();
	}
}
