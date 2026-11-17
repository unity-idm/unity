package io.imunity.test.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.server.JettyServer;

@ExtendWith({SpringExtension.class, SeleniumLifecycle.class})
@ContextConfiguration(locations = "classpath*:META-INF/components.xml", initializers = UiFixtureInitializer.class)
@ActiveProfiles(UnityServerConfiguration.PROFILE_TEST)
@TestPropertySource(properties = "unityConfig=target/ui-fixtures/unityServer.conf")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class SeleniumTestBase
{
	public static final int WAIT_TIME_S = Integer.getInteger("unity.selenium.wait", 30);
	protected final String baseUrl = "https://localhost:2443";
	protected WebDriver driver;

	@Autowired
	protected JettyServer httpServer;

	private final List<WebDriver> browsers = new ArrayList<>();
	ReviewScreenshots reviewScreenshots;

	@BeforeEach
	public void startBrowser() throws Exception
	{
		httpServer.start();
		driver = newBrowser();
	}

	protected final WebDriver newBrowser()
	{
		ChromeOptions options = new ChromeOptions();
		options.addArguments("--window-size=1280,1024", "--no-sandbox", "--force-device-scale-factor=1",
				"--ignore-certificate-errors", "--lang=en-US");
		options.setAcceptInsecureCerts(true);
		String extraOptions = System.getProperty("unity.selenium.opts", "");
		if (!extraOptions.isBlank())
			options.addArguments(extraOptions.split(","));
		LoggingPreferences logging = new LoggingPreferences();
		logging.enable(LogType.BROWSER, java.util.logging.Level.ALL);
		options.setCapability("goog:loggingPrefs", logging);
		WebDriver browser = createDriver(options);
		browsers.add(browser);
		browser.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(WAIT_TIME_S));
		browser.manage().timeouts().scriptTimeout(Duration.ofSeconds(WAIT_TIME_S));
		return browser;
	}

	WebDriver createDriver(ChromeOptions options)
	{
		return new ChromeDriver(options);
	}

	protected final WebElement waitForElement(By locator)
	{
		return new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME_S))
				.until(ExpectedConditions.visibilityOfElementLocated(locator));
	}

	protected final void waitFor(Supplier<Boolean> condition)
	{
		new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME_S))
				.ignoring(StaleElementReferenceException.class)
				.until(ignored -> condition.get());
	}

	protected final WebElement isElementPresent(By locator)
	{
		try
		{
			WebElement element = driver.findElement(locator);
			return element.isDisplayed() ? element : null;
		} catch (NoSuchElementException | StaleElementReferenceException e)
		{
			return null;
		}
	}

	protected final void waitForElementNotPresent(By locator)
	{
		new WebDriverWait(driver, Duration.ofSeconds(WAIT_TIME_S))
				.until(ExpectedConditions.invisibilityOfElementLocated(locator));
	}

	protected final WebElement waitForPageLoad(By locator)
	{
		waitForUi();
		waitForElement(locator);
		waitForUi();
		return waitForElement(locator);
	}

	protected final void waitForPageLoadByURL(String suffix)
	{
		waitFor(() -> driver.getCurrentUrl().endsWith(suffix));
		waitForUi();
	}

	protected final void waitForUi()
	{
		waitForUi(driver);
	}

	private void waitForUi(WebDriver browser)
	{
		// Flow exposes request activity outside the DOM; visibility alone can precede a server rerender.
		new WebDriverWait(browser, Duration.ofSeconds(WAIT_TIME_S))
				.ignoring(StaleElementReferenceException.class)
				.until(ignored -> Boolean.TRUE.equals(((JavascriptExecutor) browser).executeScript("""
				return document.readyState === 'complete' &&
					(!window.Vaadin || !Vaadin.Flow || !Vaadin.Flow.clients ||
					Object.values(Vaadin.Flow.clients).every(client => !client.isActive || !client.isActive()));
				""")));
	}

	protected final void screenshot(String checkpoint)
	{
		screenshot(driver, checkpoint);
	}

	protected final void screenshot(WebDriver browser, String checkpoint)
	{
		if (reviewScreenshots == null)
			return;
		waitForUi(browser);
		((JavascriptExecutor) browser).executeAsyncScript("""
				const done = arguments[arguments.length - 1];
				document.fonts.ready.then(() => requestAnimationFrame(() => requestAnimationFrame(() => done())));
				""");
		try
		{
			reviewScreenshots.capture(browser, checkpoint);
		} catch (IOException e)
		{
			throw new UncheckedIOException("Cannot capture review checkpoint: " + checkpoint, e);
		}
	}

	List<WebDriver> browsers()
	{
		return List.copyOf(browsers);
	}

	void closeResources() throws Exception
	{
		Exception failure = null;
		for (WebDriver browser : browsers)
		{
			try
			{
				browser.quit();
			} catch (Exception e)
			{
				if (failure == null)
					failure = e;
				else
					failure.addSuppressed(e);
			}
		}
		browsers.clear();
		try
		{
			if (httpServer != null)
				httpServer.stop();
		} catch (Exception e)
		{
			if (failure == null)
				failure = e;
			else
				failure.addSuppressed(e);
		}
		if (failure != null)
			throw failure;
	}
}
