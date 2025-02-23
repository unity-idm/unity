/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.CredentialInfo;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstanceMetadata;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata.Protocol;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner.TxRunnableThrowingRet;

@ExtendWith(MockitoExtension.class)
public class AuthenticationFlowPolicyConfigMVELContextBuilderTest
{
	@Mock
	private AttributesHelper attributesHelper;
	@Mock
	private EntityManagement identitiesMan;
	@Mock
	private AttributeValueConverter attrConverter;
	@Mock
	private TransactionalRunner tx;
	@InjectMocks
	private AuthenticationFlowPolicyConfigMVELContextBuilder contextBuilder;

	@SuppressWarnings("unchecked")
	@Test
	public void shouldCreateContextWithAllVariables() throws EngineException
	{

		RemotelyAuthenticatedPrincipal remotelyAuthenticatedPrincipal = new RemotelyAuthenticatedPrincipal("idp",
				"profile");
		RemotelyAuthenticatedInput input = new RemotelyAuthenticatedInput("idp");
		input.setRemoteAuthnMetadata(new RemoteAuthnMetadata(Protocol.OIDC, "idp", List.of("acr1")));
		remotelyAuthenticatedPrincipal.setAuthnInput(input);
		AuthenticationResult result = RemoteAuthenticationResult.successful(remotelyAuthenticatedPrincipal,
				new AuthenticatedEntity(1L, "info", null), AuthenticationMethod.u_oauth);

		AuthenticatorInstanceMetadata meta = new AuthenticatorInstanceMetadata();
		meta.setLocalCredentialName("pass");

		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.DYNAMIC_EXPRESSION, null,
				List.of(new AuthenticatorImpl(null, null, meta)), null, 0);

		when(attributesHelper.getAttributesInternal(1L, true, "/", null, false))
				.thenReturn(List.of(new AttributeExt(new Attribute("a1", "syn", "/", List.of("v1")), false)));
		when(tx.runInTransactionRetThrowing(any()))
				.thenAnswer(i -> ((TxRunnableThrowingRet<?>) i.getArguments()[0]).run());
		when(identitiesMan.getEntity(new EntityParam(1L)))
				.thenReturn(new Entity(List.of(new Identity("type", "val", 1L, "val")), new EntityInformation(1), new CredentialInfo("id",
						Map.of("pass", new CredentialPublicInformation(LocalCredentialState.correct, null)))));
		when(attrConverter.internalValuesToExternal("a1", List.of("v1"))).thenReturn(List.of("v1"));
		when(identitiesMan.getGroupsForPresentation(new EntityParam(1L)))
				.thenReturn(List.of(new Group("/g1")));
		when(attrConverter.internalValuesToObjectValues("a1", List.of("v1"))).thenAnswer(new Answer<List<?>>()
		{
			@Override
			public List<?> answer(InvocationOnMock invocation) throws Throwable
			{
				return List.of("v1");
			}
		});

		Map<String, Object> mvelContext = contextBuilder.createMvelContext(new AuthenticationOptionKey("akey", "op"),
				result, true, flow);

		assertThat(mvelContext.get("userOptIn")).isEqualTo(true);
		assertThat(mvelContext.get("hasValid2FCredential")).isEqualTo(true);
		assertThat(mvelContext.get("authentication1F")).isEqualTo("akey");
		assertThat(((Map<String, Object>) mvelContext.get("attr")).get("a1")).isEqualTo("v1");
		assertThat(((Map<String, Object>) mvelContext.get("attrObj")).get("a1")).isEqualTo(List.of("v1"));
		assertThat(((Map<String, Object>) mvelContext.get("idsByType")).get("type")).isEqualTo(List.of("val"));
		assertThat(mvelContext.get("groups")).isEqualTo(List.of("/g1"));
		assertThat(mvelContext.get("upstreamACRs")).isEqualTo(List.of("acr1"));
		assertThat(mvelContext.get("upstreamIdP")).isEqualTo("idp");
		assertThat(mvelContext.get("upstreamProtocol")).isEqualTo("OIDC");

	}

}
