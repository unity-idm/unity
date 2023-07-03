/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.Test;

import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings.ConfirmationMode;

public class PasswordStrengthChangeTest
{
	@Test
	public void scoreIncreaseOutdatesCredential() throws Exception
	{
		LocalCredentialVerificator verificator = getMockPasswordVerificator();
		PasswordCredential credCfg = getEmpty();
		credCfg.setMinScore(10);
		verificator.setSerializedConfiguration(toCredConfig(credCfg));

		credCfg.setMinScore(11);
		boolean outdating = verificator.isCredentialDefinitionChagneOutdatingCredentials(toCredConfig(credCfg));
		
		assertThat(outdating, is(true));
	}

	@Test
	public void scoreDecreaseDoesntOutdatesCredential() throws Exception
	{
		LocalCredentialVerificator verificator = getMockPasswordVerificator();
		PasswordCredential credCfg = getEmpty();
		credCfg.setMinScore(10);
		verificator.setSerializedConfiguration(toCredConfig(credCfg));

		credCfg.setMinScore(9);
		boolean outdating = verificator.isCredentialDefinitionChagneOutdatingCredentials(toCredConfig(credCfg));
		
		assertThat(outdating, is(false));
	}
	
	@Test
	public void storageIncreaseOutdatesCredential() throws Exception
	{
		LocalCredentialVerificator verificator = getMockPasswordVerificator();
		PasswordCredential credCfg = getEmpty();
		credCfg.setScryptParams(new ScryptParams(10));
		verificator.setSerializedConfiguration(toCredConfig(credCfg));

		credCfg.setScryptParams(new ScryptParams(20));
		boolean outdating = verificator.isCredentialDefinitionChagneOutdatingCredentials(toCredConfig(credCfg));
		
		assertThat(outdating, is(true));
	}

	@Test
	public void storageDecreaseDoesntOutdatesCredential() throws Exception
	{
		LocalCredentialVerificator verificator = getMockPasswordVerificator();
		PasswordCredential credCfg = getEmpty();
		credCfg.setScryptParams(new ScryptParams(20));
		verificator.setSerializedConfiguration(toCredConfig(credCfg));

		credCfg.setScryptParams(new ScryptParams(10));
		boolean outdating = verificator.isCredentialDefinitionChagneOutdatingCredentials(toCredConfig(credCfg));
		
		assertThat(outdating, is(false));
	}
	
	@Test
	public void resetSettingsChangeDoesntOutdatesCredential() throws Exception
	{
		LocalCredentialVerificator verificator = getMockPasswordVerificator();
		PasswordCredential credCfg = getEmpty();
		PasswordCredentialResetSettings resetSettings = new PasswordCredentialResetSettings();
		resetSettings.setCodeLength(3);
		resetSettings.setConfirmationMode(ConfirmationMode.NothingRequire);
		credCfg.setPasswordResetSettings(resetSettings);
		verificator.setSerializedConfiguration(toCredConfig(credCfg));

		resetSettings.setCodeLength(10);
		resetSettings.setConfirmationMode(ConfirmationMode.RequireEmail);
		resetSettings.setEmailSecurityCodeMsgTemplate("foo");
		boolean outdating = verificator.isCredentialDefinitionChagneOutdatingCredentials(toCredConfig(credCfg));
		
		assertThat(outdating, is(false));
	}
	
	private String toCredConfig(PasswordCredential credCfg)
	{
		return JsonUtil.serialize(credCfg.getSerializedConfiguration());
	}
	
	
	private PasswordCredential getEmpty()
	{
		PasswordCredential credCfg = new PasswordCredential();
		credCfg.setMinLength(1);
		credCfg.setMinScore(0);
		credCfg.setDenySequences(false);
		credCfg.setHistorySize(0);
		credCfg.setMaxAge(PasswordCredential.MAX_AGE_UNDEF);
		credCfg.setMinClassesNum(1);
		credCfg.setScryptParams(new ScryptParams(10));
		return credCfg;
	}
	
	private PasswordVerificator getMockPasswordVerificator()
	{
		return new PasswordVerificator(null, null, Optional.empty());
	}
}
