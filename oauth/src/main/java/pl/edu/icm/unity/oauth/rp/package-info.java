/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 *
 * Created on 10 lis 2014
 * Author: K. Benedyczak
 */

/**
 * Implementation of a simple OAuth authenticator, where Unity acts as a RP: it is assumed that an
 * access token is provided by the client being authenticated and the access token is checked by calling
 * a configured validation endpoint at a remote AS. Finally the user's profile is obtained from the AS.
 * 
 * @author K. Benedyczak
 */
package pl.edu.icm.unity.oauth.rp;