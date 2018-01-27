/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServerSelenium.conf" })
public abstract class SeleniumTestBase
{
	protected String baseUrl = "https://localhost:2443";
	public static final int WAIT_TIME_S = Integer.parseInt(
			System.getProperty("unity.selenium.wait", "30"));
	public static final int SLEEP_TIME_MS = 100;
	public static final int SIMPLE_WAIT_TIME_MS = Integer.parseInt(
			System.getProperty("unity.selenium.delay", "1500"));
	protected WebDriver driver;

	private StringBuffer verificationErrors = new StringBuffer();
	@Autowired
	protected JettyServer httpServer;
	
	@Before
	public void setUp() throws Exception
	{
		httpServer.start();
		driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(WAIT_TIME_S, TimeUnit.SECONDS);
	}

	
	@Rule
	public TestRule watchman = new TestWatcher() 
	{
		@Override
		protected void failed(Throwable e, Description description) 
		{
			takeScreenshot(description.getClassName() + "-" + description.getMethodName());
			cleanup();
		}
		
		@Override
		protected void succeeded(Description description) 
		{
			cleanup();
		}

		private void takeScreenshot(String suffix)
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

		private void cleanup()
		{
			driver.manage().deleteAllCookies();
			httpServer.stop();
			driver.quit();
			String verificationErrorString = verificationErrors.toString();
			if (!"".equals(verificationErrorString))
			{
				Assert.fail(verificationErrorString);
			}
		}
	};

	
	
	protected WebElement waitForElement(By by)
	{
		for (int i = 0;; i++)
		{
			if (i >= WAIT_TIME_S*1000/SLEEP_TIME_MS)
				Assert.fail("timeout");
			try
			{
				WebElement elementPresent = isElementPresent(by);
				if (elementPresent != null)
					return elementPresent;
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
	
	protected WebElement waitForPageLoad(By someElement)
	{
		WebElement ret = waitForElement(someElement);
		simpleWait();
		return ret;
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
}
