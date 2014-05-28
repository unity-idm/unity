/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt.endpoint;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;

import eu.emi.security.authn.x509.X509Credential;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationProperties;
import pl.edu.icm.unity.rest.jwt.JWTUtils;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;

/**
 * JWT tokens management implementation implemented as JAX-RS resource object.
 * 
 * @author K. Benedyczak
 */
public class JWTManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, JWTManagement.class);
	public static final String JWT_TOKEN_ID = "simpleJWT";
	
	private TokensManagement tokensMan;
	private IdentitiesManagement identitiesMan;
	private PKIManagement pkiManagement;
	private String audience;
	private String issuer;
	private JWTAuthenticationProperties config;

	public JWTManagement(TokensManagement tokensMan, PKIManagement pkiManagement, IdentitiesManagement identitiesMan,
			String realm, String address, JWTAuthenticationProperties config)
	{
		this.tokensMan = tokensMan;
		this.pkiManagement = pkiManagement;
		this.identitiesMan = identitiesMan;
		this.issuer = address;
		this.audience = address + "#" + realm;
		this.config = config;
	}
	
	@Path("/token")
	@GET
	@Produces("application/jwt")
	public String generate()
	{
		X509Credential signingCred = getCredential();
		EntityParam entityId = getCurrentEntity();
		String persistentId = getClientsPersistentId(entityId);
		return generateCommon(signingCred, persistentId, entityId);
	}
	
	@Path("/refreshToken")
	@POST
	@Produces("application/jwt")
	public String refresh(String toRefresh)
	{	
		X509Credential signingCred = getCredential();
		ReadOnlyJWTClaimsSet claims = parseAndValidate(toRefresh, signingCred);
		
		EntityParam entityId = getCurrentEntity();
		String persistentId = getClientsPersistentIdValidating(entityId, claims.getSubject());

		try
		{
			tokensMan.getTokenById(JWT_TOKEN_ID, claims.getJWTID());
		} catch (WrongArgumentException e)
		{
			throw new ClientErrorException(Response.Status.GONE);
		}
		
		return generateCommon(signingCred, persistentId, entityId);
	}
	
	@Path("/invalidateToken")
	@POST
	public void invalidate(String toDrop)
	{
		X509Credential signingCred = getCredential();
		ReadOnlyJWTClaimsSet claims = parseAndValidate(toDrop, signingCred);
		
		EntityParam entityId = getCurrentEntity();
		getClientsPersistentIdValidating(entityId, claims.getSubject());

		try
		{
			tokensMan.removeToken(JWT_TOKEN_ID, claims.getJWTID());
		} catch (WrongArgumentException e)
		{
			throw new ClientErrorException(Response.Status.GONE);
		}
	}
	
	
	
	
	private String generateCommon(X509Credential signingCred, String persistentId, EntityParam entityId)
	{
		Date now = new Date();
		long ttl = 1000 * config.getIntValue(JWTAuthenticationProperties.TOKEN_TTL);
		Date expiration = new Date(now.getTime() + ttl);
		String id = UUID.randomUUID().toString();
		
		String token;
		try
		{
			token = JWTUtils.generate(signingCred, persistentId, issuer, audience, expiration, id);
		} catch (Exception e)
		{
			log.error("Can't generate JWT", e);
			throw new InternalServerErrorException();
		}
		
		try
		{
			tokensMan.addToken(JWT_TOKEN_ID, id, entityId, token.getBytes(Constants.UTF), now, expiration);
		} catch (Exception e)
		{
			log.error("Can't persist the generated JWT", e);
			throw new InternalServerErrorException();
		}
		
		return token;
	}
	
	private EntityParam getCurrentEntity()
	{
		InvocationContext ctx = InvocationContext.getCurrent();
		LoginSession ls = ctx.getLoginSession();
		return new EntityParam(ls.getEntityId());
	}

	private String getClientsPersistentIdValidating(EntityParam entityId, String expected)
	{
		String persistentId = getClientsPersistentId(entityId);
		if (!persistentId.equals(expected))
		{
			log.warn("Client with persistent id " + persistentId + " is trying to manipulate JWT of " + 
					expected);
			throw new ClientErrorException(Status.FORBIDDEN);
		}
		return persistentId;
	}
	
	private X509Credential getCredential()
	{
		String credential = config.getValue(JWTAuthenticationProperties.SIGNING_CREDENTIAL);
		try
		{
			return pkiManagement.getCredential(credential);
		} catch (EngineException e2)
		{
			log.error("Can not load credential configured to sign JWTs", e2);
			throw new InternalServerErrorException();
		}
	}
	
	private ReadOnlyJWTClaimsSet parseAndValidate(String token, X509Credential signingCred)
	{
		try
		{
			return JWTUtils.parseAndValidate(token, signingCred);
		} catch (ParseException|JOSEException e1)
		{
			log.debug("Received invalid JWT to be refreshed", e1);
			throw new BadRequestException(e1);
		}
	}
	
	private String getClientsPersistentId(EntityParam entityId)
	{
		Entity entity;
		try
		{
			entity = identitiesMan.getEntity(entityId, null, true);
		} catch (EngineException e)
		{
			log.error("Can't resolve entities of the authenticated client, "
					+ "entity id is " + entityId.getEntityId(), e);
			throw new InternalServerErrorException();
		}
		Identity[] ids = entity.getIdentities();
		for (Identity id: ids)
		{
			if (PersistentIdentity.ID.equals(id.getTypeId()))
			{
				return id.getValue();
			}
		}

		log.fatal("Authenticated client has no persistent identity, entity id is " + entityId.getEntityId());
		throw new InternalServerErrorException();
	}
}
