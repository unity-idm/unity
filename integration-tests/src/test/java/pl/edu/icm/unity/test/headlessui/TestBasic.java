/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;

/**
 * 
 * @author K. Benedyczak
 */
public class TestBasic extends SeleniumTestBase
{
	@Test
	public void loginTest() throws Exception
	{
		driver.get(baseUrl + "/admin/admin");
		waitForPageLoad(By.className("u-passwordSignInButton"));
		
		Cookie sessionBefore = driver.manage().getCookieNamed("JSESSIONID");
		driver.findElement(By.className("u-passwordUsernameField")).clear();
		driver.findElement(By.className("u-passwordUsernameField")).sendKeys("a");
		driver.findElement(By.className("u-passwordField")).clear();
		driver.findElement(By.className("u-passwordField")).sendKeys("a");
		driver.findElement(By.className("u-passwordSignInButton")).click();
		
		waitForPageLoad(By.id("MainHeader.logout"));
		assertTrue(driver.findElement(By.id("MainHeader.loggedAs")).getText().contains("Default Administrator"));
		driver.findElement(By.id("MainHeader.logout"));
		Cookie sessionAfter = driver.manage().getCookieNamed("JSESSIONID");
		assertNotEquals(sessionBefore.getValue(), sessionAfter.getValue());
		waitForElement(By.id("MainHeader.logout")).click();
	}
}
