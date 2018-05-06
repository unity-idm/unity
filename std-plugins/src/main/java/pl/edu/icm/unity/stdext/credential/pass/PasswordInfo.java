/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * In DB representation of the credential state is a list of objects as the 
 * one described in this class.
 * <p>
 * Object stores hash, salt and time as 1st level citizens. Additionally hashing method and its
 * parameters are encoded in a map. The map should use method specific keys and simple objects only
 * directly JSON serializable. 
 * 
 * @author K. Benedyczak
 */
public class PasswordInfo
{
	private PasswordHashMethod method;
	private Map<String, Object> methodParams;
	private byte[] hash;
	private byte[] salt;
	private Date time;

	public PasswordInfo(PasswordHashMethod method, byte[] hash, byte[] salt, 
			Map<String, Object> methodParams)
	{
		this(method, hash, salt, methodParams, System.currentTimeMillis());
	}
	
	public PasswordInfo(PasswordHashMethod method, byte[] hash, byte[] salt, 
			Map<String, Object> methodParams, long time)
	{
		this.method = method;
		this.methodParams = new HashMap<>(methodParams);
		this.hash = Arrays.copyOf(hash, hash.length);
		this.salt = salt;
		this.time = new Date(time);
	}
	
	protected PasswordInfo()
	{
	}
	
	public byte[] getHash()
	{
		return Arrays.copyOf(hash, hash.length);
	}
	public byte[] getSalt()
	{
		return salt;
	}
	public Date getTime()
	{
		return time;
	}

	public PasswordHashMethod getMethod()
	{
		return method;
	}

	public Map<String, Object> getMethodParams()
	{
		return  new HashMap<>(methodParams);
	}
}