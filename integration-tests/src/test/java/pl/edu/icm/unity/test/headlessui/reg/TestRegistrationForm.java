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
		waitForElement(By.className("idpentry_samlWeb_remoteIdp.5.")).click();;
		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		waitForElement(By.id("AuthenticationUI.username")).clear();
		waitForElement(By.id("AuthenticationUI.username")).sendKeys("demo-user");
		waitForElement(By.id("WebPasswordRetrieval.password")).clear();
		waitForElement(By.id("WebPasswordRetrieval.password")).sendKeys("the!test1");
		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		waitForElement(By.id("IdpButtonsBar.confirmButton")).click();
		waitForElement(By.id("UnknownUserDialog.register")).click();
		assertTrue(waitForElement(By.id("EmailValueEditor.Email")).getAttribute("value")
				.contains("x"));
		waitForElement(By.id("EmailValueEditor.Email")).clear();
		waitForElement(By.id("EmailValueEditor.Email")).sendKeys("test@test.com");
		assertTrue(waitForElement(By.id("ListOfElements")) != null);
		waitForElement(By.id("AbstractDialog.confirm")).click();	
		//wait
		simpleWait(1000);
		waitForElement(By.className("v-Notification")).click();	

		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		waitForElement(By.id("IdpButtonsBar.confirmButton")).click();
		assertTrue(waitForElement(By.id("MainHeader.loggedAs")) != null);
		waitForElement(By.id("MainHeader.logout"));			
	}	
}
