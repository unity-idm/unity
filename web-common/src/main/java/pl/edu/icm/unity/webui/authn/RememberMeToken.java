/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

/**
 * Contains information used by remember me functionality
 * 
 * @author P.Piernik
 *
 */
public class RememberMeToken
{
	private long entity;
	private LoginMachineDetails machineDetails;
	private Date loginTime;
	private String  firstFactorAuthnOptionId;
	private String  secondFactorAuthnOptionId;
	private byte[] rememberMeTokenHash;
	private RememberMePolicy rememberMePolicy;

	public RememberMeToken()
	{
		
	}
	
	public RememberMeToken(long entity, LoginMachineDetails machineDetails, Date loginTime,
			String firstFactorAuthnOptionId, String secondFactorAuthnOptionId, byte[] rememberMeTokenHash,
			RememberMePolicy rememberMePolicy)
	{
		this.entity = entity;
		this.machineDetails = machineDetails;
		this.loginTime = loginTime;
		this.firstFactorAuthnOptionId = firstFactorAuthnOptionId;
		this.secondFactorAuthnOptionId = secondFactorAuthnOptionId;
		this.rememberMeTokenHash = rememberMeTokenHash;
		this.rememberMePolicy = rememberMePolicy;
	}
	
	public static RememberMeToken getInstanceFromJson(byte[] json) 
	{
		try
		{
			return Constants.MAPPER.readValue(json, RememberMeToken.class);
		} catch (IOException e)
		{
			throw new IllegalArgumentException("Can not parse token's JSON", e);
		}
	}
	
	@JsonIgnore
	public byte[] getSerialized() throws JsonProcessingException
	{
		return Constants.MAPPER.writeValueAsBytes(this);
	}
	
	
	public long getEntity()
	{
		return entity;
	}

	public void setEntity(long entity)
	{
		this.entity = entity;
	}

	public LoginMachineDetails getMachineDetails()
	{
		return machineDetails;
	}

	public void setMachineDetails(LoginMachineDetails machineDetails)
	{
		this.machineDetails = machineDetails;
	}

	public Date getLoginTime()
	{
		return loginTime;
	}

	public void setLoginTime(Date loginTime)
	{
		this.loginTime = loginTime;
	}

	public byte[] getRememberMeTokenHash()
	{
		return rememberMeTokenHash;
	}

	public void setRememberMeTokenHash(byte[] rememberMeTokenHash)
	{
		this.rememberMeTokenHash = rememberMeTokenHash;
	}

	public RememberMePolicy getRememberMePolicy()
	{
		return rememberMePolicy;
	}

	public void setRememberMePolicy(RememberMePolicy rememberMePolicy)
	{
		this.rememberMePolicy = rememberMePolicy;
	}

	public String getFirstFactorAuthnOptionId()
	{
		return firstFactorAuthnOptionId;
	}

	public void setFirstFactorAuthnOptionId(String firstFactorAuthnOptionId)
	{
		this.firstFactorAuthnOptionId = firstFactorAuthnOptionId;
	}

	public String getSecondFactorAuthnOptionId()
	{
		return secondFactorAuthnOptionId;
	}

	public void setSecondFactorAuthnOptionId(String secondFactorAuthnOptionId)
	{
		this.secondFactorAuthnOptionId = secondFactorAuthnOptionId;
	}

	public static class LoginMachineDetails
	{
		private  String ip;
		private  String os;
		private  String browser;

		public LoginMachineDetails()
		{

		}
		
		public LoginMachineDetails(String ip, String os, String browser)
		{
			this.setIp(ip);
			this.os = os;
			this.setBrowser(browser);
		}

		public String getIp()
		{
			return ip;
		}

		public void setIp(String ip)
		{
			this.ip = ip;
		}
		
		public String getOs()
		{
			return os;
		}

		public void setOs(String os)
		{
			this.os = os;
		}

		public String getBrowser()
		{
			return browser;
		}

		public void setBrowser(String browser)
		{
			this.browser = browser;
		}
	}
}
