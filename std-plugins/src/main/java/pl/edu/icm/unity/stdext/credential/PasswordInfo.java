/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.util.Arrays;
import java.util.Date;

/**
 * In DB representation of the credential state is a list of objects as the one described in this class.
 * @author K. Benedyczak
 */
public class PasswordInfo
{
	private byte[] hash;
	private String salt;
	private Date time;
	private int rehashNumber;

	public PasswordInfo(byte[] hash, String salt, int rehashNumber)
	{
		this(hash, salt, rehashNumber, System.currentTimeMillis());
	}
	
	public PasswordInfo(byte[] hash, String salt, int rehashNumber, long time)
	{
		this.rehashNumber = rehashNumber;
		this.hash = Arrays.copyOf(hash, hash.length);
		this.salt = salt;
		this.time = new Date(time);
	}
	public byte[] getHash()
	{
		return hash;
	}
	public String getSalt()
	{
		return salt;
	}
	public Date getTime()
	{
		return time;
	}
	public int getRehashNumber()
	{
		return rehashNumber;
	}
}