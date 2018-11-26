/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.credential.sms;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Lost phone recovery settings
 * 
 * @author P.Piernik
 *
 */
public class SMSCredentialRecoverySettings
{
	public static final int DEFAULT_CODE_LENGTH = 6;
	
	private boolean enabled = false;
	private String emailSecurityCodeMsgTemplate;
	private int codeLength = DEFAULT_CODE_LENGTH;
	private boolean capchaRequire = true;

	public SMSCredentialRecoverySettings()
	{		
	}

	public SMSCredentialRecoverySettings(ObjectNode node)
	{
		deserializeFrom(node);
	}
	
	public void serializeTo(ObjectNode node)
	{
		node.put("enable", enabled);
		if (!enabled)
			return;
		node.put("emailSecurityCodeMsgTemplate", emailSecurityCodeMsgTemplate);
		node.put("codeLength", codeLength);
		node.put("capchaRequire", capchaRequire);
	}

	public void deserializeFrom(ObjectNode node)
	{
		this.enabled = node.get("enable").asBoolean();
		if (!enabled)
			return;
		
		if (node.has("codeLength"))
			codeLength = node.get("codeLength").asInt();		
		if (node.has("emailSecurityCodeMsgTemplate")
				&& !node.get("emailSecurityCodeMsgTemplate").isNull())
			emailSecurityCodeMsgTemplate = node.get("emailSecurityCodeMsgTemplate")
					.asText();
		if (node.has("capchaRequire"))
			capchaRequire = node.get("capchaRequire").asBoolean();

	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public String getEmailSecurityCodeMsgTemplate()
	{
		return emailSecurityCodeMsgTemplate;
	}

	public void setEmailSecurityCodeMsgTemplate(String emailSecurityCodeMsgTemplate)
	{
		this.emailSecurityCodeMsgTemplate = emailSecurityCodeMsgTemplate;
	}

	public boolean isCapchaRequired()
	{
		return capchaRequire;
	}

	public void setCapchaRequire(boolean capchaRequire)
	{
		this.capchaRequire = capchaRequire;
	}

	public int getCodeLength()
	{
		return codeLength;
	}

	public void setCodeLength(int codeLength)
	{
		this.codeLength = codeLength;
	}
}