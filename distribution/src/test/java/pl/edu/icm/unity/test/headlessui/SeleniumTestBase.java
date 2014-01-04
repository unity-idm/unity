/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import pl.edu.icm.unity.server.UnityApplication;

/**
 * This is a base class for Selenium WebDriver based headless Web UI testing.
 * <p>
 * Provides: startup of the test server, setup of a web driver and its cleanup.
 * What's more utility methods are provided so Selenium programming is more concise.
 * 
 * @author K. Benedyczak
 */
public class SeleniumTestBase
{
	public static final int WAIT_TIME_S = 5;
	protected WebDriver driver;

	private StringBuffer verificationErrors = new StringBuffer();

	@Before
	public void setUp() throws Exception
	{
		FileUtils.deleteDirectory(new File("target/data"));
		UnityApplication.main(new String[] {"src/test/resources/unityServer.conf"});
		driver = new FirefoxDriver();
		driver.manage().timeouts().implicitlyWait(WAIT_TIME_S, TimeUnit.SECONDS);
	}

	@After
	public void tearDown() throws Exception
	{
		driver.quit();
		String verificationErrorString = verificationErrors.toString();
		if (!"".equals(verificationErrorString))
		{
			Assert.fail(verificationErrorString);
		}
	}

	protected void waitForElement(By by)
	{
		for (int second = 0;; second++)
		{
			if (second >= WAIT_TIME_S)
				Assert.fail("timeout");
			try
			{
				if (isElementPresent(by))
					break;
				Thread.sleep(250);
			} catch (InterruptedException e)
			{
				//OK
			}
		}
	}
	
	protected boolean isElementPresent(By by)
	{
		try
		{
			driver.findElement(by);
			return true;
		} catch (NoSuchElementException e)
		{
			return false;
		}
	}
}
