/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui.reg;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openqa.selenium.By;

import pl.edu.icm.unity.test.headlessui.SeleniumTestBase;

/**
 * 
 * @author P. Piernik
 */
public class TestRegistrationForm extends SeleniumTestBase
{
	@Test
	public void registrationTest() throws Exception
	{
		driver.get(baseUrl + "/admin/admin");
		waitForPageLoad(By.className("idpentry_samlWeb.5")).click();
		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		waitForElement(By.id("AuthenticationUI.username")).clear();
		waitForElement(By.id("AuthenticationUI.username")).sendKeys("demo-user");
		waitForElement(By.id("WebPasswordRetrieval.password")).clear();
		waitForElement(By.id("WebPasswordRetrieval.password")).sendKeys("the!test12");
		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();
		
		waitForPageLoad(By.id("UnknownUserDialog.register")).click();
		assertTrue(waitForPageLoad(By.id("EmailValueEditor.Email")).getAttribute("value")
				.contains("x"));
		waitForElement(By.id("EmailValueEditor.Email")).clear();
		waitForElement(By.id("EmailValueEditor.Email")).sendKeys("test@test.com");
		waitForElement(By.id("AbstractDialog.confirm")).click();	

		waitForPageLoad(By.className("v-Notification")).click();	
		
		waitForPageLoad(By.id("AuthenticationUI.authnenticateButton")).click();
		
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();
		assertTrue(waitForElement(By.id("MainHeader.loggedAs")) != null);
		waitForElement(By.id("MainHeader.logout"));			
	}	
}
