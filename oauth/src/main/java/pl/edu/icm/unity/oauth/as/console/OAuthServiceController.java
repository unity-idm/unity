/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.client.ClientType;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.attr.ImageType;
import pl.edu.icm.unity.attr.UnityImage;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.EntityInGroupData;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.ImageAttribute;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceDefinition;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;
import pl.edu.icm.unity.webui.console.services.idp.IdpServiceController;
import pl.edu.icm.unity.webui.console.services.idp.IdpUsersHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for Auth service. Responsible for creating and updating full oauth
 * service - pair of {@link OAuthAuthzWebEndpoint} and
 * {@link OAuthTokenEndpoint}.
 * 
 * @author P.Piernik
 *
 */
@Component
class OAuthServiceController implements IdpServiceController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, OAuthServiceController.class);
	public static final String DEFAULT_CREDENTIAL = "sys:password";
	public static final String IDP_CLIENT_MAIN_GROUP = "/IdPs";
	public static final String OAUTH_CLIENTS_SUBGROUP = "oauth-clients";

	private MessageSource msg;
	private EndpointManagement endpointMan;
	private RealmsManagement realmsMan;
	private AuthenticationFlowManagement flowsMan;
	private AuthenticatorManagement authMan;
	private AttributeTypeManagement atMan;
	private BulkGroupQueryService bulkService;
	private RegistrationsManagement registrationMan;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private AuthenticatorSupportService authenticatorSupportService;
	private IdentityTypeSupport idTypeSupport;
	private PKIManagement pkiMan;
	private AdvertisedAddressProvider advertisedAddrProvider;
	private OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory;
	private AttributeTypeSupport attrTypeSupport;
	private AttributesManagement attrMan;
	private EntityManagement entityMan;
	private GroupsManagement groupMan;
	private EntityCredentialManagement entityCredentialManagement;
	private IdpUsersHelper idpUsersHelper;
	private ImageAccessService imageService;
	private PolicyDocumentManagement policyDocumentManagement;
	private NetworkServer server;
	private final EndpointFileConfigurationManagement serviceFileConfigController;

	@Autowired
	OAuthServiceController(MessageSource msg,
			EndpointManagement endpointMan,
			RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan,
			AuthenticatorManagement authMan,
			AttributeTypeManagement atMan,
			BulkGroupQueryService bulkService,
			RegistrationsManagement registrationMan,
			URIAccessService uriAccessService,
			FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig,
			AuthenticatorSupportService authenticatorSupportService,
			PKIManagement pkiMan,
			NetworkServer server,
			AdvertisedAddressProvider advertisedAddrProvider,
			IdentityTypeSupport idTypeSupport,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory,
			AttributeTypeSupport attrTypeSupport,
			AttributesManagement attrMan,
			EntityManagement entityMan,
			GroupsManagement groupMan,
			EntityCredentialManagement entityCredentialManagement,
			ImageAccessService imageService,
			IdpUsersHelper idpUsersHelper,
			PolicyDocumentManagement policyDocumentManagement,
			EndpointFileConfigurationManagement serviceFileConfigController)
	{
		this.msg = msg;
		this.endpointMan = endpointMan;
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
		this.atMan = atMan;
		this.bulkService = bulkService;
		this.registrationMan = registrationMan;
		this.uriAccessService = uriAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.pkiMan = pkiMan;
		this.advertisedAddrProvider = advertisedAddrProvider;
		this.idTypeSupport = idTypeSupport;
		this.outputTranslationProfileFieldFactory = outputTranslationProfileFieldFactory;
		this.attrTypeSupport = attrTypeSupport;
		this.attrMan = attrMan;
		this.entityMan = entityMan;
		this.groupMan = groupMan;
		this.entityCredentialManagement = entityCredentialManagement;
		this.imageService = imageService;
		this.idpUsersHelper = idpUsersHelper;
		this.server = server;
		this.policyDocumentManagement = policyDocumentManagement;
		this.serviceFileConfigController = serviceFileConfigController;
	}

	@Override
	public List<ServiceDefinition> getServices() throws ControllerException
	{
		List<ServiceDefinition> ret = new ArrayList<>();
		try
		{
			for (Endpoint endpoint : endpointMan.getEndpoints().stream()
					.filter(e -> e.getTypeId().equals(OAuthAuthzWebEndpoint.Factory.TYPE.getName()))
					.collect(Collectors.toList()))
			{
				DefaultServiceDefinition oauthWebService = getServiceDef(endpoint);
				oauthWebService.setBinding(OAuthAuthzWebEndpoint.Factory.TYPE.getSupportedBinding());
				DefaultServiceDefinition tokenService = getTokenService(
						endpoint.getConfiguration().getTag());
				if (tokenService != null)
				{
					ret.add(new OAuthServiceDefinition(oauthWebService, tokenService));
				}
			}
			return ret;

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.getAllError"), e);
		}
	}

	private DefaultServiceDefinition getTokenService(String tag) throws EngineException
	{
		List<Endpoint> matchingTokenEndpoints = endpointMan.getEndpoints().stream()
				.filter(e -> e.getTypeId().equals(OAuthTokenEndpoint.TYPE.getName())
						&& e.getConfiguration().getTag().equals(tag))
				.collect(Collectors.toList());
		if (matchingTokenEndpoints.isEmpty())
		{
			log.warn("Can not find a corresponding token endpoint for OAuth AS endpoint with tag {}", tag);
			return null;
		}
		if (matchingTokenEndpoints.size() > 1)
		{
			log.warn("Found {} token endpoints for OAuth AS endpoint with tag {}", 
					matchingTokenEndpoints.size(), tag);
			return null;
		}

		DefaultServiceDefinition tokenService = getServiceDef(matchingTokenEndpoints.get(0));
		tokenService.setBinding(OAuthTokenEndpoint.TYPE.getSupportedBinding());
		return tokenService;
	}

	private DefaultServiceDefinition getServiceDef(Endpoint endpoint)
	{
		DefaultServiceDefinition serviceDef = new DefaultServiceDefinition(endpoint.getTypeId());
		serviceDef.setName(endpoint.getName());
		serviceDef.setAddress(endpoint.getContextAddress());
		serviceDef.setConfiguration(endpoint.getConfiguration().getConfiguration());
		serviceDef.setAuthenticationOptions(endpoint.getConfiguration().getAuthenticationOptions());
		serviceDef.setDisplayedName(endpoint.getConfiguration().getDisplayedName());
		serviceDef.setRealm(endpoint.getConfiguration().getRealm());
		serviceDef.setDescription(endpoint.getConfiguration().getDescription());
		serviceDef.setState(endpoint.getState());
		serviceDef.setSupportsConfigReloadFromFile(serviceFileConfigController.getEndpointConfigKey(endpoint.getName()).isPresent());
		return serviceDef;
	}

	@Override
	public ServiceDefinition getService(String name) throws ControllerException
	{
		try
		{
			Endpoint endpoint = endpointMan.getEndpoints().stream()
					.filter(e -> e.getName().equals(name) && e.getTypeId()
							.equals(OAuthAuthzWebEndpoint.Factory.TYPE.getName()))
					.findFirst().orElse(null);

			if (endpoint == null)
				return null;

			DefaultServiceDefinition oauthWebService = getServiceDef(endpoint);
			oauthWebService.setBinding(OAuthAuthzWebEndpoint.Factory.TYPE.getSupportedBinding());
			OAuthServiceDefinition def = new OAuthServiceDefinition(oauthWebService,
					getTokenService(endpoint.getConfiguration().getTag()));
			def.setClientsSupplier(this::getOAuthClients);
			return def;
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.getError", name), e);
		}
	}

	@Override
	public void deploy(ServiceDefinition service) throws ControllerException
	{
		OAuthServiceDefinition def = (OAuthServiceDefinition) service;
		DefaultServiceDefinition webAuthzService = def.getWebAuthzService();
		DefaultServiceDefinition tokenService = def.getTokenService();
		String tag = UUID.randomUUID().toString();
		try
		{
			EndpointConfiguration wconfig = new EndpointConfiguration(webAuthzService.getDisplayedName(),
					webAuthzService.getDescription(), webAuthzService.getAuthenticationOptions(),
					webAuthzService.getConfiguration(), webAuthzService.getRealm(), tag);
			endpointMan.deploy(webAuthzService.getType(), webAuthzService.getName(),
					webAuthzService.getAddress(), wconfig);
			if (tokenService != null)
			{
				EndpointConfiguration rconfig = new EndpointConfiguration(
						tokenService.getDisplayedName(), tokenService.getDescription(),
						tokenService.getAuthenticationOptions(),
						tokenService.getConfiguration(), tokenService.getRealm(), tag);
				endpointMan.deploy(tokenService.getType(), tokenService.getName(),
						tokenService.getAddress(), rconfig);
			}

			if (groupMan.getChildGroups("/").stream().map(g -> g.toString())
					.filter(g -> g.equals(IDP_CLIENT_MAIN_GROUP)).count() == 0)
			{
				groupMan.addGroup(new Group(IDP_CLIENT_MAIN_GROUP));
			}

			createClientsGroup(def);

			if (def.getSelectedClients() != null)
				updateClients(def.getSelectedClients());
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("ServicesController.deployError", webAuthzService.getName()), e);
		}

	}

	private void createClientsGroup(OAuthServiceDefinition def) throws EngineException
	{
		String autoGeneratedClientsGroup = def.getAutoGeneratedClientsGroup();
		if (autoGeneratedClientsGroup == null)
			return;
		Group serviceClientGroup = new Group(def.getAutoGeneratedClientsGroup());
		serviceClientGroup.setDisplayedName(new I18nString(def.getWebAuthzService().getName()));
		groupMan.addGroup(serviceClientGroup);
		Group serviceClientGroupOAuth = new Group(
				def.getAutoGeneratedClientsGroup() + "/" + OAUTH_CLIENTS_SUBGROUP);
		serviceClientGroupOAuth.setDisplayedName(new I18nString(OAUTH_CLIENTS_SUBGROUP));
		groupMan.addGroup(serviceClientGroupOAuth);
	}
	
	@Override
	public void undeploy(ServiceDefinition service) throws ControllerException
	{
		OAuthServiceDefinition def = (OAuthServiceDefinition) service;
		DefaultServiceDefinition webAuthzService = def.getWebAuthzService();
		DefaultServiceDefinition tokenService = def.getTokenService();

		try
		{
			endpointMan.undeploy(webAuthzService.getName());
			if (tokenService != null)
			{
				endpointMan.undeploy(tokenService.getName());
			}

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("ServicesController.undeployError", webAuthzService.getName()),
					e);
		}
	}

	@Override
	public void update(ServiceDefinition service) throws ControllerException
	{
		OAuthServiceDefinition def = (OAuthServiceDefinition) service;
		DefaultServiceDefinition webAuthzService = def.getWebAuthzService();
		DefaultServiceDefinition tokenService = def.getTokenService();
		String tag = UUID.randomUUID().toString();
		try
		{
			EndpointConfiguration wconfig = new EndpointConfiguration(webAuthzService.getDisplayedName(),
					webAuthzService.getDescription(), webAuthzService.getAuthenticationOptions(),
					webAuthzService.getConfiguration(), webAuthzService.getRealm(), tag);
			endpointMan.updateEndpoint(webAuthzService.getName(), wconfig);
			if (tokenService != null)
			{
				EndpointConfiguration rconfig = new EndpointConfiguration(
						tokenService.getDisplayedName(), tokenService.getDescription(),
						tokenService.getAuthenticationOptions(),
						tokenService.getConfiguration(), tokenService.getRealm(), tag);
				endpointMan.updateEndpoint(tokenService.getName(), rconfig);
			}
			updateClients(def.getSelectedClients());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.updateError", def.getName()),
					e);
		}

	}
	
	@Override
	public void reloadConfigFromFile(ServiceDefinition service) throws ControllerException
	{
		OAuthServiceDefinition def = (OAuthServiceDefinition) service;
		DefaultServiceDefinition webAuthzService = def.getWebAuthzService();
		DefaultServiceDefinition tokenService = def.getTokenService();
		
		ControllerException ex = null;
		try
		{
			endpointMan.updateEndpoint(webAuthzService.getName(),
					serviceFileConfigController.getEndpointConfig(webAuthzService.getName()));
		} catch (Exception e)
		{
			ex = new ControllerException(msg.getMessage("ServicesController.updateError", def.getName()), e);
		}
		
		try
		{
			if (tokenService != null)
			{
				endpointMan.updateEndpoint(tokenService.getName(),
						serviceFileConfigController.getEndpointConfig(tokenService.getName()));
			}
		} catch (Exception e)
		{
			ex = new ControllerException(msg.getMessage("ServicesController.updateError", def.getName()), e);
		}

		if (ex != null)
		{
			throw ex;
		}
	}

	private void updateClients(List<OAuthClient> clients) throws EngineException, URISyntaxException
	{
		String clientNameAttr = idpUsersHelper.getClientNameAttr();

		for (OAuthClient client : clients)
		{
			if (client.getEntity() == null)
			{
				Long id = addOAuthClient(client);
				OAuthClient clone = client.clone();
				clone.setEntity(id);
				updateClient(clone, clientNameAttr);
			} else if (client.isToRemove())
			{
				EntityParam entity = new EntityParam(client.getEntity());
				String group = client.getGroup();
				if (group.equals("/"))
				{
					entityMan.removeEntity(entity);
				} else
				{

					Set<String> entityGroups = entityMan.getGroups(entity).keySet();
					entityGroups.remove("/");
					entityGroups.remove(IDP_CLIENT_MAIN_GROUP);
					entityGroups.remove(group);
					if (!entityGroups.isEmpty())
					{
						groupMan.removeMember(client.getGroup(), entity);
					} else
					{
						entityMan.removeEntity(entity);
					}
				}
			} else if (client.isUpdated())
			{
				updateClient(client, clientNameAttr);
			}
		}
	}

	private long addOAuthClient(OAuthClient client) throws EngineException
	{
		IdentityParam id = new IdentityParam(UsernameIdentity.ID, client.getId());
		Identity addEntity = entityMan.addEntity(id, EntityState.valid);
		Deque<String> notMember = Group.getMissingGroups(client.getGroup(), Arrays.asList("/"));
		addToGroupRecursive(notMember, addEntity.getEntityId());
		return addEntity.getEntityId();
	}

	private void addToGroupRecursive(final Deque<String> notMember, long entity) throws EngineException
	{
		if (notMember.isEmpty())
			return;
		String current = notMember.pollLast();
		groupMan.addMemberFromParent(current, new EntityParam(entity));
		addToGroupRecursive(notMember, entity);
	}

	private void updateClient(OAuthClient client, String clientNameAttr) throws EngineException, URISyntaxException
	{

		EntityParam entity = new EntityParam(client.getEntity());
		String group = client.getGroup();

		LocalOrRemoteResource logoResource = client.getLogo();
		if (logoResource != null)
		{
			if (logoResource.getLocal() != null)
			{
				updateLogo(entity, group, logoResource.getLocal());
			} else if (logoResource.getRemote() != null)
			{
				FileData data = uriAccessService.readURI(new URI(logoResource.getRemote()));
				updateLogo(entity, group, data.getContents());
			} else
			{
				attrMan.removeAttribute(entity, group, OAuthSystemAttributesProvider.CLIENT_LOGO);
			}

		}
		if (client.getFlows() != null)
		{
			Attribute flows = EnumAttribute.of(OAuthSystemAttributesProvider.ALLOWED_FLOWS, group,
					client.getFlows());
			attrMan.setAttribute(entity, flows);
		}
		
		if (!client.isAllowAnyScopes())
		{
			Attribute scopes = StringAttribute.of(OAuthSystemAttributesProvider.ALLOWED_SCOPES, group,
					client.getScopes() != null ? client.getScopes() : Collections.emptyList());
			attrMan.setAttribute(entity, scopes);
		}else
		{
			if (attrMan.getAttributes(entity, group, OAuthSystemAttributesProvider.ALLOWED_SCOPES).size() > 0)
			{
				attrMan.removeAttribute(entity, group, OAuthSystemAttributesProvider.ALLOWED_SCOPES);
			}
		}

		if (client.getTitle() != null)
		{
			Attribute title = StringAttribute.of(OAuthSystemAttributesProvider.CLIENT_NAME, group,
					client.getTitle());
			attrMan.setAttribute(entity, title);
		}

		if (client.getType() != null)
		{
			Attribute type = EnumAttribute.of(OAuthSystemAttributesProvider.CLIENT_TYPE, group,
					client.getType());
			attrMan.setAttribute(entity, type);
		}

		if (client.getRedirectURIs() != null)
		{
			Attribute uris = StringAttribute.of(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI, group,
					client.getRedirectURIs());
			attrMan.setAttribute(entity, uris);
		}

		if (client.getName() != null && clientNameAttr != null)
		{
			Attribute name = StringAttribute.of(clientNameAttr, "/", client.getName());
			attrMan.setAttribute(entity, name);
		}

		if (!client.getType().equals(ClientType.PUBLIC.toString()))
		{
			if (client.getSecret() != null && !client.getSecret().isEmpty())
			{
				entityCredentialManagement.setEntityCredential(entity, DEFAULT_CREDENTIAL,
						new PasswordToken(client.getSecret()).toJson());
			}
		} else
		{
			entityCredentialManagement.setEntityCredentialStatus(entity, DEFAULT_CREDENTIAL,
					LocalCredentialState.notSet);
		}
	}

	private void updateLogo(EntityParam entity, String group, byte[] value) throws EngineException
	{
		ImageAttributeSyntax syntax = (ImageAttributeSyntax) attrTypeSupport
				.getSyntax(attrTypeSupport.getType(OAuthSystemAttributesProvider.CLIENT_LOGO));
		UnityImage image = new UnityImage(value, ImageType.JPG);
		image.scaleDown(syntax.getConfig().getMaxWidth(), syntax.getConfig().getMaxHeight());

		Attribute logoAttr = ImageAttribute.of(OAuthSystemAttributesProvider.CLIENT_LOGO, group, image);
		attrMan.setAttribute(entity, logoAttr);
	}

	@Override
	public void remove(ServiceDefinition service) throws ControllerException
	{
		OAuthServiceDefinition def = (OAuthServiceDefinition) service;
		DefaultServiceDefinition webAuthzService = def.getWebAuthzService();
		DefaultServiceDefinition tokenService = def.getTokenService();

		try
		{
			endpointMan.removeEndpoint(webAuthzService.getName());
			if (tokenService != null)
			{
				endpointMan.removeEndpoint(tokenService.getName());
			}

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("ServicesController.removeError", webAuthzService.getName()), e);
		}
	}

	@Override
	public String getSupportedEndpointType()
	{
		return OAuthAuthzWebEndpoint.Factory.TYPE.getName();
	}

	private List<OAuthClient> getOAuthClients(String group)
	{
		try
		{
			List<OAuthClient> clients = new ArrayList<>();

			Map<Long, EntityInGroupData> membershipInfo = bulkService
					.getMembershipInfo(bulkService.getBulkMembershipData(group));
			String nameAttr = idpUsersHelper.getClientNameAttr();

			for (EntityInGroupData member : membershipInfo.values())
			{
				if (isOAuthClient(member))
					clients.add(getOAuthClient(member, group, nameAttr));
			}
			return clients;
		} catch (EngineException e)
		{
			throw new RuntimeEngineException(e);
		}
	}

	private boolean isOAuthClient(EntityInGroupData candidate)
	{
		return candidate.groupAttributesByName.keySet().contains(OAuthSystemAttributesProvider.ALLOWED_FLOWS)
				&& getUserName(candidate.entity.getIdentities()) != null;
	}

	private OAuthClient getOAuthClient(EntityInGroupData info, String group, String nameAttr) throws EngineException
	{
		OAuthClient c = new OAuthClient();
		c.setEntity(info.entity.getId());
		c.setId(getUserName(info.entity.getIdentities()));
		c.setGroup(group);
		Map<String, AttributeExt> attrs = info.groupAttributesByName;

		c.setFlows(attrs.get(OAuthSystemAttributesProvider.ALLOWED_FLOWS).getValues());

		if (attrs.containsKey(OAuthSystemAttributesProvider.ALLOWED_SCOPES))
		{
			c.setScopes(attrs.get(OAuthSystemAttributesProvider.ALLOWED_SCOPES).getValues());
			c.setAllowAnyScopes(false);
		} else
		{
			c.setAllowAnyScopes(true);
		}
		
		if (attrs.containsKey(OAuthSystemAttributesProvider.CLIENT_TYPE))
		{
			c.setType(attrs.get(OAuthSystemAttributesProvider.CLIENT_TYPE).getValues().get(0));
		} else
		{
			c.setType(ClientType.CONFIDENTIAL.toString());
		}

		if (attrs.containsKey(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI))
		{
			c.setRedirectURIs(attrs.get(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI).getValues());
		}

		if (attrs.containsKey(OAuthSystemAttributesProvider.CLIENT_NAME))
		{
			c.setTitle(attrs.get(OAuthSystemAttributesProvider.CLIENT_NAME).getValues().get(0));
		}

		if (attrs.containsKey(OAuthSystemAttributesProvider.CLIENT_NAME))
		{
			c.setTitle(attrs.get(OAuthSystemAttributesProvider.CLIENT_NAME).getValues().get(0));
		}

		if (nameAttr != null && info.rootAttributesByName.containsKey(nameAttr))
		{
			c.setName(info.rootAttributesByName.get(nameAttr).getValues().get(0));
		}

		if (attrs.containsKey(OAuthSystemAttributesProvider.CLIENT_LOGO))
		{

			Attribute logo = attrs.get(OAuthSystemAttributesProvider.CLIENT_LOGO);
			ImageAttributeSyntax syntax = (ImageAttributeSyntax) attrTypeSupport.getSyntax(logo);
			UnityImage image = syntax.convertFromString(logo.getValues().get(0));

			LocalOrRemoteResource lrLogo = new LocalOrRemoteResource();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try
			{
				ImageIO.write(image.getBufferedImage(), image.getType().toExt(), baos);
				lrLogo.setLocal(baos.toByteArray());
				baos.close();
				c.setLogo(lrLogo);
			} catch (IOException e)
			{
				throw new EngineException(e);
			}

		}
		return c;
	}

	private String getUserName(List<Identity> identities)
	{
		for (Identity i : identities)
			if (i.getTypeId().equals(UsernameIdentity.ID))
				return i.getValue();
		return null;
	}

	private List<String> getAllUsernames() throws EngineException
	{

		List<String> ret = new ArrayList<>();
		Map<Long, EntityInGroupData> membershipInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData("/"));
		for (EntityInGroupData info : membershipInfo.values())
		{
			for (Identity id : info.entity.getIdentities())
			{
				if (id.getTypeId().equals(UsernameIdentity.ID))
				{
					ret.add(id.getValue());
				}

			}
		}

		return ret;

	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{

		return new OAuthServiceEditor(msg, subViewSwitcher, outputTranslationProfileFieldFactory,
				advertisedAddrProvider.get().toString(), server.getUsedContextPaths(),
				imageService, uriAccessService, fileStorageService, serverConfig,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
				atMan.getAttributeTypes().stream().map(a -> a.getName()).collect(Collectors.toList()),
				bulkService.getGroupAndSubgroups(bulkService.getBulkStructuralData("/")).values()
						.stream().map(g -> g.getGroup()).collect(Collectors.toList()),
				idpUsersHelper.getAllUsers(), this::getOAuthClients, getAllUsernames(),
				registrationMan.getForms().stream().filter(r -> r.isPubliclyAvailable())
						.map(r -> r.getName()).collect(Collectors.toList()),
				pkiMan.getCredentialNames(), authenticatorSupportService,
				idTypeSupport.getIdentityTypes(), endpointMan.getEndpoints().stream()
				.map(e -> e.getContextAddress()).collect(Collectors.toList()), policyDocumentManagement.getPolicyDocuments());
	}
}
