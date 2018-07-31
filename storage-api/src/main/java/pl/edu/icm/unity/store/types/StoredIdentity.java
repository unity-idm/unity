/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.types;

import java.nio.charset.StandardCharsets;

import org.apache.xml.security.utils.Base64;
import org.bouncycastle.crypto.digests.SHA512Digest;

import pl.edu.icm.unity.types.NamedObject;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * Holds {@link Identity} and properly creates the comparable value which is used in database:
 * it contains identity type and type-specific comparable value (which is available in 
 * the base {@link Identity}) which is hashed so we ensure that even long identities are supported.
 * @author K. Benedyczak
 */
public class StoredIdentity implements NamedObject
{
	private Identity identity;
	private String inDBComparableName;
	
	public StoredIdentity(Identity identity)
	{
		this.identity = identity;
		this.inDBComparableName = toInDBIdentityValue(identity.getTypeId(), 
				identity.getComparableValue());
	}

	@Override
	public String getName()
	{
		return inDBComparableName;
	}

	/**
	 * @return entityId (used by index)
	 */
	public long getEntityId()
	{
		return identity.getEntityId();
	}
	
	public Identity getIdentity()
	{
		return identity;
	}
	
	/**
	 * @param typeName
	 * @param comparableTypeSpecificValue
	 * @return in-db name which can be used as a key for a given type and type-specific comparable value
	 */
	public static String toInDBIdentityValue(String typeName, String comparableTypeSpecificValue)
	{
		return hashIdentity(typeName + "::" + comparableTypeSpecificValue);
	}
	
	/**
	 * Hashes identity with SHA512 and returns Base64 encoded string
	 * @param identity
	 * @return
	 */
	private static String hashIdentity(String identity)
	{
		SHA512Digest digest = new SHA512Digest();
		int size = digest.getDigestSize();
		byte[] asBytes = identity.getBytes(StandardCharsets.UTF_8);
		digest.update(asBytes, 0, asBytes.length);
		byte[] hashed = new byte[size];
		digest.doFinal(hashed, 0);
		return Base64.encode(hashed, 0);
	}

	@Override
	public StoredIdentity clone()
	{
		return new StoredIdentity(identity.clone());
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((identity == null) ? 0 : identity.hashCode());
		result = prime * result + ((inDBComparableName == null) ? 0
				: inDBComparableName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StoredIdentity other = (StoredIdentity) obj;
		if (identity == null)
		{
			if (other.identity != null)
				return false;
		} else if (!identity.equals(other.identity))
			return false;
		if (inDBComparableName == null)
		{
			if (other.inDBComparableName != null)
				return false;
		} else if (!inDBComparableName.equals(other.inDBComparableName))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "StoredIdentity [identity=" + identity + ", inDBComparableName="
				+ inDBComparableName + "]";
	}
}
