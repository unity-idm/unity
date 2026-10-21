/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.headlessui.reg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import io.imunity.test.ui.VaadinPage;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.test.headlessui.SeleniumTestBase;

public class TestAccountAssociation extends SeleniumTestBase
{
	@Autowired
	@Qualifier("insecure")
	private EntityManagement entities;
	@Autowired
	@Qualifier("insecure")
	private AttributesManagement attributes;

	@Test
	public void shouldPersistLinkedIdentityAndAuthenticateThroughLinkedProvider() throws Exception
	{
		EntityParam account = new EntityParam(new IdentityTaV("userName", "demo-user2"));
		long accountId = entities.getEntity(account).getId();
		List<IdentityTaV> originalIdentities = identities(account).stream()
				.map(identity -> new IdentityTaV(identity.getTypeId(), identity.getValue())).toList();
		String displayName = "Association Selenium member";
		attributes.setAttribute(account, StringAttribute.of("name", "/", displayName));

		new VaadinPage(driver).login(baseUrl, "/home", "demo-user2", "the!test2");
		associateAccount();
		List<Identity> linked = identities(account).stream().filter(identity -> !originalIdentities
				.contains(new IdentityTaV(identity.getTypeId(), identity.getValue()))).toList();
		assertThat(linked).singleElement().satisfies(identity ->
		{
			assertThat(identity.getTypeId()).isEqualTo("userName");
			assertThat(identity.getRemoteIdp()).isEqualTo("http://example-saml-idp.org");
			assertThat(identity.getEntityId()).isEqualTo(accountId);
		});

		waitForElement(By.cssSelector("vaadin-icon[icon='vaadin:sign-out']")).click();
		assertLinkedLogin(displayName);
		Identity identity = linked.getFirst();
		assertThat(entities.getEntity(new EntityParam(new IdentityTaV(identity.getTypeId(), identity.getValue())))
				.getId()).isEqualTo(accountId);
		assertThat(identities(account)).hasSize(originalIdentities.size() + 1);
	}

	private void assertLinkedLogin(String displayName)
	{
		WebDriver freshBrowser = newBrowser();
		VaadinPage freshPage = new VaadinPage(freshBrowser);
		freshBrowser.get(baseUrl + "/home2");
		freshPage.click(By.className("u-idpAuthentication-saml-single-5"));
		freshPage.signIn("demo-user2", "the!test2");
		freshPage.click(By.id("IdpButtonsBar.confirmButton"));
		assertThat(freshPage.visible(By.id("MainHeader.loggedAs")).getText()).contains(displayName);
	}

	private void associateAccount()
	{
		waitForPageLoad(By.id("EntityDetailsWithActions.associateAccount")).click();
		String mainWindow = driver.getWindowHandle();
		driver.switchTo().window(waitForPopup());
		waitForElement(By.className("u-idpAuthentication-saml-single-5")).click();
		// The sandbox shares the source realm, so the IdP reuses the current login session.
		waitForPageLoad(By.id("IdpButtonsBar.confirmButton")).click();
		driver.switchTo().window(mainWindow);
		WebElement finishButton = waitForElement(By.id("Wizard.finish"));
		finishButton.click();
		waitForElementNotPresent(By.id("Wizard.finish"));
	}

	private List<Identity> identities(EntityParam account) throws EngineException
	{
		return entities.getEntity(account).getIdentities();
	}
	
	private String waitForPopup()
	{
		String mainWindow = driver.getWindowHandle();
		waitForElement(By.id("Wizard.next")).click();
		waitFor(() -> driver.getWindowHandles().size() > 1);
		for (String window : driver.getWindowHandles())
		{
			if (!window.equals(mainWindow))
				return window;
		}
		throw new IllegalStateException("Popup closed immediately");
	}
}
