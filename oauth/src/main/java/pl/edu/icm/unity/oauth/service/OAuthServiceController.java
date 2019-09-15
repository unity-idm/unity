/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.client.ClientType;

import io.imunity.webconsole.utils.tprofile.OutputTranslationProfileFieldFactory;
import pl.edu.icm.unity.base.file.FileData;
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
import pl.edu.icm.unity.engine.api.bulk.GroupMembershipInfo;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.JpegImageAttribute;
import pl.edu.icm.unity.stdext.attr.JpegImageAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.UnityImage;
import pl.edu.icm.unity.stdext.utils.UnityImage.ImageType;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.authn.services.DefaultServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceDefinition;
import pl.edu.icm.unity.webui.authn.services.ServiceEditor;
import pl.edu.icm.unity.webui.authn.services.idp.IdpServiceController;
import pl.edu.icm.unity.webui.authn.services.idp.IdpUsersHelper;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
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
	public static final String DEFAULT_CREDENTIAL = "sys:password";
	public static final String IDP_CLIENT_MAIN_GROUP = "/IdPs";
	public static final String OAUTH_CLIENTS_SUBGROUP = "oauth-clients";

	private UnityMessageSource msg;
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
	private NetworkServer server;
	private OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory;
	private AttributeTypeSupport attrTypeSupport;
	private AttributesManagement attrMan;
	private EntityManagement entityMan;
	private GroupsManagement groupMan;
	private EntityCredentialManagement entityCredentialManagement;
	private IdpUsersHelper idpUsersHelper;

	@Autowired
	OAuthServiceController(UnityMessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan,
			AttributeTypeManagement atMan, BulkGroupQueryService bulkService,
			RegistrationsManagement registrationMan, URIAccessService uriAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			AuthenticatorSupportService authenticatorSupportService, PKIManagement pkiMan,
			NetworkServer server, IdentityTypeSupport idTypeSupport,
			OutputTranslationProfileFieldFactory outputTranslationProfileFieldFactory,
			AttributeTypeSupport attrTypeSupport, AttributesManagement attrMan, EntityManagement entityMan,
			GroupsManagement groupMan, EntityCredentialManagement entityCredentialManagement,
			IdpUsersHelper idpUsersHelper)
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
		this.server = server;
		this.idTypeSupport = idTypeSupport;
		this.outputTranslationProfileFieldFactory = outputTranslationProfileFieldFactory;
		this.attrTypeSupport = attrTypeSupport;
		this.attrMan = attrMan;
		this.entityMan = entityMan;
		this.groupMan = groupMan;
		this.entityCredentialManagement = entityCredentialManagement;
		this.idpUsersHelper = idpUsersHelper;
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
		for (Endpoint endpoint : endpointMan.getEndpoints().stream()
				.filter(e -> e.getTypeId().equals(OAuthTokenEndpoint.TYPE.getName())
						&& e.getConfiguration().getTag().equals(tag))
				.collect(Collectors.toList()))
		{
			DefaultServiceDefinition tokenService = getServiceDef(endpoint);
			tokenService.setBinding(OAuthTokenEndpoint.TYPE.getSupportedBinding());
			return tokenService;
		}
		return null;
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
		return serviceDef;
	}

	@Override
	public ServiceDefinition getService(String name) throws ControllerException
	{
		try
		{
			ResolvedEndpoint endpoint = endpointMan.getDeployedEndpoints().stream()
					.filter(e -> e.getName().equals(name) && e.getType().getName()
							.equals(OAuthAuthzWebEndpoint.Factory.TYPE.getName()))
					.findFirst().orElse(null);

			if (endpoint == null)
				return null;

			DefaultServiceDefinition oauthWebService = getServiceDef(endpoint.getEndpoint());
			oauthWebService.setBinding(OAuthAuthzWebEndpoint.Factory.TYPE.getSupportedBinding());
			OAuthServiceDefinition def = new OAuthServiceDefinition(oauthWebService,
					getTokenService(endpoint.getEndpoint().getConfiguration().getTag()));
			def.setClients(getOAuthClients());
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

			Group serviceClientGroup = new Group(def.getAutoGeneratedClientsGroup());
			serviceClientGroup.setDisplayedName(new I18nString(webAuthzService.getName()));
			groupMan.addGroup(serviceClientGroup);
			Group serviceClientGroupOAuth = new Group(
					def.getAutoGeneratedClientsGroup() + "/" + OAUTH_CLIENTS_SUBGROUP);
			serviceClientGroupOAuth.setDisplayedName(new I18nString(OAUTH_CLIENTS_SUBGROUP));
			groupMan.addGroup(serviceClientGroupOAuth);

			updateClients(def.getClients(), getClientsGroup(webAuthzService.getConfiguration()));

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("ServicesController.deployError", webAuthzService.getName()), e);
		}

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

			updateClients(def.getClients(), getClientsGroup(webAuthzService.getConfiguration()));

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("ServicesController.updateError", def.getName()),
					e);
		}

	}

	private String getClientsGroup(String configuration)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(configuration));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the oauth idp service", e);
		}

		OAuthASProperties oauthProp = new OAuthASProperties(raw);
		return oauthProp.getValue(OAuthASProperties.CLIENTS_GROUP);
	}

	private void updateClients(List<OAuthClient> clients, String fromGroup)
			throws EngineException, URISyntaxException
	{
		String clientNameAttr = idpUsersHelper.getClientNameAttr();

		for (OAuthClient client : clients.stream().filter(c -> c.getGroup().equals(fromGroup))
				.collect(Collectors.toList()))
		{
			if (client.getEntity() == null)
			{
				Long id = addOAuthClient(client);
				OAuthClient clone = client.clone();
				clone.setEntity(id);
				updateClient(clone, clientNameAttr);
				continue;
			}

			if (client.isToRemove())
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
				continue;
			}

			if (client.isUpdated())
			{
				updateClient(client, clientNameAttr);
				continue;
			}
		}
	}

	private long addOAuthClient(OAuthClient client) throws EngineException
	{
		IdentityParam id = new IdentityParam(UsernameIdentity.ID, client.getId());
		Identity addEntity = entityMan.addEntity(id, EntityState.valid, false);
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

		if (client.getSecret() != null)
		{
			entityCredentialManagement.setEntityCredential(entity, DEFAULT_CREDENTIAL,
					new PasswordToken(client.getSecret()).toJson());
		}

	}

	private void updateLogo(EntityParam entity, String group, byte[] value) throws EngineException
	{
		JpegImageAttributeSyntax syntax = (JpegImageAttributeSyntax) attrTypeSupport
				.getSyntax(attrTypeSupport.getType(OAuthSystemAttributesProvider.CLIENT_LOGO));
		UnityImage image = new UnityImage(value, ImageType.JPG);
		image.scaleDown(syntax.getMaxWidth(), syntax.getMaxHeight());

		Attribute logoAttr = JpegImageAttribute.of(OAuthSystemAttributesProvider.CLIENT_LOGO, group,
				image.getBufferedImage());
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

	private List<OAuthClient> getOAuthClients() throws EngineException
	{
		List<OAuthClient> clients = new ArrayList<>();

		Map<Long, GroupMembershipInfo> membershipInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData("/"));
		String nameAttr = idpUsersHelper.getClientNameAttr();

		for (GroupMembershipInfo info : membershipInfo.values())
		{
			for (String group : info.attributes.keySet())
			{
				Map<String, AttributeExt> attrs = info.attributes.get(group);
				if (attrs.keySet().contains(OAuthSystemAttributesProvider.ALLOWED_FLOWS))
				{
					clients.add(getOAuthClient(info, group, attrs, nameAttr));
				}

			}

		}
		return clients;
	}

	private OAuthClient getOAuthClient(GroupMembershipInfo info, String group, Map<String, AttributeExt> attrs,
			String nameAttr) throws EngineException
	{
		OAuthClient c = new OAuthClient();
		c.setEntity(info.entityInfo.getId());
		c.setId(getUserName(info.identities));
		c.setGroup(group);

		c.setFlows(attrs.get(OAuthSystemAttributesProvider.ALLOWED_FLOWS).getValues());

		if (attrs.keySet().contains(OAuthSystemAttributesProvider.CLIENT_TYPE))
		{
			c.setType(attrs.get(OAuthSystemAttributesProvider.CLIENT_TYPE).getValues().get(0));
		} else
		{
			c.setType(ClientType.CONFIDENTIAL.toString());
		}

		if (attrs.keySet().contains(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI))
		{
			c.setRedirectURIs(attrs.get(OAuthSystemAttributesProvider.ALLOWED_RETURN_URI).getValues());
		}

		if (attrs.keySet().contains(OAuthSystemAttributesProvider.CLIENT_NAME))
		{
			c.setTitle(attrs.get(OAuthSystemAttributesProvider.CLIENT_NAME).getValues().get(0));
		}

		if (attrs.keySet().contains(OAuthSystemAttributesProvider.CLIENT_NAME))
		{
			c.setTitle(attrs.get(OAuthSystemAttributesProvider.CLIENT_NAME).getValues().get(0));
		}

		if (nameAttr != null && info.attributes.get("/").keySet().contains(nameAttr))
		{
			c.setName(info.attributes.get("/").get(nameAttr).getValues().get(0));
		}

		if (attrs.keySet().contains(OAuthSystemAttributesProvider.CLIENT_LOGO))
		{

			Attribute logo = attrs.get(OAuthSystemAttributesProvider.CLIENT_LOGO);
			JpegImageAttributeSyntax syntax = (JpegImageAttributeSyntax) attrTypeSupport.getSyntax(logo);
			BufferedImage image = syntax.convertFromString(logo.getValues().get(0));

			LocalOrRemoteResource lrLogo = new LocalOrRemoteResource();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try
			{
				ImageIO.write(image, "jpg", baos);
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
		{

			if (i.getTypeId().equals(UsernameIdentity.ID))
			{
				return i.getValue();
			}

		}
		return null;
	}

	private List<String> getAllUsernames() throws EngineException
	{

		List<String> ret = new ArrayList<>();
		Map<Long, GroupMembershipInfo> membershipInfo = bulkService
				.getMembershipInfo(bulkService.getBulkMembershipData("/"));
		for (GroupMembershipInfo info : membershipInfo.values())
		{
			for (Identity id : info.identities)
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

		return new OAuthServiceEditor(msg, subViewSwitcher, outputTranslationProfileFieldFactory, server,
				uriAccessService, fileStorageService, serverConfig,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
				atMan.getAttributeTypes().stream().map(a -> a.getName()).collect(Collectors.toList()),
				bulkService.getGroupAndSubgroups(bulkService.getBulkStructuralData("/")).values()
						.stream().map(g -> g.getGroup()).collect(Collectors.toList()),
				idpUsersHelper.getAllUsers(), getOAuthClients(), getAllUsernames(),
				registrationMan.getForms().stream().filter(r -> r.isPubliclyAvailable())
						.map(r -> r.getName()).collect(Collectors.toList()),
				pkiMan.getCredentialNames(), authenticatorSupportService,
				idTypeSupport.getIdentityTypes(), endpointMan.getEndpoints().stream()
						.map(e -> e.getContextAddress()).collect(Collectors.toList()));
	}

}
