/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.CREDENTIAL;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.DEF_SIGN_REQUEST;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_BINDING;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_ID;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDP_PREFIX;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.METADATA_PATH;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.P;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.REQUESTER_ID;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableSet;

import eu.unicore.samly2.messages.XMLExpandedMessage;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.engine.api.authn.remote.RedirectedAuthnState;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultTranslator;
import pl.edu.icm.unity.engine.credential.SystemAllCredentialRequirements;
import pl.edu.icm.unity.engine.translation.in.action.MapIdentityActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

public class SAMLResponseVerificatorInegrationTest extends DBIntegrationTestBase
{
	@Autowired
	private RemoteAuthnResultTranslator translator;
	
	@Test
	public void shouldAcceptAuthnAssertionWithoutNameId() throws Exception
	{
		TranslationProfile profile = createTranslationProfile();

		SAMLResponseVerificator verificator = new SAMLResponseVerificator(
				mock(ReplayAttackChecker.class), 
				"https://192.168.0.10:2443/unitygw/spSAMLResponseConsumer", 
				translator);
		
		RemoteAuthnContext remoteAuthnState = createRemoteContext();
		
		AuthenticationResult authnResult = verificator.processResponse(remoteAuthnState, profile);
		
		assertThat(authnResult.getStatus()).isEqualTo(Status.success);
		IdentityParam identityParam = new IdentityParam(IdentifierIdentity.ID, "EMPTY", 
				"http://centos6-unity1:8080/simplesaml/saml2/idp/metadata.php", "testIP");
		identityParam.setConfirmationInfo(new ConfirmationInfo(false));
		assertThat(authnResult.asRemote().getRemotelyAuthenticatedPrincipal().getIdentities())
			.containsExactly(identityParam);
	}
	
	private RemoteAuthnContext createRemoteContext() throws Exception
	{
		SAMLSPProperties config = createConfig(); 
		RedirectedAuthnState baseState = new RedirectedAuthnState(
				new AuthenticationStepContext(new AuthenticationRealm(), 
						mock(AuthenticationFlow.class), 
						AuthenticationOptionKey.authenticatorOnlyKey("authnKey"), 
						FactorOrder.FIRST, 
						"endpointPath"), 
				null, 
				new LoginMachineDetails("ip", "os", "browser"), 
				"ultimateReturnURL", 
				AuthenticationTriggeringContext.authenticationTriggeredFirstFactor());
		RemoteAuthnContext ret = new RemoteAuthnContext(config, "entryKey", baseState);
		String response = loadResponse();
		ResponseDocument responseDocument = ResponseDocument.Factory.parse(response);
		ret.setResponse(response, Binding.HTTP_POST, new XMLExpandedMessage(responseDocument, 
				responseDocument.getResponse()));
		return ret;
	}
	
	private String loadResponse()
	{
		try
		{
			return FileUtils.readFileToString(
					new File("src/test/resources/responseDocWithoutSubjectNameId.xml"), 
					StandardCharsets.UTF_8);
		} catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private SAMLSPProperties createConfig() throws EngineException
	{
		Properties properties = new Properties();
		properties.setProperty(P+REQUESTER_ID, "http://unity/as/sp");
		properties.setProperty(P+DEF_SIGN_REQUEST, "true");
		properties.setProperty(P+CREDENTIAL, "MAIN");
		properties.setProperty(P+METADATA_PATH, "meta");
		properties.setProperty(P+IDP_PREFIX+"K1."+IDP_ID, "idp");
		properties.setProperty(P+IDP_PREFIX+"K1."+IDP_BINDING, "HTTP_POST");
		PKIManagement pkiMan = mock(PKIManagement.class);
		when(pkiMan.getCredentialNames()).thenReturn(ImmutableSet.of("MAIN"));
		return new TrustAllSPProperties(properties, pkiMan);
	}
	
	private TranslationProfile createTranslationProfile()
	{
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action = (TranslationAction) new TranslationAction(
				MapIdentityActionFactory.NAME,
				new String[] { IdentifierIdentity.ID, "id == empty ? 'EMPTY' : id", 
						SystemAllCredentialRequirements.NAME, 
						"CREATE_OR_MATCH" });
		rules.add(new TranslationRule("true", action));

		return new TranslationProfile("testIP", "",
				ProfileType.INPUT, ProfileMode.DEFAULT, rules);
	}
}