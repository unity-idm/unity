/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.devicesignin;

import java.util.Collections;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.auth.server.SecureVaadin2XEndpoint;
import io.imunity.vaadin.endpoint.common.RemoteRedirectedAuthnResponseProcessingFilter;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFactory;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthResourceProvider;

/**
 * RFC 8628 §3.3: standardly-authenticated Vaadin endpoint where a logged in user confirms/denies a
 * device authorization request identified by a user_code.
 */
@PrototypeComponent
public class DeviceSignInWebEndpoint extends SecureVaadin2XEndpoint
{
	public static final String NAME = "OAuth2DeviceSignIn";
	public static final String SERVLET_PATH = "/device_signin";
	public static final EndpointTypeDescription TYPE = new EndpointTypeDescription(NAME,
			"OAuth 2 Server - RFC 8628 Device Authorization Grant sign-in endpoint", VaadinAuthentication.NAME,
			Collections.singletonMap(SERVLET_PATH, "OAuth 2 Device sign-in web endpoint"));

	private final PKIManagement pkiManagement;
	private final OAuthEndpointsCoordinator coordinator;

	@Autowired
	public DeviceSignInWebEndpoint(NetworkServer server, AdvertisedAddressProvider advertisedAddrProvider,
			MessageSource msg, ApplicationContext applicationContext,
			RemoteRedirectedAuthnResponseProcessingFilter remoteAuthnResponseProcessingFilter,
			SandboxAuthnRouter sandboxAuthnRouter, PKIManagement pkiManagement, OAuthEndpointsCoordinator coordinator)
	{
		super(server, advertisedAddrProvider, msg, applicationContext, new OAuthResourceProvider(),
				SERVLET_PATH, remoteAuthnResponseProcessingFilter, sandboxAuthnRouter, DeviceSignInServlet.class);
		this.pkiManagement = pkiManagement;
		this.coordinator = coordinator;
	}

	@Override
	public void setSerializedConfiguration(String properties)
	{
		super.setSerializedConfiguration(properties);
		try
		{
			OAuthASProperties config = new OAuthASProperties(this.properties, pkiManagement,
					getServletUrl(SERVLET_PATH));
			String issuerUri = config.getValue(OAuthASProperties.ISSUER_URI);
			coordinator.registerDeviceSignInEndpoint(issuerUri, getServletUrl(SERVLET_PATH));
			coordinator.registerDeviceSignInConfig(issuerUri, config);
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't initialize the OAuth 2 device sign-in endpoint's configuration",
					e);
		}
	}

	@Component
	public static class Factory implements EndpointFactory
	{
		@Autowired
		private ObjectFactory<DeviceSignInWebEndpoint> factory;

		@Override
		public EndpointTypeDescription getDescription()
		{
			return TYPE;
		}

		@Override
		public EndpointInstance newInstance()
		{
			return factory.getObject();
		}
	}
}
