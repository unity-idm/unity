/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hc.client5.http.ContextBuilder;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;

abstract class UpmanRESTTestBase extends DBIntegrationTestBase
{
	public static final String AUTHENTICATOR_REST_PASS = "ApassREST";
	public static final String CONFIGURATION = "#\n" +
		"unity.upman.rest.rootGroup=/A\n" +
		"unity.upman.rest.authorizationGroup=/A\n";
	public static final String AUTHENTICATION_FLOW_PASS = "ApassRESTFlow";
	protected long entityId;
	protected String entityEmail;

	@Autowired
	protected AuthenticatorManagement authnMan;
	@Autowired
	protected AuthenticationFlowManagement authFlowMan;

	protected ObjectMapper m = new ObjectMapper().findAndRegisterModules();
	{
		m.enable(SerializationFeature.INDENT_OUTPUT);
	}
	protected HttpHost host;

	protected HttpClient client;
	
	@BeforeEach
	public void setup() throws Exception
	{
		setupPasswordAuthn();

		Identity system_manager = createUsernameUserWithRole("System Manager");
		entityId = system_manager.getEntityId();
		entityEmail = "email@email.com";
		idsMan.addIdentity(new IdentityParam(EmailIdentity.ID, entityEmail), new EntityParam(system_manager));
		EntityParam entity = new EntityParam(entityId);
		String group = "/A";
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addMemberFromParent(group, entity);
		attrsMan.createAttribute(
			entity,
			new Attribute("sys:ProjectManagementRESTAPIRole", "enumeration", "/A", List.of("manager"))
		);

		deployEndpoint();
		client = getClient();
		host = getHost();
	}

	protected HttpHost getHost() {
		return new HttpHost("https", "localhost", 53458);
	}

	protected HttpClient getClient() throws Exception
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		return HttpUtils.createClient("https://localhost:53456", clientCfg);
	}

	protected HttpClientContext getClientContext(HttpHost host)
	{
		return getClientContext(host, DEF_USER, DEF_PASSWORD);
	}

	protected HttpClientContext getClientContext(HttpHost host, String user, String pass)
	{
		ContextBuilder cb = ContextBuilder.create();
		cb.preemptiveBasicAuth(host, new UsernamePasswordCredentials(user, pass.toCharArray()));
		return cb.build();
	}

	@Override
	protected void setupPasswordAuthn() throws EngineException
	{
		super.setupPasswordAuthn();

		Set<String> firstFactor = new HashSet<>();
		firstFactor.add(AUTHENTICATOR_REST_PASS);
		authnMan.createAuthenticator(AUTHENTICATOR_REST_PASS, "password", "", "credential1");
		authFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(AUTHENTICATION_FLOW_PASS, AuthenticationFlowDefinition.Policy.NEVER, firstFactor));
	}

	protected void deployEndpoint() throws Exception
	{
		AuthenticationRealm realm = new AuthenticationRealm("testr", "",
			10, 100, RememberMePolicy.disallow , 1, 600);
		realmsMan.addRealm(realm);

		List<String> authnCfg = new ArrayList<>();
		authnCfg.add(AUTHENTICATION_FLOW_PASS);
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("restUpman"),
			"desc", authnCfg, CONFIGURATION, realm.getName());
		endpointMan.deploy(RESTUpmanEndpoint.NAME, "restUpman", "/restupm", cfg);
		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		assertThat(endpoints.size()).isEqualTo(1);

		httpServer.start();
	}
}
