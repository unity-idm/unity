/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui;

import static org.assertj.core.api.Assertions.assertThat;

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
	public void shouldRotateSessionOnLoginAndDenyProtectedAccessAfterLogout()
	{
		driver.get(baseUrl + "/home");
		waitForPageLoadByURL("/home/authentication");
		waitForPageLoad(By.className("u-passwordSignInButton"));
		
		Cookie sessionBefore = driver.manage().getCookieNamed("JSESSIONID");
		driver.findElement(By.className("u-passwordUsernameField")).sendKeys("a");
		driver.findElement(By.className("u-passwordField")).sendKeys("a");
		driver.findElement(By.className("u-passwordSignInButton")).click();

		waitForPageLoad(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"));
		assertThat(driver.findElement(By.id("MainHeader.loggedAs")).getText()).contains("Default Administrator");
		Cookie sessionAfter = driver.manage().getCookieNamed("JSESSIONID");
		assertThat(sessionAfter.getValue()).isNotEqualTo(sessionBefore.getValue());
		waitForElement(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']")).click();
		waitForPageLoad(By.className("u-passwordSignInButton"));
		driver.get(baseUrl + "/home/sign-in");
		waitForPageLoadByURL("/home/authentication");
		waitForPageLoad(By.className("u-passwordUsernameField"));
		assertThat(driver.findElements(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"))).isEmpty();
		assertThat(driver.findElements(By.id("MainHeader.loggedAs"))).isEmpty();
	}
}
