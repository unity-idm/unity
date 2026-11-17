/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.test.headlessui;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;


/**
 * Test login via Local OAuth with output profile which create dynamic attribute
 * @author P.Piernik
 *
 */
public class TestOAuthAuthnViaLocalUnity extends SeleniumTestBase
{

	@Test
	public void shouldReleaseDynamicUsernameAndAuthenticateExpectedUser()
	{
		driver.get(baseUrl + "/home");
		waitForPageLoadByURL("/home/authentication");
		waitForPageLoad(By.className("u-idpAuthentication-oauth-local")).click();	
		
		waitForPageLoadByURL("/oauth2-as/authentication");
		waitForElement(By.className("u-passwordUsernameField")).sendKeys("demo-user");
		waitForElement(By.className("u-passwordField")).sendKeys("the!test12");
		waitForElement(By.className("u-passwordSignInButton")).click();
		
		waitForElement(By.id("ExposedAttributes.showDetails")).click();
		assertThat(waitForElement(By.xpath("//*[@id='ExposedAttributes.showDetails']"
				+ "//vaadin-text-field[label[normalize-space(.)='username']]/input")).getDomProperty("value"))
				.isEqualTo("demo-user");
		screenshot("OAuth consent with exposed attributes");
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();	
		
		waitForPageLoad(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']"));
		assertThat(waitForElement(By.id("MainHeader.loggedAs")).getText()).contains("Demo user");
		waitForElement(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']")).click();
	}
}
