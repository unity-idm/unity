/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.idp.AccessProtocol;
import io.imunity.idp.ApplicationId;
import io.imunity.idp.IdPClientData;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement.LastIdPClientAccessKey;
import io.imunity.idp.TrustedIdPClientsManagement;
import io.imunity.idp.IdPClientData.AccessStatus;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint;
import pl.edu.icm.unity.saml.idp.web.SamlIdPWebEndpointFactory;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.idpcommon.URIPresentationHelper;

@Component
class SAMLTrustedApplicationManagement implements TrustedIdPClientsManagement
{
	private final PreferencesManagement preferencesManagement;
	private final EndpointManagement endpointManagement;
	private final MessageSource msg;
	private final URIAccessService uriAccessService;
	private final LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement;

	SAMLTrustedApplicationManagement(@Qualifier("insecure") PreferencesManagement preferencesManagement,
			@Qualifier("insecure") EndpointManagement endpointManagement, MessageSource msg,
			URIAccessService uriAccessService, LastIdPClinetAccessAttributeManagement lastAccessAttributeManagement)
	{
		this.preferencesManagement = preferencesManagement;
		this.endpointManagement = endpointManagement;
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.lastAccessAttributeManagement = lastAccessAttributeManagement;
	}

	@Override
	public List<IdPClientData> getIdpClientsData() throws EngineException
	{
		List<SAMLServiceConfiguration> services = getServices();
		SamlPreferences preferences = getPreferences();
		List<IdPClientData> ret = new ArrayList<>();

		for (SAMLServiceConfiguration service : services)
		{
			for (SAMLIndividualTrustedSPConfiguration client : service.individualTrustedSPs)
			{
				if (preferences.getKeys().contains(client.id))
				{
					ret.add(IdPClientData.builder().withApplicationId(new ApplicationId(client.id))
							.withApplicationName(getApplicationName(client)).withLogo(Optional.ofNullable(client.logo))
							.withApplicationDomain(Optional.of(
									URIPresentationHelper.getHumanReadableDomain(client.authorizedRedirectsUri.get(0))))
							.withAccessStatus(preferences.getSPSettings(client.id).isDefaultAccept()
									? AccessStatus.allowWithoutAsking
									: AccessStatus.disallowWithoutAsking)
							.withAccessGrantTime(
									Optional.ofNullable(!preferences.getSPSettings(client.id).isDefaultAccept() ? null
											: preferences.getSPSettings(client.id).getTimestamp()))
							.withLastAccessTime(Optional.ofNullable(getLastAccessByClient()
									.get(new LastIdPClientAccessKey(AccessProtocol.SAML, client.id))))
							.withAccessProtocol(AccessProtocol.SAML).build());
				}
			}
		}

		return ret;
	}

	@Override
	public synchronized void unblockAccess(ApplicationId appId) throws EngineException
	{
		clearPreferences(appId.id);
	}

	@Override
	public synchronized void revokeAccess(ApplicationId appId) throws EngineException
	{
		clearPreferences(appId.id);
	}

	private String getApplicationName(SAMLIndividualTrustedSPConfiguration client)
	{
		return client.displayedName != null && client.displayedName.getValue(msg) != null
				? client.displayedName.getValue(msg)
				: client.id;
	}

	private void clearPreferences(String appId) throws EngineException
	{
		SamlPreferences preferences = getPreferences();
		preferences.removeSPSettings(appId);
		SamlPreferences.savePreferences(preferencesManagement, preferences);
	}

	@Override
	public AccessProtocol getSupportedProtocol()
	{
		return AccessProtocol.SAML;
	}

	private List<SAMLServiceConfiguration> getServices() throws AuthorizationException
	{
		List<SAMLServiceConfiguration> ret = new ArrayList<>();
		for (EndpointInstance endpoint : endpointManagement.getDeployedEndpointInstances().stream().filter(e -> e
				.getEndpointDescription().getEndpoint().getTypeId().equals(SamlIdPWebEndpointFactory.TYPE.getName()))
				.collect(Collectors.toList()))
		{
			SamlAuthVaadinEndpoint samlEndpoint = (SamlAuthVaadinEndpoint) endpoint;
			SAMLServiceConfiguration config = new SAMLServiceConfiguration(samlEndpoint.getVirtualConfiguration(), msg,
					uriAccessService);
			ret.add(config);
		}

		return ret;
	}

	private SamlPreferences getPreferences() throws EngineException
	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		EntityParam entityParam = new EntityParam(entity.getEntityId());
		String pref = preferencesManagement.getPreference(entityParam, SamlPreferences.ID);
		SamlPreferences preferences = new SamlPreferences();
		if (pref != null)
			preferences.setSerializedConfiguration(JsonUtil.parse(pref));
		return preferences;
	}

	private Map<LastIdPClientAccessKey, Instant> getLastAccessByClient() throws EngineException
	{
		return lastAccessAttributeManagement.getLastAccessByClient();

	}

	private static class SAMLServiceConfiguration
	{
		public final List<SAMLIndividualTrustedSPConfiguration> individualTrustedSPs;

		public SAMLServiceConfiguration(SamlProperties samlIdpProperties, MessageSource msg,
				URIAccessService uriAccessService)
		{
			Set<String> spKeys = samlIdpProperties.getStructuredListKeys(SamlIdpProperties.ALLOWED_SP_PREFIX);
			individualTrustedSPs = new ArrayList<>();
			spKeys.forEach(key ->
			{
				key = key.substring(SamlIdpProperties.ALLOWED_SP_PREFIX.length(), key.length() - 1);
				SAMLIndividualTrustedSPConfiguration idp = new SAMLIndividualTrustedSPConfiguration(msg,
						uriAccessService, samlIdpProperties, key);
				individualTrustedSPs.add(idp);
			});
		}
	}

	private static class SAMLIndividualTrustedSPConfiguration
	{
		public final String id;
		public final I18nString displayedName;
		public final byte[] logo;
		public final ArrayList<String> authorizedRedirectsUri;

		SAMLIndividualTrustedSPConfiguration(MessageSource msg, URIAccessService imageAccessService,
				SamlProperties source, String name)
		{
			String prefix = SamlIdpProperties.ALLOWED_SP_PREFIX + name + ".";

			if (source.isSet(prefix + SamlIdpProperties.ALLOWED_SP_ENTITY))
			{

				id = source.getValue(prefix + SamlIdpProperties.ALLOWED_SP_ENTITY);
			} else
			{

				id = source.getValue(prefix + SamlIdpProperties.ALLOWED_SP_DN);
			}

			displayedName = source.getLocalizedStringWithoutFallbackToDefault(msg,
					prefix + SamlIdpProperties.ALLOWED_SP_NAME);

			if (source.isSet(prefix + SamlIdpProperties.ALLOWED_SP_LOGO))
			{
				String logoUri = source.getValue(prefix + SamlIdpProperties.ALLOWED_SP_LOGO);
				logo = imageAccessService.readURI(URI.create(logoUri)).getContents();
			} else
			{
				logo = null;
			}

			authorizedRedirectsUri = new ArrayList<>();
			if (source.isSet(prefix + SamlIdpProperties.ALLOWED_SP_RETURN_URL))
			{
				authorizedRedirectsUri.add(source.getValue(prefix + SamlIdpProperties.ALLOWED_SP_RETURN_URL));
			}

			List<String> uris = source.getListOfValues(prefix + SamlIdpProperties.ALLOWED_SP_RETURN_URLS);
			uris.forEach(c ->
			{
				authorizedRedirectsUri.add(c);
			});

		}
	}
}
