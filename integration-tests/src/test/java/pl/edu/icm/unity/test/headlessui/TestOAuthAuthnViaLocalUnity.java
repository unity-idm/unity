/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.test.headlessui;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openqa.selenium.By;

/**
 * Test login via Local OAuth with output profile which create dynamic attribute
 * @author P.Piernik
 *
 */
public class TestOAuthAuthnViaLocalUnity extends SeleniumTestBase
{

	@Test
	public void testLoginWithDynamicAttributeOnConsentScreen()
	{
		driver.get(baseUrl + "/admin/admin");
		waitForPageLoad(By.className("idpentry_oauthWeb.local"));	
		driver.findElement(By.className("idpentry_oauthWeb.local")).click();
		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		waitForElement(By.id("AuthenticationUI.username")).clear();
		waitForElement(By.id("AuthenticationUI.username")).sendKeys("demo-user");
		waitForElement(By.id("WebPasswordRetrieval.password")).clear();
		waitForElement(By.id("WebPasswordRetrieval.password")).sendKeys("the!test12");
		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		
		waitForElement(By.id("ExposedAttributes.showDetails")).click();
		assertThat(waitForElement(By.xpath("//*[contains(text(), 'username')]")), notNullValue());	
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();	
		
		waitForPageLoad(By.id("MainHeader.logout"));
		assertTrue(waitForElement(By.id("MainHeader.loggedAs")) != null);
		waitForElement(By.id("MainHeader.logout")).click();
	}
}
