/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
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
		waitForPageLoad(By.className("u-idpAuthentication-oauth-local")).click();	
		
		waitForPageLoadByURL("/oauth2-as/oauth2-authz-web-entry");
		waitForElement(By.className("u-passwordUsernameField")).clear();
		waitForElement(By.className("u-passwordUsernameField")).sendKeys("demo-user");
		waitForElement(By.className("u-passwordField")).clear();
		waitForElement(By.className("u-passwordField")).sendKeys("the!test12");
		waitForElement(By.className("u-passwordSignInButton")).click();
		
		waitForElement(By.id("ExposedAttributes.showDetails")).click();
		assertThat(waitForElement(By.xpath("//*[contains(text(), 'username')]")), notNullValue());	
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();	
		
		waitForPageLoad(By.id("MainHeader.logout"));
		assertTrue(waitForElement(By.id("MainHeader.loggedAs")) != null);
		waitForElement(By.id("MainHeader.logout")).click();
	}
}
