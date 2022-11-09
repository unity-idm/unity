/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp;

import io.imunity.idp.*;
import io.imunity.idp.IdPClientData.AccessStatus;
import io.imunity.idp.LastIdPClinetAccessAttributeManagement.LastIdPClientAccessKey;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.api.files.URIAccessException;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.processor.AuthnResponseProcessor;
import pl.edu.icm.unity.saml.idp.web.SamlAuthVaadinEndpoint;
import pl.edu.icm.unity.saml.idp.web.SamlIdPWebEndpointFactory;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.idpcommon.URIPresentationHelper;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
class SAMLTrustedApplicationManagement implements TrustedIdPClientsManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, AuthnResponseProcessor.class);
	
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
			SAMLServiceConfiguration config = new SAMLServiceConfiguration(samlEndpoint.getSpsConfiguration(), msg,
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

		public SAMLServiceConfiguration(TrustedServiceProviders trustedServiceProviders, MessageSource msg,
		                                URIAccessService uriAccessService)
		{
			individualTrustedSPs = new ArrayList<>();
			trustedServiceProviders.getSPConfigs().forEach(configuration ->
			{
				SAMLIndividualTrustedSPConfiguration idp = new SAMLIndividualTrustedSPConfiguration(msg,
						uriAccessService, configuration);
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
		                                     TrustedServiceProviderConfiguration configuration)
		{
			if (configuration.entityId != null)
			{

				id = configuration.entityId;
			} else
			{

				id = configuration.dnSamlId;
			}

			displayedName = configuration.name;

			if (configuration.logoUri != null)
			{
				String logoUri = configuration.logoUri.getDefaultLocaleValue(msg);
				logo = readLogo(imageAccessService, logoUri);
			} else
			{
				logo = null;
			}

			authorizedRedirectsUri = new ArrayList<>();
			if (configuration.returnUrl != null)
			{
				authorizedRedirectsUri.add(configuration.returnUrl);
			}
			authorizedRedirectsUri.addAll(configuration.returnUrls);

		}
		
		private byte[] readLogo(URIAccessService imageAccessService, String logoUri)
		{
			try
			{
				return imageAccessService.readURI(URI.create(logoUri)).getContents();
			} catch (URIAccessException e)
			{
				log.trace("Can not read logo URI", e);
				return null;
			}
		}
	}
}
