/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.server.JettyServer;

/**
 * This is a base class for Selenium WebDriver based headless Web UI testing.
 * <p>
 * Provides: startup of the test server, setup of a web driver and its cleanup.
 * What's more utility methods are provided so Selenium programming is more concise.
 * 
 * @author K. Benedyczak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath*:META-INF/components.xml", "classpath:META-INF/test-selenium.xml"})
@ActiveProfiles("test")
public class SeleniumTestBase
{
	protected String baseUrl = "https://localhost:2443";
	public static final int WAIT_TIME_S = 450;
	public static final int SLEEP_TIME_MS = 250;
	public static final int SIMPLE_WAIT_TIME_MS = Integer.parseInt(
			System.getProperty("unity.selenium.delay", "2000"));
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

	@After
	public void tearDown() throws Exception
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
	
	protected void simpleWait()
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
