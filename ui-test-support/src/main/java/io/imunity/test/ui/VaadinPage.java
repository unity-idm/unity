package io.imunity.test.ui;

import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

public final class VaadinPage
{
	private final WebDriver driver;
	private final WebDriverWait wait;

	public VaadinPage(WebDriver driver)
	{
		this.driver = driver;
		wait = new WebDriverWait(driver, Duration.ofSeconds(SeleniumTestBase.WAIT_TIME_S));
	}

	public static By testId(String... components)
	{
		return By.cssSelector(Arrays.stream(components)
				.map(component -> "[data-testid='" + component + "']").collect(Collectors.joining(" ")));
	}

	public WebElement visible(By locator)
	{
		return wait.ignoring(StaleElementReferenceException.class).until(ignored -> driver.findElements(locator)
				.stream().filter(WebElement::isDisplayed).findFirst().orElse(null));
	}

	public void click(By locator)
	{
		wait.ignoring(ElementClickInterceptedException.class).ignoring(StaleElementReferenceException.class)
				.until(ignored ->
				{
					WebElement element = driver.findElements(locator).stream()
							.filter(candidate -> candidate.isDisplayed() && candidate.isEnabled()).findFirst().orElse(null);
					if (element == null)
						return false;
					element.click();
					return true;
				});
	}

	public void fill(By locator, String value)
	{
		WebElement input = input(locator);
		input.sendKeys(Keys.chord(Keys.CONTROL, "a"), value, Keys.TAB);
	}

	public void select(By locator, String value)
	{
		WebElement input = input(locator);
		input.sendKeys(Keys.chord(Keys.CONTROL, "a"), value);
		WebElement option = wait.until(ignored -> visible(locator).findElements(By.cssSelector("vaadin-combo-box-item"))
				.stream().filter(item -> item.isDisplayed() && value.equals(item.getText())).findFirst().orElse(null));
		option.click();
		input.sendKeys(Keys.TAB);
		wait.until(ignored -> value.equals(value(locator))
				&& !Boolean.parseBoolean(visible(locator).getDomProperty("opened")));
	}

	public void check(By locator, boolean checked)
	{
		if (Boolean.parseBoolean(visible(locator).getDomProperty("checked")) != checked)
			click(locator);
		wait.until(ignored -> Boolean.parseBoolean(visible(locator).getDomProperty("checked")) == checked);
	}

	public String value(By locator)
	{
		return input(locator).getDomProperty("value");
	}

	public void login(String baseUrl, String path, String username, String password)
	{
		driver.get(baseUrl + path);
		signIn(username, password);
		visible(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"));
	}

	public void signIn(String username, String password)
	{
		fill(By.className("u-passwordUsernameField"), username);
		fill(By.className("u-passwordField"), password);
		click(By.className("u-passwordSignInButton"));
	}

	private WebElement input(By locator)
	{
		return wait.until(ignored ->
		{
			WebElement host = visible(locator);
			WebElement input = host.getTagName().equals("input") || host.getTagName().equals("textarea")
					? host : host.findElement(By.cssSelector("input, textarea"));
			return input.isDisplayed() && input.isEnabled() ? input : null;
		});
	}
}
