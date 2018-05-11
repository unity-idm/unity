/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.credential.sms;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;

/**
 * Represents business object of sms credential. Supports (de)serialization
 * of the state to/from JSON.
 * 
 * @author P.Piernik
 */
public class SMSCredential
{
	public static final int DEFAULT_VALIDITY = 15;
	public static final int DEFAULT_CODE_LENGTH = 6;
	private static final int AUTHN_SMS_LIMIT = 3;
	
	private int validityTime = DEFAULT_VALIDITY;
	private int codeLength = DEFAULT_CODE_LENGTH;
	private String messageTemplate;	
	private int authnSMSLimit = AUTHN_SMS_LIMIT;
		
	private MobileNumberConfirmationConfiguration mobileNumberConfirmationConfiguration;
	private SMSCredentialRecoverySettings recoverySettings = new SMSCredentialRecoverySettings();
	
	public ObjectNode getSerializedConfiguration() throws InternalException
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("validityTime", validityTime);
		root.put("codeLength", codeLength);
		root.put("messageTemplate", messageTemplate);
		root.put("authnSMSLimit", authnSMSLimit);
		ObjectNode recoverNode = root.putObject("recoverySettings");
		recoverySettings.serializeTo(recoverNode);
		
		if (getMobileNumberConfirmationConfiguration().isPresent())
		{
			root.set("mobileConfirmationConfiguration", getMobileNumberConfirmationConfiguration().get().toJson());
		}
		
		return root;
	}
	
	public void setSerializedConfiguration(ObjectNode root) throws InternalException
	{
		codeLength = root.get("codeLength").asInt();
		if (codeLength <= 2 || codeLength > 50)
			throw new InternalException("Minimal code length must be in range [1-50]");
		validityTime = root.get("validityTime").asInt();
		if (validityTime < 0 || validityTime > 60 * 24 * 365)
			throw new InternalException("Validity time must be in range [0-525600]");
		setAuthnSMSLimit(root.get("authnSMSLimit").asInt());
		if (getAuthnSMSLimit() < 0 || getAuthnSMSLimit() > 10000)
			throw new InternalException("AuthnSMSLimit must be in range [0-10000]");	
		JsonNode msg = root.get("messageTemplate");
		if (msg != null);
			messageTemplate = msg.asText();
		JsonNode confirmationConfigNode = root.get("mobileConfirmationConfiguration");
		if (confirmationConfigNode != null)
			setMobileNumberConfirmationConfiguration(new MobileNumberConfirmationConfiguration((ObjectNode) root.get("mobileConfirmationConfiguration")));
		JsonNode recoverNode = root.get("recoverySettings");
		if (recoverNode != null)
			recoverySettings.deserializeFrom((ObjectNode) recoverNode);
	}
	
	public int getValidityTime()
	{
		return validityTime;
	}

	public void setValidityTime(int validityTime)
	{
		this.validityTime = validityTime;
	}

	public int getCodeLength()
	{
		return codeLength;
	}

	public void setCodeLength(int codeLength)
	{
		this.codeLength = codeLength;
	}

	public SMSCredentialRecoverySettings getRecoverySettings()
	{
		return recoverySettings;
	}

	public void setRecoverySettings(SMSCredentialRecoverySettings recoverSettings)
	{
		this.recoverySettings = recoverSettings;
	}
	
	public void setMobileNumberConfirmationConfiguration(
			MobileNumberConfirmationConfiguration confirmationConfiguration)
	{
		this.mobileNumberConfirmationConfiguration = confirmationConfiguration;
	}
	
	public Optional<MobileNumberConfirmationConfiguration> getMobileNumberConfirmationConfiguration()
	{
		return Optional.ofNullable(mobileNumberConfirmationConfiguration);
	}

	public String getMessageTemplate()
	{
		return messageTemplate;
	}

	public void setMessageTemplate(String messageTemplate)
	{
		this.messageTemplate = messageTemplate;
	}

	public int getAuthnSMSLimit()
	{
		return authnSMSLimit;
	}

	public void setAuthnSMSLimit(int authnSMSLimit)
	{
		this.authnSMSLimit = authnSMSLimit;
	}
}
