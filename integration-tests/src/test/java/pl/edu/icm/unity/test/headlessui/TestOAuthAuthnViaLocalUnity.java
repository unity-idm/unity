/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.test.headlessui;

import org.junit.Test;
import org.openqa.selenium.By;

import static org.junit.Assert.assertNotNull;

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
		driver.get(baseUrl + "/home");
		waitForPageLoad(By.className("u-idpAuthentication-oauth-local")).click();	
		
		waitForPageLoadByURL("/oauth2-as/authentication");
		waitForElement(By.className("u-passwordUsernameField")).sendKeys("demo-user");
		waitForElement(By.className("u-passwordField")).sendKeys("the!test12");
		waitForElement(By.className("u-passwordSignInButton")).click();
		
		waitForElement(By.id("ExposedAttributes.showDetails")).click();
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();	
		
		waitForPageLoad(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"));
		assertNotNull(waitForElement(By.id("MainHeader.loggedAs")));
		waitForElement(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']")).click();
	}
}
