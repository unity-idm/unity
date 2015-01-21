/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt.authn;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.List;
import java.util.Properties;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationProperties;
import pl.edu.icm.unity.rest.jwt.JWTUtils;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.authn.AbstractVerificator;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Simple JWT verificator. Token must be not expired, properly signed, belong to a current realm and issued by 
 * the local system.
 * 
 * @author K. Benedyczak
 */
public class JWTVerificator extends AbstractVerificator implements JWTExchange
{
	private static final String[] IDENTITY_TYPES = {PersistentIdentity.ID};
	private PKIManagement pkiManagement;
	private JWTAuthenticationProperties config;
	
	public JWTVerificator(String name, String description, PKIManagement pkiManagement)
	{
		super(name, description, JWTExchange.ID);
		this.pkiManagement = pkiManagement;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		CharArrayWriter writer = new CharArrayWriter();
		try
		{
			config.getProperties().store(writer, "");
		} catch (IOException e)
		{
			throw new IllegalStateException("Can not serialize JWT verificator's configuration", e);
		}
		return writer.toString();
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		Properties properties = new Properties();
		try
		{
			properties.load(new StringReader(json));
			config = new JWTAuthenticationProperties(properties);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the the "
					+ "JWT verificator's configuration", e);
		}
	}

	@Override
	public AuthenticationResult checkJWT(String token) throws EngineException
	{
		String credential = config.getValue(JWTAuthenticationProperties.SIGNING_CREDENTIAL);
		X509Credential signingCred = pkiManagement.getCredential(credential);
		
		try
		{
			ReadOnlyJWTClaimsSet claims = JWTUtils.parseAndValidate(token, signingCred);
			String realm = InvocationContext.safeGetRealm();
			List<String> audiences = claims.getAudience();
			if (audiences.size() != 1)
			{
				throw new AuthenticationException("Invalid audiences specification: "
						+ "must have exactly one audience");
			}
			String audience = audiences.get(0);
			int hash = audience.lastIndexOf('#');
			if (hash < 0)
			{
				throw new AuthenticationException("Invalid audience specification: "
						+ "no realm specification");
			}
			String tokenRealm = audience.substring(hash+1);
			if (!tokenRealm.equals(realm))
			{
				throw new AuthenticationException("Token's realm '" + tokenRealm + 
						"' is different from the endpoint's realm: " + realm);
			}

			EntityWithCredential resolved = identityResolver.resolveIdentity(claims.getSubject(), 
					IDENTITY_TYPES, null);
			AuthenticatedEntity ae = new AuthenticatedEntity(resolved.getEntityId(), 
					claims.getSubject(), false); 
			return new AuthenticationResult(Status.success, ae);
		} catch (ParseException | JOSEException e)
		{
			throw new AuthenticationException("Token is invalid", e);
		}
	}
}
