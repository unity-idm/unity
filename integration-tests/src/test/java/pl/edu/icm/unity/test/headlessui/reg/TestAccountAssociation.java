/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui.reg;

import org.junit.Test;
import org.openqa.selenium.By;

import pl.edu.icm.unity.test.headlessui.SeleniumTestBase;

/**
 * 
 * @author K. Benedyczak
 */
public class TestAccountAssociation extends SeleniumTestBase
{
	@Test
	public void accountAssociationTest() throws Exception
	{
		//login to home UI 
		driver.get(baseUrl + "/home/home");
		waitForPageLoad(By.className("u-passwordUsernameField")).clear();
		waitForElement(By.className("u-passwordUsernameField")).sendKeys("demo-user2");
		waitForElement(By.className("u-passwordField")).clear();
		waitForElement(By.className("u-passwordField")).sendKeys("the!test2");
		waitForElement(By.className("u-passwordSignInButton")).click();
		
		//invoke association
		waitForPageLoad(By.id("EntityDetailsWithActions.associateAccount")).click();
		
		//wizard -> invoke sandbox login
		String cwh = driver.getWindowHandle();
		String popupH = waitForPopup();
		driver.switchTo().window(popupH);

		//login to Unity over loopback SAML in the sandbox
		waitForElement(By.className("u-idpAuthentication-saml-5")).click();
		
		//we don't have to login as both are in the same realm so we are instantly SSO-logged-in
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();
		
		//go back to the main window and complete wizard
		driver.switchTo().window(cwh);
		waitForPageLoad(By.id("SandboxWizard.finish")).click();
		waitForPageLoad(By.className("success")).click();
		waitForPageLoad(By.id("MainHeader.logout")).click();
	}
	
	private String waitForPopup() throws InterruptedException
	{
		String cwh = driver.getWindowHandle();
		waitForElement(By.id("SandboxWizard.next")).click();
		int i=0;
		while (driver.getWindowHandles().size() == 1 && i <= WAIT_TIME_S)
		{
			Thread.sleep(SLEEP_TIME_MS);
			i+=SLEEP_TIME_MS;
		}
		for (String h: driver.getWindowHandles())
		{
			if (!h.equals(cwh))
				return h;
		}
		throw new IllegalStateException("Popup closed immediatly");
	}
}
