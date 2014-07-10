/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.RemoteAuthenticationContextManagement;

/**
 * Singleton component managing SAML ECP contexts used in all remote authentications currently handled by the server.
 * See {@link RemoteAuthenticationContextManagement}.
 * @author K. Benedyczak
 */
@Component
public class ECPContextManagement extends RemoteAuthenticationContextManagement<ECPAuthnState>
{
}
