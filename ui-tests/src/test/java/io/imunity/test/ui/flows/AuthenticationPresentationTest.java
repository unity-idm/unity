package io.imunity.test.ui.flows;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;

import io.imunity.test.ui.VaadinPage;

@Tag("authentication")
class AuthenticationPresentationTest extends SamlWorkflowTestBase
{
	@ParameterizedTest(name = "{0}")
	@MethodSource("layouts")
	void shouldSwitchLanguageAndAuthenticateThroughConfiguredLayout(Layout scenario) throws Exception
	{
		TestUser user = createUser("Authentication layout owner");
		deploySaml("", List.of());
		deployAt("UserHomeUI", "/login-layout", """
				unity.endpoint.web.authnScreenTitle.en=Choose your Selenium sign-in
				unity.endpoint.web.authnScreenTitle.pl=Wybierz logowanie Selenium
				unity.endpoint.web.authnScreenColumn.1.columnTitle=Local account
				unity.endpoint.web.authnScreenColumn.1.columnContents=pwdSys
				unity.endpoint.web.authnScreenColumn.2.columnTitle=Federated account
				unity.endpoint.web.authnScreenColumn.2.columnContents=%s
				""".formatted(scenario.grid() ? "_GRID_ui" : "ui-saml") + (scenario.grid() ? """
				unity.endpoint.web.authnGrid.ui.gridContents=ui-saml
				unity.endpoint.web.authnScreenShowSearch=true
				""" : ""), List.of("pwdSys", "ui-saml"));
		driver.manage().window().setSize(new Dimension(scenario.width(), 1000));
		driver.get(baseUrl + "/login-layout");
		VaadinPage page = new VaadinPage(driver);
		page.visible(By.className("u-passwordUsernameField"));
		assertThat(driver.findElement(By.tagName("body")).getText()).contains("Choose your Selenium sign-in");
		page.select(By.className("u-authn-languageSelector"), "Polski");
		waitFor(() -> driver.findElement(By.tagName("body")).getText().contains("Wybierz logowanie Selenium"));
		screenshot("Polish authentication layout");
		if (scenario.grid())
		{
			page.click(By.cssSelector("[class~='u-idpAuthentication-ui-saml.1']"));
			waitForPageLoadByURL("/saml-idp/authentication");
			page.signIn(user.username(), user.password());
			page.click(By.className("u-consentConfirmButton"));
		} else
		{
			page.fill(By.className("u-passwordUsernameField"), user.username());
			page.fill(By.className("u-passwordField"), user.password());
			page.visible(By.className("u-passwordSignInButton")).sendKeys(Keys.ENTER);
		}
		assertThat(page.visible(By.id("MainHeader.loggedAs")).getText()).contains(user.displayName());
		page.click(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"));
		page.visible(By.className("u-passwordUsernameField"));
		driver.get(baseUrl + "/login-layout/profile");
		page.visible(By.className("u-passwordUsernameField"));
		assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
	}

	private static Stream<Layout> layouts()
	{
		return Stream.of(Layout.builder().grid(false).width(420).build(),
				Layout.builder().grid(true).width(1280).build());
	}

	private record Layout(boolean grid, int width)
	{
		static Builder builder()
		{
			return new Builder();
		}

		private static class Builder
		{
			private boolean grid;
			private int width;

			Builder grid(boolean value)
			{
				grid = value;
				return this;
			}

			Builder width(int value)
			{
				width = value;
				return this;
			}

			Layout build()
			{
				return new Layout(grid, width);
			}
		}
	}
}
