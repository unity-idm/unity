/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.util.Random;

import org.bouncycastle.crypto.digests.SHA256Digest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.authn.AbstractLocalVerificator;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

/**
 * Ordinary password credential. Highly configurable: it is possible to set minimal length,
 * what character classes are required, minimum number of character classes, how many previous passwords 
 * should be stored and not repeated after change, how often the password must be changed.
 * TODO - currently this is only a stub. No options are implemented.
 * @author K. Benedyczak
 */
public class PasswordVerificator extends AbstractLocalVerificator implements PasswordExchange
{ 	
	private Random random = new Random();
	
	public PasswordVerificator(String name, String description)
	{
		super(name, description, PasswordExchange.ID);
	}

	@Override
	public String prepareCredential(String rawCredential, String currentCredential)
			throws IllegalCredentialException
	{
		String salt = random.nextInt() + "";
		byte[] hashed = hash(rawCredential, salt);
		
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("password", hashed);
		root.put("salt", salt);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new RuntimeEngineException("Can't serialize password credential to JSON", e);
		}
	}

	@Override
	public LocalCredentialState checkCredentialState(String currentCredential)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSerializedConfiguration()
	{
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		// TODO Auto-generated method stub
		
	}
	
	private byte[] hash(String password, String salt)
	{
		SHA256Digest digest = new SHA256Digest();
		int size = digest.getDigestSize();
		byte[] salted = (salt+password).getBytes(Constants.UTF);
		digest.update(salted, 0, salted.length);
		byte[] hashed = new byte[size];
		digest.doFinal(hashed, 0);
		return hashed;
	}

	@Override
	public long checkPassword(String username, String password)
			throws IllegalIdentityValueException, IllegalCredentialException
	{
		// TODO Auto-generated method stub
		return 0;
	}
}
