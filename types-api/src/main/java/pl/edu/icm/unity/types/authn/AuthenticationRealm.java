/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.types.DescribedObjectImpl;

/**
 * Authentication realm definition.
 * @author K. Benedyczak
 */
public class AuthenticationRealm extends DescribedObjectImpl
{
	private int blockAfterUnsuccessfulLogins;
	private int blockFor;
	private int allowForRememberMeDays;
	private RememberMePolicy rememberMePolicy;
	private int maxInactivity;

	public AuthenticationRealm()
	{
	}

	public AuthenticationRealm(String name, String description, int blockAfterUnsuccessfulLogins, int blockFor,
			RememberMePolicy rememberMePolicy, int allowForRememberMeDays, int maxInactivity)
	{
		super(name, description);
		this.blockAfterUnsuccessfulLogins = blockAfterUnsuccessfulLogins;
		this.blockFor = blockFor;
		this.allowForRememberMeDays = allowForRememberMeDays;
		this.rememberMePolicy = rememberMePolicy;
		this.maxInactivity = maxInactivity;
	}

	@JsonCreator
	public AuthenticationRealm(ObjectNode root)
	{
		super(root);
		fromJson(root);
	}

	public int getBlockAfterUnsuccessfulLogins()
	{
		return blockAfterUnsuccessfulLogins;
	}
	public void setBlockAfterUnsuccessfulLogins(int blockAfterUnsuccessfulLogins)
	{
		this.blockAfterUnsuccessfulLogins = blockAfterUnsuccessfulLogins;
	}
	public int getBlockFor()
	{
		return blockFor;
	}
	public void setBlockFor(int blockFor)
	{
		this.blockFor = blockFor;
	}
	public int getAllowForRememberMeDays()
	{
		return allowForRememberMeDays;
	}
	public void setAllowForRememberMeDays(int allowForRememberMeDays)
	{
		this.allowForRememberMeDays = allowForRememberMeDays;
	}
	public int getMaxInactivity()
	{
		return maxInactivity;
	}
	public void setMaxInactivity(int maxInactivity)
	{
		this.maxInactivity = maxInactivity;
	}
	
	public RememberMePolicy getRememberMePolicy()
	{
		return rememberMePolicy;
	}

	public void setRememberMePolicy(RememberMePolicy rememberMePolicy)
	{
		this.rememberMePolicy = rememberMePolicy;
	}

	private void fromJson(ObjectNode root)
	{
		rememberMePolicy = RememberMePolicy.valueOf(root.get("rememberMePolicy").asText());
		allowForRememberMeDays = root.get("allowForRememberMeDays").asInt();
		blockAfterUnsuccessfulLogins = root.get("blockAfterUnsuccessfulLogins").asInt();
		blockFor = root.get("blockFor").asInt();
		maxInactivity = root.get("maxInactivity").asInt();
	}
	
	@JsonValue
	@Override
	public ObjectNode toJson()
	{
		ObjectNode root = super.toJson();
		root.put("rememberMePolicy", getRememberMePolicy().toString());
		root.put("allowForRememberMeDays", getAllowForRememberMeDays());
		root.put("blockAfterUnsuccessfulLogins", getBlockAfterUnsuccessfulLogins());
		root.put("blockFor", getBlockFor());
		root.put("maxInactivity", getMaxInactivity());
		return root;
	}
	
	@Override
	public String toString()
	{
		return "AuthenticationRealm [blockAfterUnsuccessfulLogins="
				+ blockAfterUnsuccessfulLogins + ", blockFor=" + blockFor
				+ ", rememberMePolicy=" + rememberMePolicy
				+ ", allowForRememberMeDays=" + allowForRememberMeDays
				+ ", maxInactivity=" + maxInactivity + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + rememberMePolicy.hashCode();
		result = prime * result + allowForRememberMeDays;
		result = prime * result + blockAfterUnsuccessfulLogins;
		result = prime * result + blockFor;
		result = prime * result + maxInactivity;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		AuthenticationRealm other = (AuthenticationRealm) obj;
		if (allowForRememberMeDays != other.allowForRememberMeDays)
			return false;
		if (blockAfterUnsuccessfulLogins != other.blockAfterUnsuccessfulLogins)
			return false;
		if (blockFor != other.blockFor)
			return false;
		if (maxInactivity != other.maxInactivity)
			return false;
		return true;
	}
}
