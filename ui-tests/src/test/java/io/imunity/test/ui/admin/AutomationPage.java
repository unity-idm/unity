package io.imunity.test.ui.admin;

import static io.imunity.test.ui.VaadinPage.testId;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.imunity.test.ui.SeleniumTestBase;
import io.imunity.test.ui.VaadinPage;

final class AutomationPage
{
	private final WebDriver driver;
	private final VaadinPage page;
	private final WebDriverWait wait;

	AutomationPage(WebDriver driver)
	{
		this.driver = driver;
		page = new VaadinPage(driver);
		wait = new WebDriverWait(driver, Duration.ofSeconds(SeleniumTestBase.WAIT_TIME_S));
	}

	void configure(String owner, String condition, String status)
	{
		page.fill(testId(owner, owner + ".condition"), condition);
		page.click(testId(owner, "ActionEditor.action"));
		WebElement action = wait.ignoring(StaleElementReferenceException.class).until(ignored -> driver
				.findElements(By.cssSelector("vaadin-select-item")).stream()
				.filter(item -> item.isDisplayed() && "changeStatus".equals(item.getText())).findFirst().orElse(null));
		action.click();
		page.select(parameter(owner, "status"), status);
	}

	void assertConfiguration(String owner, String condition, String status)
	{
		assertThat(page.value(testId(owner, owner + ".condition"))).isEqualTo(condition);
		assertThat(page.visible(testId(owner, "ActionEditor.action")).getText()).contains("changeStatus");
		assertThat(page.value(parameter(owner, "status"))).isEqualTo(status);
	}

	void schedule(String expression)
	{
		page.fill(testId("AutomationEditView", "AutomationEditView.schedule"), expression);
	}

	void assertSchedule(String expression)
	{
		assertThat(page.value(testId("AutomationEditView", "AutomationEditView.schedule"))).isEqualTo(expression);
	}

	void assertInvalidSchedule()
	{
		By schedule = testId("AutomationEditView", "AutomationEditView.schedule");
		wait.until(ignored -> Boolean.parseBoolean(page.visible(schedule).getDomProperty("invalid")));
		assertThat(page.visible(schedule).getDomProperty("errorMessage"))
				.contains("The cron expression is invalid");
		assertThat(driver.getCurrentUrl()).contains("/automation/edit");
	}

	void assertListed(String ruleId, String expression)
	{
		assertThat(new GridPage(driver).text("AutomationView.rules", "rule-" + ruleId)).contains(expression);
	}

	private static By parameter(String owner, String name)
	{
		return By.cssSelector("[data-testid='" + owner + "'] "
				+ "[data-testid='ActionEditor.parameter'][data-parameter-name='" + name + "']");
	}
}
