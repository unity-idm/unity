/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui;

import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

/**
 * The test is ignored as it is long lasting stress test. Requires manual intervention:
 *  - configuration of automatic users creation via profile
 *  - enable skip consent on SAML IDP endpoint
 *  - first interactive login to google
 * @author K. Benedyczak
 */
public class TestGoogleAuthnViaSAMLIdP extends SeleniumTestBase
{
	@Ignore
	@Test
	public void loginTest() throws Exception
	{
		driver.get(baseUrl + "/admin/admin");
		for (int i=0; i<1000; i++)
		{
			try
			{
				singleLoop();
			} catch (Exception e)
			{
				e.printStackTrace();
				Thread.sleep(60000*60*10);
			}
		}
	}
	
	private void singleLoop() throws InterruptedException
	{
		waitForElement(By.xpath("//*[contains(text(), 'Login to UNITY admin')]"));
		waitForElement(By.className("idpentry_saml_remoteIdp.7.")).click();
		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		
		waitForElement(By.xpath("//*[contains(text(), 'Login to UNITY SAML web authentication')]"));
		waitForElement(By.xpath("//*[contains(text(), 'Google Account')]")).click();
		waitForElement(By.id("AuthenticationUI.authnenticateButton")).click();
		
		driver.findElement(By.id("MainHeader.logout"));
		Thread.sleep(500);
		waitForElement(By.id("MainHeader.logout")).click();
	}
}
