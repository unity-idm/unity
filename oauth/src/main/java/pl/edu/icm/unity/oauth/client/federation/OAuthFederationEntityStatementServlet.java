/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.io.IOException;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.http.JakartaServletUtils;
import com.nimbusds.openid.connect.sdk.federation.api.FetchEntityStatementSuccessResponse;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import pl.edu.icm.unity.base.utils.Log;

public class OAuthFederationEntityStatementServlet extends HttpServlet
{
	public static final String PATH = "/oauth2-federation-entity";
	public static final String WELL_KNOWN_SUFFIX = "/.well-known/openid-federation";

	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthFederationEntityStatementServlet.class);

	private final OAuthFederationMetadataManager federationManager;

	public OAuthFederationEntityStatementServlet(OAuthFederationMetadataManager federationManager)
	{
		this.federationManager = federationManager;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		String pathInfo = req.getPathInfo();
		if (pathInfo == null || !pathInfo.endsWith(WELL_KNOWN_SUFFIX))
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Path must end with " + WELL_KNOWN_SUFFIX);
			return;
		}

		String nameSegment = pathInfo.substring(0, pathInfo.length() - WELL_KNOWN_SUFFIX.length());
		String authenticatorName = nameSegment.startsWith("/") ? nameSegment.substring(1) : nameSegment;
		if (authenticatorName.isEmpty())
		{
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Authenticator name missing in path");
			return;
		}

		OAuthFederationEntityStatementConfig config = federationManager.getConfiguration(authenticatorName);

		if (config == null)
		{
			resp.sendError(HttpServletResponse.SC_NOT_FOUND,
					"No federation configuration for authenticator: " + authenticatorName);
			return;
		}

		try
		{
			EntityStatement entityStatement = OAuthFederationEntityStatementGenerator.generate(config);
			JakartaServletUtils.applyHTTPResponse(
					new FetchEntityStatementSuccessResponse(entityStatement).toHTTPResponse(), resp);
		} catch (Exception e)
		{
			log.error("Failed to generate federation entity statement for authenticator: " + authenticatorName, e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Failed to generate entity statement");
		}
	}
}
