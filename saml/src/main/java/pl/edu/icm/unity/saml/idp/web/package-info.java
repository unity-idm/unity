/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on 09-01-2014
 * Author: K. Benedyczak <golbi@icm.edu.pl>
 */

/**
 * SAML 2 IdP, web part. Implements SAML SSO profile. Currently the HTTP POST and 
 * the HTTP Redirect bindings. The overall processing is as follows (paths are given without
 * the leading endpoint context part):
 * <pre>
 * SAML request in POST/GET to /saml2-idp-web
 * ->
 * SAML request is validated by the SamlParseServlet and if fine put in the session-stored SAML context.
 * -> 
 * processing is forwarded to /saml2-idp-web-ui
 * -> 
 * authN filter redirects to authN servlet if needed
 * -> 
 * The IdpDispatcherServlet (at /saml2-idp-web-ui) decides whether to process the request automatically 
 * or whether to show a consent screen. In the first case it returns a response and in second it forwards to
 * ->
 * the SamlIdpWebUI which shows the confirmation & info screen and 
 * if accepted redirects with a SAML response.  
 * </pre>
 * The {@link pl.edu.icm.unity.saml.idp.web.filter.SamlGuardFilter} protects
 * both servlets so it is always guaranteed that the {@link pl.edu.icm.unity.saml.idp.web.SamlIdPWebUI}
 * can be reached only when SAML context is available in session. 
 *   
 * @author K. Benedyczak
 */
package pl.edu.icm.unity.saml.idp.web;