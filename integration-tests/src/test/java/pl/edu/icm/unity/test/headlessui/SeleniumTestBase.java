/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.server.JettyServer;

/**
 * This is a base class for Selenium WebDriver based headless Web UI testing.
 * <p>
 * Provides: startup of the test server, setup of a web driver and its cleanup.
 * What's more utility methods are provided so Selenium programming is more concise.
 * 
 * @author K. Benedyczak
 */
@ExtendWith(SpringExtension.class)
@UnityIntegrationTest
@TestPropertySource(properties = { "unityConfig: src/test/resources/selenium/unityServer.conf" })
@ExtendWith(pl.edu.icm.unity.test.headlessui.SeleniumTestBase.SeleniumTestWatcher.class)
public abstract class SeleniumTestBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SeleniumTestBase.class);
	protected String baseUrl = "https://localhost:2443";
	public static final int WAIT_TIME_S = Integer.parseInt(
			System.getProperty("unity.selenium.wait", "30"));
	public static final int SLEEP_TIME_MS = 100;
	public static final int SIMPLE_WAIT_TIME_MS = Integer.parseInt(
			System.getProperty("unity.selenium.delay", "1500"));
	protected WebDriver driver;

	@Autowired
	protected JettyServer httpServer;
	
	public JettyServer getHttpServer()
	{
		return httpServer;
	}

	@BeforeEach
	public void setUp() throws Exception
	{
		httpServer.start();
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments("window-size=1280,1024", "no-sandbox", "force-device-scale-factor=1", "ignore-certificate-errors", "--remote-allow-origins=*");
		chromeOptions.setAcceptInsecureCerts(true);
		String seleniumOpts = System.getProperty("unity.selenium.opts");
		if (seleniumOpts != null && !seleniumOpts.isEmpty())
		{
			String[] opts = seleniumOpts.split(",");
			log.info("Using additional Selenium options: {}", Arrays.toString(opts));
			chromeOptions.addArguments(opts);
		}
		try
		{
			driver = new ChromeDriver(chromeOptions);
		} catch (Throwable t)
		{
			log.error("Failed to create ChromeDriver: {}", t.toString(), t);
			throw t;
		}
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(WAIT_TIME_S));
	}	
	
	public WebDriver getDriver()
	{
		return driver;
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
				fail("timeout");
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

	protected void waitForElementNotPresent(By by)
	{
		waitFor(() -> isElementPresent(by) == null);
	}
	
	protected WebElement waitForPageLoad(By someElement)
	{
		WebElement ret = waitForElement(someElement);
		simpleWait();
		return ret;
	}

	protected void waitForPageLoadByURL(String urlSuffix)
	{
		waitFor(() -> driver.getCurrentUrl().endsWith(urlSuffix)); 
		simpleWait();
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
	
	public static class SeleniumTestWatcher implements TestWatcher
	{
		
		public SeleniumTestWatcher()
		{
		}
		
		public  void testSuccessful(org.junit.jupiter.api.extension.ExtensionContext context)
		{
			cleanup(context);	
		};

		public void testFailed(org.junit.jupiter.api.extension.ExtensionContext context, Throwable cause)
		{
			SeleniumTestBase requiredTestInstance = (SeleniumTestBase) context.getRequiredTestInstance();
			try
			{
				takeScreenshot(context.getClass().getName() + "-" + context.getTestMethod().get().getName(), requiredTestInstance.getDriver());
			} finally
			{
				
				cleanup(context);
			}
		};
	
		private void takeScreenshot(String suffix, WebDriver driver)
		{
			byte[] screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.BYTES);
			try
			{
				OutputStream fos = new FileOutputStream(
						new File("target/failshot-" + suffix + ".png"));
				IOUtils.write(screenshot, fos);
			} catch (Exception e1)
			{
				throw new RuntimeException("Can not take screenshot", e1);
			}
		}

		private void cleanup(org.junit.jupiter.api.extension.ExtensionContext context)
		{
			SeleniumTestBase testInstance = (SeleniumTestBase) context.getRequiredTestInstance();
			WebDriver driver = testInstance.getDriver();
			driver.manage().deleteAllCookies();
			testInstance.getHttpServer().stop();
			driver.quit();
		}
	};
}
