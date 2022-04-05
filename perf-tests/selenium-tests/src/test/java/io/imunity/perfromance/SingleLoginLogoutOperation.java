/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.perfromance;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleLoginLogoutOperation implements PerformanceTestRunnable
{
	private static final Logger LOG = LoggerFactory.getLogger(SingleLoginLogoutOperation.class);
	
	private static final int WAIT_TIME_S = Integer.parseInt(System.getProperty("unity.selenium.wait", "30"));
	private static final int SLEEP_TIME_MS = 100;
	private static final int SIMPLE_WAIT_TIME_MS = Integer.parseInt(System.getProperty("unity.selenium.delay", "500"));
	
	private final RestAdminHttpClient adminClient;
	private final String baseUrl;
	private final String userName;

	private WebDriver driver;

	
	public SingleLoginLogoutOperation(int index, PerformanceTestConfig config)
	{
		this.userName = "perf-user-" + index;
		this.baseUrl = config.unityBaseURL;
		adminClient = new RestAdminHttpClient(config);
	}

	@Override
	public void beforeRun()
	{
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments("window-size=1280,1024", "no-sandbox", "force-device-scale-factor=1",
				"allow-insecure-localhost");
		String seleniumOpts = System.getProperty("unity.selenium.opts");
		if (seleniumOpts != null && !seleniumOpts.isEmpty())
		{
			String[] opts = seleniumOpts.split(",");
			LOG.info("Using additional Selenium options: {}", Arrays.toString(opts));
			chromeOptions.addArguments(opts);
		}
		driver = new ChromeDriver(chromeOptions);
		driver.manage().timeouts().implicitlyWait(WAIT_TIME_S, TimeUnit.SECONDS);
	}

	@Override
	public void run()
	{
		driver.get(baseUrl + "/home");
		waitForPageLoad(By.className("u-passwordSignInButton"));
		
		Cookie sessionBefore = driver.manage().getCookieNamed("JSESSIONID");
		
		driver.findElement(By.className("u-idpAuthentication-oauth-local")).click();
		
		waitForPageLoad(By.xpath("//*[contains(text(), 'Cancel authentication')]"));
		
		driver.findElement(By.className("u-passwordUsernameField")).clear();
		driver.findElement(By.className("u-passwordUsernameField")).sendKeys(userName);
		driver.findElement(By.className("u-passwordField")).clear();
		driver.findElement(By.className("u-passwordField")).sendKeys("the!test12");
		driver.findElement(By.className("u-passwordSignInButton")).click();
		
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton"));
		driver.findElement(By.id("IdpButtonsBar.confirmButton")).click();
		
		waitForPageLoad(By.id("MainHeader.logout"));
		
		assertTrue(driver.findElement(By.id("MainHeader.loggedAs")).getText().contains("user id:"));
		driver.findElement(By.id("MainHeader.logout"));
		Cookie sessionAfter = driver.manage().getCookieNamed("JSESSIONID");
		assertNotEquals(sessionBefore.getValue(), sessionAfter.getValue());
		waitForElement(By.id("MainHeader.logout")).click();
		adminClient.invalidateSession(userName);
		driver.manage().deleteAllCookies();
	}

	@Override
	public void afterRun()
	{
		driver.quit();
	}
	
	private WebElement waitForPageLoad(By someElement)
	{
		WebElement ret = waitForElement(someElement);
		simpleWait();
		return ret;
	}
	
	protected WebElement waitForElement(By by)
	{
		waitFor(() -> isElementPresent(by) != null);
		return isElementPresent(by);
	}
	
	protected void waitFor(Supplier<Boolean> awaited)
	{
		for (int i = 0;; i++)
		{
			if (i >= WAIT_TIME_S*1000/SLEEP_TIME_MS)
				Assert.fail("timeout");
			try
			{
				if (awaited.get())
					return;
				Thread.sleep(SLEEP_TIME_MS);
			} catch (InterruptedException e)
			{
				//OK
			}
		}
	}

	protected WebElement isElementPresent(By by)
	{
		try
		{
			WebElement ret = driver.findElement(by);
			if (ret == null || !ret.isDisplayed())
				return null;
			return ret;
		} catch (NoSuchElementException e)
		{
			return null;
		}
	}
	
	private void simpleWait()
	{
		try
		{
			Thread.sleep(SIMPLE_WAIT_TIME_MS);
		} catch (InterruptedException e)
		{
			//OK
		}
	}
	
	@Override
	public String takeScreenshot(String suffix)
	{
		byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
		try
		{
			String screenshotName = "target/failshot-" + userName + "-iter-" + suffix + ".png";
			OutputStream fos = new FileOutputStream(new File(screenshotName));
			IOUtils.write(screenshot, fos);
			return screenshotName;
		} catch (Exception e1)
		{
			throw new RuntimeException("Can not take screenshot", e1);
		}
	}

}
