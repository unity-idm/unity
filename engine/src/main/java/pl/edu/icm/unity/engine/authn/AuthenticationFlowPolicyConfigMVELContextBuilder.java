/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.DynamicPolicyConfigurationMVELContextKey;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.authn.SigInInProgressContext;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@Component
class AuthenticationFlowPolicyConfigMVELContextBuilder
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN,
			AuthenticationFlowPolicyConfigMVELContextBuilder.class);

	private final AttributesHelper attributesHelper;
	private final EntityManagement identitiesMan;
	private final AttributeValueConverter attrConverter;
	private final TransactionalRunner tx;

	AuthenticationFlowPolicyConfigMVELContextBuilder(AttributesHelper attributesHelper,
			@Qualifier("insecure") EntityManagement identitiesMan, AttributeValueConverter attrConverter,
			TransactionalRunner tx)
	{
		this.attributesHelper = attributesHelper;
		this.identitiesMan = identitiesMan;
		this.attrConverter = attrConverter;
		this.tx = tx;
	}

	Map<String, Object> createMvelContext(AuthenticationOptionKey firstFactorOptionId,
			AuthenticationResult authenticationSuccessResult, boolean userOptIn, AuthenticationFlow authenticationFlow,
			SigInInProgressContext sigInInProgressContext) throws EngineException
	{

		EntityParam entityParam = new EntityParam(
				authenticationSuccessResult.getSuccessResult().authenticatedEntity.getEntityId());
		Entity entity = identitiesMan.getEntity(entityParam);
		List<String> resolvedGroups = identitiesMan.getGroupsForPresentation(entityParam)
				.stream()
				.map(group -> group.getName())
				.collect(Collectors.toList());
		Collection<AttributeExt> allAttributes = tx.runInTransactionRetThrowing(
				() -> attributesHelper.getAttributesInternal(entityParam.getEntityId(), true, "/", null, false));
		allAttributes = attributesHelper.filterSecuritySensitive(allAttributes);

		RemoteAuthnMetadata context = null;
		if (authenticationSuccessResult.isRemote())
		{
			context = authenticationSuccessResult.asRemote()
					.getSuccessResult()
					.getRemotelyAuthenticatedPrincipal()
					.getAuthnInput()
					.getRemoteAuthnMetadata();
		}

		AuthenticationFlowPolicyContextInput input = new AuthenticationFlowPolicyContextInput(entity, context,
				allAttributes, resolvedGroups, userOptIn, authenticationFlow, firstFactorOptionId,
				sigInInProgressContext);
		log.debug("Authentication flow policy context input:\n" + input.getTextDump());

		return setupContext(input);

	}

	private Map<String, Object> setupContext(AuthenticationFlowPolicyContextInput input) throws EngineException
	{
		Map<String, Object> ret = new HashMap<>();
		addAttributesToContext(DynamicPolicyConfigurationMVELContextKey.attr.name(),
				DynamicPolicyConfigurationMVELContextKey.attrObj.name(), ret, input.attributes(), attrConverter);
		ret.put(DynamicPolicyConfigurationMVELContextKey.idsByType.name(), getIdentitiesByType(input.entity()));
		ret.put(DynamicPolicyConfigurationMVELContextKey.groups.name(), input.groups());
		ret.putAll(getAuthnContextMvelVariables(input.context()));
		ret.put(DynamicPolicyConfigurationMVELContextKey.userOptIn.name(), input.userOptIn());
		ret.put(DynamicPolicyConfigurationMVELContextKey.authentication1F.name(), input.firstFactorOptionId()
				.getAuthenticatorKey());
		ret.put(DynamicPolicyConfigurationMVELContextKey.hasValid2FCredential.name(),
				hasValid2FCredential(input.entity(), input.authenticationFlow()));
		ret.put(DynamicPolicyConfigurationMVELContextKey.requestedACRs.name(),
				input.sigInInProgressContext() != null ? input.sigInInProgressContext()
						.acr()
						.getAll() : List.of());
		ret.put(DynamicPolicyConfigurationMVELContextKey.requestedEssentialACRs.name(),
				input.sigInInProgressContext() != null ? input.sigInInProgressContext()
						.acr()
						.essentialACRs() : List.of());

		log.trace("Created MVEL context for entity {}: {}", input.entity()
				.getId(), ret);

		return ret;
	}

	private Map<String, List<String>> getIdentitiesByType(Entity entity)
	{
		Map<String, List<String>> idsByType = new HashMap<>();
		for (Identity id : entity.getIdentities())
		{
			List<String> vals = idsByType.get(id.getTypeId());
			if (vals == null)
			{
				vals = new ArrayList<>();
				idsByType.put(id.getTypeId(), vals);
			}
			vals.add(id.getValue());
		}
		return idsByType;
	}

	private Map<String, Object> getAuthnContextMvelVariables(RemoteAuthnMetadata authnContext)
	{
		Map<String, Object> ret = new HashMap<>();

		List<String> acrs = new ArrayList<>();
		String upstreamProtocol = DynamicPolicyConfigurationMVELContextKey.DEFAULT_UPSTREAM_PROTOCOL;
		String upstreamIdP = null;

		if (authnContext != null)
		{
			acrs.addAll(authnContext.classReferences());
			upstreamIdP = authnContext.remoteIdPId();
			upstreamProtocol = authnContext.protocol()
					.name();
		}

		ret.put(DynamicPolicyConfigurationMVELContextKey.upstreamACRs.name(), acrs);
		ret.put(DynamicPolicyConfigurationMVELContextKey.upstreamProtocol.name(), upstreamProtocol);
		ret.put(DynamicPolicyConfigurationMVELContextKey.upstreamIdP.name(), upstreamIdP);
		return ret;
	}

	private void addAttributesToContext(String attrKey, String attrObjKey, Map<String, Object> ret,
			Collection<AttributeExt> attributes, AttributeValueConverter attrConverter)
			throws IllegalAttributeValueException
	{
		Map<String, Object> attr = new HashMap<>();
		Map<String, Object> attrObj = new HashMap<>();

		for (Attribute ra : attributes)
		{
			List<String> values = attrConverter.internalValuesToExternal(ra.getName(), ra.getValues());
			String v = values.isEmpty() ? "" : values.get(0);
			attr.put(ra.getName(), v);
			attrObj.put(ra.getName(),
					values.isEmpty() ? "" : attrConverter.internalValuesToObjectValues(ra.getName(), ra.getValues()));
		}
		ret.put(attrKey, attr);
		ret.put(attrObjKey, attrObj);
	}

	private boolean hasValid2FCredential(Entity entity, AuthenticationFlow authenticationFlow) throws EngineException
	{
		Map<String, CredentialPublicInformation> userCredentialsState = entity.getCredentialInfo()
				.getCredentialsState();

		for (AuthenticatorInstance authenticator : authenticationFlow.getSecondFactorAuthenticators())
		{
			if (authenticator.getMetadata()
					.getLocalCredentialName() != null)
			{
				if (userHasValidCredential(userCredentialsState, authenticator.getMetadata()
						.getLocalCredentialName()))
					return true;
			}
		}
		return false;
	}

	private boolean userHasValidCredential(Map<String, CredentialPublicInformation> userCredentialsState,
			String localCredentialName) throws EngineException
	{
		CredentialPublicInformation credentialPublicInformation = userCredentialsState.get(localCredentialName);
		if (credentialPublicInformation != null && credentialPublicInformation.getState()
				.equals(LocalCredentialState.correct))
		{
			return true;
		}

		return false;
	}

	private static record AuthenticationFlowPolicyContextInput(Entity entity, RemoteAuthnMetadata context,
			Collection<AttributeExt> attributes, List<String> groups, boolean userOptIn,
			AuthenticationFlow authenticationFlow, AuthenticationOptionKey firstFactorOptionId,
			SigInInProgressContext sigInInProgressContext)
	{
		String getTextDump()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("Entity " + entity.getId() + ":\n");
			for (Identity id : entity.getIdentities())
				sb.append(" - ")
						.append(id.toString())
						.append("\n");
			if (!attributes.isEmpty())
			{
				sb.append("Attributes:\n");
				for (Attribute at : attributes)
					sb.append(" - ")
							.append(at)
							.append("\n");
			}

			if (!groups.isEmpty())
			{
				sb.append("Groups: " + groups + "\n");
			}
			sb.append("UserOptIn:")
					.append(userOptIn)
					.append("\n");

			if (sigInInProgressContext != null && !sigInInProgressContext.acr()
					.getAll()
					.isEmpty())
			{
				sb.append("Requested voluntary ACRs: " + sigInInProgressContext.acr()
						.voluntaryACRs() + "\n");
				sb.append("Requested essential ACRs: " + sigInInProgressContext.acr()
						.essentialACRs() + "\n");
			}

			if (context != null)
			{
				sb.append("Upstream protocol: " + context.protocol()
						.name() + "\n");
				sb.append("Upstream ACRs: " + context.classReferences() + "\n");
				sb.append("Upstream IDP: " + context.remoteIdPId() + "\n");
			}

			return sb.toString();
		}

	}

}
