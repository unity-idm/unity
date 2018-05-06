/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Represents business object of password credential. Supports (de)serialization
 * of the state to/from JSON.
 *  
 * @author K. Benedyczak
 */
public class PasswordCredential
{
	//200 years should be enough, Long MAX is too much as we would fail on maths
	public static final long MAX_AGE_UNDEF = 200L*12L*30L*24L*3600000L; 
	
	private int minLength = 1;
	private int historySize = 1;
	private int minClassesNum = 1;
	private boolean denySequences = false;
	private boolean allowLegacy = true;
	private int minScore = 10;
	private long maxAge = MAX_AGE_UNDEF; 
	private ScryptParams scryptParams;
	private PasswordCredentialResetSettings passwordResetSettings = new PasswordCredentialResetSettings();
	
	public ObjectNode getSerializedConfiguration() throws InternalException
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("minLength", minLength);
		root.put("historySize", historySize);
		root.put("minClassesNum", minClassesNum);
		root.put("maxAge", maxAge);
		root.put("denySequences", denySequences);
		root.put("allowLegacy", allowLegacy);
		root.putPOJO("scryptParams", scryptParams);
		root.put("minScore", minScore);
		
		ObjectNode resetNode = root.putObject("resetSettings");
		passwordResetSettings.serializeTo(resetNode);

		return root;
	}

	public void setSerializedConfiguration(ObjectNode root) throws InternalException
	{
		minLength = root.get("minLength").asInt();
		if (minLength <= 0 || minLength > 100)
			throw new InternalException("Minimal password length must be in range [1-100]");
		historySize = root.get("historySize").asInt();
		if (historySize < 0 || historySize > 1000)
			throw new InternalException("History size must be in range [0-1000]");
		minClassesNum = root.get("minClassesNum").asInt();
		if (minClassesNum <= 0 || minClassesNum > 4)
			throw new InternalException("Minimum classes number must be in range [1-4]");
		maxAge = root.get("maxAge").asLong();
		if (maxAge <= 0)
			throw new InternalException("Maximum age must be positive");
		denySequences = root.get("denySequences").asBoolean();
		
		JsonNode allowLegacyNode = root.get("allowLegacy");
		if (allowLegacyNode != null)
			allowLegacy = allowLegacyNode.asBoolean();
		
		JsonNode scryptParamsNode = root.get("scryptParams");
		if (scryptParamsNode != null && !scryptParamsNode.isNull())
			scryptParams = Constants.MAPPER.convertValue(scryptParamsNode, 
				ScryptParams.class);
		else
			scryptParams = new ScryptParams();
		scryptParams.sanitize();
		
		JsonNode resetNode = root.get("resetSettings");
		if (resetNode != null)
			passwordResetSettings.deserializeFrom((ObjectNode) resetNode);
		
		if (JsonUtil.notNull(root, "minScore"))
			minScore = root.get("minScore").asInt();
		else
			minScore = 0;
	}
	
	public int getMinLength()
	{
		return minLength;
	}

	public void setMinLength(int minLength)
	{
		this.minLength = minLength;
	}

	public int getHistorySize()
	{
		return historySize;
	}

	public void setHistorySize(int historySize)
	{
		this.historySize = historySize;
	}

	public int getMinClassesNum()
	{
		return minClassesNum;
	}

	public void setMinClassesNum(int minClassesNum)
	{
		this.minClassesNum = minClassesNum;
	}

	public boolean isDenySequences()
	{
		return denySequences;
	}

	public boolean isAllowLegacy()
	{
		return allowLegacy;
	}

	public void setAllowLegacy(boolean allowLegacy)
	{
		this.allowLegacy = allowLegacy;
	}

	public void setDenySequences(boolean denySequences)
	{
		this.denySequences = denySequences;
	}

	public long getMaxAge()
	{
		return maxAge;
	}

	public void setMaxAge(long maxAge)
	{
		this.maxAge = maxAge;
	}

	public ScryptParams getScryptParams()
	{
		return scryptParams;
	}

	public void setScryptParams(ScryptParams params)
	{
		this.scryptParams = params;
	}

	public PasswordCredentialResetSettings getPasswordResetSettings()
	{
		return passwordResetSettings;
	}

	public void setPasswordResetSettings(PasswordCredentialResetSettings passwordResetSettings)
	{
		this.passwordResetSettings = passwordResetSettings;
	}

	public int getMinScore()
	{
		return minScore;
	}

	public void setMinScore(int minScore)
	{
		this.minScore = minScore;
	}
}
