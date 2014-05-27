/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.RemoteAuthenticationContextManagement;

/**
 * Responsible for management of OAuth authentication contexts.
 * See {@link RemoteAuthenticationContextManagement}.
 * @author K. Benedyczak
 */
@Component
public class OAuthContextsManagement extends RemoteAuthenticationContextManagement<OAuthContext>
{
}
