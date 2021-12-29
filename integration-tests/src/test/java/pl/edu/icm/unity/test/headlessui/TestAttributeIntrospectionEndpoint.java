/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.test.headlessui;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openqa.selenium.By;

public class TestAttributeIntrospectionEndpoint extends SeleniumTestBase
{
	@Test
	public void attributeInstrospectionTest() throws Exception
	{		
		driver.get(baseUrl + "/introspection");
		
		waitForElement(By.className("u-idpAuthentication-saml-7")).click();
		
		//saml login
		waitForPageLoad(By.className("u-passwordUsernameField")).clear();
		waitForElement(By.className("u-passwordUsernameField")).sendKeys("demo-user");
		waitForElement(By.className("u-passwordField")).clear();
		waitForElement(By.className("u-passwordField")).sendKeys("the!test12");
		waitForElement(By.className("u-passwordSignInButton")).click();
		
		//consent
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();

		//summary component
		assertTrue(waitForPageLoad(By.id("PolicyProcessingSummaryComponent.TryAgain")) != null);

		waitForElement(By.xpath("//*[contains(text(), 'Good')]"));
		waitForElement(By.xpath("//*[contains(text(), '50% (1/2) optional attributes were provided')]"));
		waitForElement(By.xpath("//*[contains(text(), 'All mandatory attributes were provided')]"));

		//waitForPageLoad(By.id("PolicyProcessingSummaryComponent.TryAgain")).click();
	}
}
