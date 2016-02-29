/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui.reg;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import pl.edu.icm.unity.test.headlessui.SeleniumTestBase;

/**
 * 
 * @author K. Benedyczak
 */
public class TestAccountAssociation extends SeleniumTestBase
{
	@Test
	public void registrationTest() throws Exception
	{
		//login to home UI 
		driver.get(baseUrl + "/home/home");
		waitForElement(By.id("AuthenticationUI.username")).clear();
		waitForElement(By.id("AuthenticationUI.username")).sendKeys("demo-user2");
		waitForElement(By.id("WebPasswordRetrieval.password")).clear();
		waitForElement(By.id("WebPasswordRetrieval.password")).sendKeys("the!test2");
		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		
		//invoke association
		waitForElement(By.id("EntityDetailsWithActions.associateAccount")).click();
		
		//wizard -> invoke sandbox login
		String cwh = driver.getWindowHandle();
		String popupH = waitForPopup();
		driver.switchTo().window(popupH);

		//login to Unity over loopback SAML in the sandbox
		waitForElement(By.className("idpentry_samlWeb_remoteIdp.5.")).click();
		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		
		//we don't have to login as both are in the same realm so we are instantly SSO-logged-in
//		waitForElement(By.id("AuthenticationUI.username")).clear();
//		waitForElement(By.id("AuthenticationUI.username")).sendKeys("demo-user");
//		waitForElement(By.id("WebPasswordRetrieval.password")).clear();
//		waitForElement(By.id("WebPasswordRetrieval.password")).sendKeys("the!test1");
//		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		waitForElement(By.id("IdpButtonsBar.confirmButton")).click();
		
		//go back to the main window and complete wizard
		driver.switchTo().window(cwh);
		WebElement finishB = waitForElement(By.id("SandboxWizard.finish"));
		Assert.assertTrue(finishB.isEnabled());
		simpleWait();
		finishB.click();
		simpleWait();
		waitForElement(By.className("success")).click();
		waitForElement(By.id("MainHeader.logout")).click();
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
