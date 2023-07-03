/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt.endpoint;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
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

import org.apache.logging.log4j.Logger;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;

import eu.emi.security.authn.x509.X509Credential;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.rest.jwt.JWTAuthenticationConfig;
import pl.edu.icm.unity.rest.jwt.JWTUtils;
import pl.edu.icm.unity.stdext.identity.PersistentIdentity;

/**
 * JWT tokens management implementation implemented as JAX-RS resource object.
 * 
 * @author K. Benedyczak
 */
public class JWTManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, JWTManagement.class);
	public static final String JWT_TOKEN_ID = "simpleJWT";
	
	private final TokensManagement tokensMan;
	private final EntityManagement identitiesMan;
	private final PKIManagement pkiManagement;
	private final String audience;
	private final String issuer;
	private final JWTAuthenticationConfig config;

	public JWTManagement(TokensManagement tokensMan, PKIManagement pkiManagement, EntityManagement identitiesMan,
			String realm, String address, JWTAuthenticationConfig config)
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
		EntityParam entityId = getCurrentEntity();
		return generate(entityId);
	}

	public String generate(EntityParam entityId)
	{
		X509Credential signingCred = getCredential();
		String persistentId = getClientsPersistentId(entityId);
		return generateCommon(signingCred, persistentId, entityId);
	}
	
	@Path("/refreshToken")
	@POST
	@Produces("application/jwt")
	public String refresh(String toRefresh)
	{	
		X509Credential signingCred = getCredential();
		JWTClaimsSet claims = parseAndValidate(toRefresh, signingCred);
		
		EntityParam entityId = getCurrentEntity();
		String persistentId = getClientsPersistentIdValidating(entityId, claims.getSubject());

		try
		{
			tokensMan.getTokenById(JWT_TOKEN_ID, claims.getJWTID());
		} catch (IllegalArgumentException e)
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
		JWTClaimsSet claims = parseAndValidate(toDrop, signingCred);
		
		EntityParam entityId = getCurrentEntity();
		getClientsPersistentIdValidating(entityId, claims.getSubject());

		try
		{
			tokensMan.removeToken(JWT_TOKEN_ID, claims.getJWTID());
		} catch (IllegalArgumentException e)
		{
			throw new ClientErrorException(Response.Status.GONE);
		}
	}
	
	
	
	
	private String generateCommon(X509Credential signingCred, String persistentId, EntityParam entityId)
	{
		Date now = new Date();
		long ttl = 1000 * config.tokenTTLSeconds;
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
			tokensMan.addToken(JWT_TOKEN_ID, id, entityId, token.getBytes(StandardCharsets.UTF_8), 
					now, expiration);
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
		String credential = config.signingCredential;
		try
		{
			return pkiManagement.getCredential(credential);
		} catch (EngineException e2)
		{
			log.error("Can not load credential configured to sign JWTs", e2);
			throw new InternalServerErrorException();
		}
	}
	
	private JWTClaimsSet parseAndValidate(String token, X509Credential signingCred)
	{
		try
		{
			return JWTUtils.parseAndValidate(token, signingCred);
		} catch (ParseException|JOSEException e1)
		{
			log.warn("Received invalid JWT to be refreshed", e1);
			throw new BadRequestException(e1);
		}
	}
	
	private String getClientsPersistentId(EntityParam entityId)
	{
		Entity entity;
		try
		{
			entity = identitiesMan.getEntity(entityId, null, true, "/");
		} catch (EngineException e)
		{
			log.error("Can't resolve entities of the authenticated client, "
					+ "entity id is " + entityId.getEntityId(), e);
			throw new InternalServerErrorException();
		}
		List<Identity> ids = entity.getIdentities();
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
