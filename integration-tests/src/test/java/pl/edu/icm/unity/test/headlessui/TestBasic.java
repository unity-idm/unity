/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;


/**
 * 
 * @author K. Benedyczak
 */
public class TestBasic extends SeleniumTestBase
{
	@Test
	public void loginTest()
	{
		driver.get(baseUrl + "/home");
		waitForPageLoad(By.className("u-passwordSignInButton"));
		
		Cookie sessionBefore = driver.manage().getCookieNamed("JSESSIONID");
		driver.findElement(By.className("u-passwordUsernameField")).sendKeys("a");
		driver.findElement(By.className("u-passwordField")).sendKeys("a");
		driver.findElement(By.className("u-passwordSignInButton")).click();

		waitForPageLoad(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"));
		assertTrue(driver.findElement(By.id("MainHeader.loggedAs")).getText().contains("Default Administrator"));
		driver.findElement(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"));
		Cookie sessionAfter = driver.manage().getCookieNamed("JSESSIONID");
		assertNotEquals(sessionBefore.getValue(), sessionAfter.getValue());
		waitForElement(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']")).click();
	}
}
