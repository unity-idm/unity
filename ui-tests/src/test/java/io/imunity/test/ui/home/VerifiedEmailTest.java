package io.imunity.test.ui.home;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import io.imunity.test.ui.VaadinPage;
import io.imunity.test.ui.flows.EmailWorkflowTestBase;

@Tag("home")
class VerifiedEmailTest extends EmailWorkflowTestBase
{
	private static final String ATTRIBUTE = "[data-testid='ProfileView.attribute']"
			+ "[data-attribute-name='" + EMAIL_ATTRIBUTE + "']";
	private static final By EMAILS = By.cssSelector(ATTRIBUTE + " input");

	@Test
	void shouldAddConfirmAndRemoveEmailWhilePreservingRequiredValue() throws Exception
	{
		TestUser user = emailOwner();
		deployHomeAt("/email-home", "unity.userhome.attributes.1.attribute=" + EMAIL_ATTRIBUTE + "\n"
				+ "unity.userhome.attributes.1.group=/\nunity.userhome.attributes.1.editable=true\n");
		VaadinPage page = new VaadinPage(driver);
		page.login(baseUrl, "/email-home", user.username(), user.password());
		driver.get(baseUrl + "/email-home/profile");
		page.visible(EMAILS);
		assertThat(driver.findElements(removeButtons())).filteredOn(element -> element.isDisplayed()).isEmpty();
		page.click(By.cssSelector(ATTRIBUTE + " vaadin-icon[icon='vaadin:plus-circle-o']"));
		waitFor(() -> driver.findElements(EMAILS).size() == 2);
		driver.findElements(EMAILS).getLast().sendKeys(NEW_EMAIL, org.openqa.selenium.Keys.TAB);
		page.click(testId("ProfileView.save"));
		waitForElementNotPresent(testId("ProfileView.save"));
		assertThat(emails(user)).anyMatch(email -> NEW_EMAIL.equals(email.getValue())
				&& !email.getConfirmationInfo().isConfirmed());

		WebDriver confirmationBrowser = newBrowser();
		confirmationBrowser.get(confirmationLink());
		assertThat(new VaadinPage(confirmationBrowser).visible(By.className("u-final-info")).getText())
				.contains("Email confirmed");
		assertThat(emails(user)).hasSize(2).allMatch(email -> email.getConfirmationInfo().isConfirmed());
		driver.navigate().refresh();
		page.visible(EMAILS);
		assertThat(driver.findElements(EMAILS)).extracting(element -> element.getDomProperty("value"))
				.containsExactly(ORIGINAL_EMAIL, NEW_EMAIL);
		driver.findElements(removeButtons()).stream().filter(element -> element.isDisplayed())
				.toList().getLast().click();
		page.click(testId("ProfileView.save"));
		waitForElementNotPresent(testId("ProfileView.save"));
		driver.navigate().refresh();
		page.visible(EMAILS);
		assertThat(driver.findElements(EMAILS)).singleElement()
				.satisfies(input -> assertThat(input.getDomProperty("value")).isEqualTo(ORIGINAL_EMAIL));
		assertThat(driver.findElements(removeButtons())).filteredOn(element -> element.isDisplayed()).isEmpty();
		assertThat(emails(user)).singleElement().satisfies(email ->
		{
			assertThat(email.getValue()).isEqualTo(ORIGINAL_EMAIL);
			assertThat(email.getConfirmationInfo().isConfirmed()).isTrue();
		});
	}

	private By removeButtons()
	{
		return By.cssSelector(ATTRIBUTE + " vaadin-icon[icon='vaadin:trash']");
	}
}
