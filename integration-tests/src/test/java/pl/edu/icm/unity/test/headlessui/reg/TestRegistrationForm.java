/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui.reg;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import pl.edu.icm.unity.test.headlessui.SeleniumTestBase;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class TestRegistrationForm extends SeleniumTestBase
{
	@Test
	public void registrationTest()
	{
		driver.get(baseUrl + "/home2");
		waitForPageLoad(By.className("u-idpAuthentication-saml-single-5")).click();
		
		waitForPageLoadByURL("/saml-idp/authentication");
		waitForElement(By.className("u-passwordUsernameField")).sendKeys("demo-user");
		waitForElement(By.className("u-passwordField")).sendKeys("the!test12");
		waitForElement(By.className("u-passwordSignInButton")).click();
		
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();
		
		waitForPageLoad(By.id("UnknownUserDialog.register")).click();
		assertTrue(waitForPageLoad(By.id("EmailValueEditor.Email")).getAttribute("value")
				.contains("x"));
		WebElement webElement = waitForElement(By.id("EmailValueEditor.Email"));
		webElement.findElement(By.tagName("input")).clear();
		webElement.sendKeys("test@test.com");
		waitForElement(By.tagName("vaadin-dialog-overlay")).findElement(By.cssSelector("vaadin-button[theme='primary']")).click();

		waitForPageLoad(By.className("u-idpAuthentication-saml-single-5")).click();
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();

		assertNotNull(waitForElement(By.id("MainHeader.loggedAs")));
	}
}
