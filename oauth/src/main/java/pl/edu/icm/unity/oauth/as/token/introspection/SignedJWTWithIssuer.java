/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.introspection;

import java.text.ParseException;
import java.util.Objects;

import com.nimbusds.jwt.SignedJWT;

public class SignedJWTWithIssuer
{
	public final SignedJWT signedJWT;
	public final String issuer;
	
	public SignedJWTWithIssuer(SignedJWT signedJWT) throws ParseException
	{
		this.signedJWT = signedJWT;
		this.issuer = signedJWT.getJWTClaimsSet().getIssuer();
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(issuer, signedJWT);
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
		SignedJWTWithIssuer other = (SignedJWTWithIssuer) obj;
		return Objects.equals(issuer, other.issuer) && Objects.equals(signedJWT, other.signedJWT);
	}
	
	
	
	
}
