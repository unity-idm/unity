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
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.authn.DynamicPolicyConfigurationMVELContextKey;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

@Component
public class AuthenticationFlowPolicyConfigMVELContextBuilder
{
	private final AttributesHelper attributesHelper;
	private final EntityManagement identitiesMan;
	private final GroupsManagement groupManagement;
	private final AttributeValueConverter attrConverter;
	private final TransactionalRunner tx;

	public AuthenticationFlowPolicyConfigMVELContextBuilder(AttributesHelper attributesHelper,
			@Qualifier("insecure") EntityManagement identitiesMan,
			@Qualifier("insecure") GroupsManagement groupManagement, AttributeValueConverter attrConverter,
			TransactionalRunner tx)
	{
		this.attributesHelper = attributesHelper;
		this.identitiesMan = identitiesMan;
		this.groupManagement = groupManagement;
		this.attrConverter = attrConverter;
		this.tx = tx;
	}

	public Map<String, Object> createMvelContext(AuthenticationOptionKey firstFactorOptionId,
			AuthenticationResult authenticationResult, boolean userOptIn, AuthenticationFlow authenticationFlow)
			throws EngineException
	{
		Map<String, Object> ret = new HashMap<>();

		EntityParam entity = new EntityParam(authenticationResult.getSuccessResult().authenticatedEntity.getEntityId());
		Set<String> allGroups = identitiesMan.getGroups(entity)
				.keySet();
		List<Group> resolvedGroups = groupManagement.getGroupsByWildcard("/**")
				.stream()
				.filter(grp -> allGroups.contains(grp.getName()))
				.collect(Collectors.toList());
		Collection<AttributeExt> allAttributes = tx.runInTransactionRetThrowing(
				() -> attributesHelper.getAttributesInternal(entity.getEntityId(), true, "/", null, false));

		addAttributesToContext(DynamicPolicyConfigurationMVELContextKey.attr.name(),
				DynamicPolicyConfigurationMVELContextKey.attrObj.name(), ret, allAttributes, attrConverter);

		Map<String, List<String>> idsByType = new HashMap<>();
		for (Identity id : identitiesMan.getEntity(entity)
				.getIdentities())
		{
			List<String> vals = idsByType.get(id.getTypeId());
			if (vals == null)
			{
				vals = new ArrayList<>();
				idsByType.put(id.getTypeId(), vals);
			}
			vals.add(id.getValue());
		}
		ret.put(DynamicPolicyConfigurationMVELContextKey.idsByType.name(), idsByType);

		List<String> groupNames = resolvedGroups.stream()
				.map(group -> group.getName())
				.collect(Collectors.toList());
		ret.put(DynamicPolicyConfigurationMVELContextKey.groups.name(), groupNames);

		RemoteAuthnMetadata context = null;
		if (authenticationResult.isRemote())
		{
			context = authenticationResult.asRemote()
					.getSuccessResult()
					.getRemotelyAuthenticatedPrincipal()
					.getAuthnInput()
					.getRemoteAuthnMetadata();
		}
		ret.putAll(getAuthnContextMvelVariables(context));
		ret.put(DynamicPolicyConfigurationMVELContextKey.userOptIn.name(), userOptIn);
		ret.put(DynamicPolicyConfigurationMVELContextKey.authentication1F.name(),
				firstFactorOptionId.getAuthenticatorKey());
		ret.put(DynamicPolicyConfigurationMVELContextKey.hasValid2FCredential.name(),
				hasValid2FCredential(entity, authenticationFlow));

		return ret;
	}

	private static Map<String, Object> getAuthnContextMvelVariables(RemoteAuthnMetadata authnContext)
	{
		Map<String, Object> ret = new HashMap<>();

		List<String> acrs = new ArrayList<>();
		String upstreamProtocol = DynamicPolicyConfigurationMVELContextKey.DEFAULT_UPSTREAM_PROTOCOL;
		String upstreamIdP = null;

		if (authnContext != null)
		{
			acrs.addAll(authnContext.classReferences());
			upstreamIdP = authnContext.remoteIdPId();
			upstreamProtocol = authnContext.protocol().name();
		}

		ret.put(DynamicPolicyConfigurationMVELContextKey.upstreamACRs.name(), acrs);
		ret.put(DynamicPolicyConfigurationMVELContextKey.upstreamProtocol.name(), upstreamProtocol);
		ret.put(DynamicPolicyConfigurationMVELContextKey.upstreamIdP.name(), upstreamIdP);
		return ret;
	}

	private static void addAttributesToContext(String attrKey, String attrObjKey, Map<String, Object> ret,
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

	private boolean hasValid2FCredential(EntityParam entity, AuthenticationFlow authenticationFlow)
			throws EngineException
	{
		Map<String, CredentialPublicInformation> userCredentialsState = identitiesMan.getEntity(entity)
				.getCredentialInfo()
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
}
