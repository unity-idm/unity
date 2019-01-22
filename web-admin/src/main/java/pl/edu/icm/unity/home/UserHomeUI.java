/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.ProjectManagementConstants;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.StandardWebAuthenticationProcessor;
import pl.edu.icm.unity.webui.common.TopHeader;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiresDialogLauncher;

/**
 * The main entry point of the web administration UI.
 * 
 * @author K. Benedyczak
 */
@Component("UserHomeUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class UserHomeUI extends UnityEndpointUIBase implements UnityWebUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, UserHomeUI.class);

	private UserAccountComponent userAccount;
	private StandardWebAuthenticationProcessor authnProcessor;
	private HomeEndpointProperties config;
	private DelegatedGroupManagement delGroupMan;
	private EndpointManagement endpointMan;
	private NetworkServer networkServer;

	@Autowired
	public UserHomeUI(UnityMessageSource msg, UserAccountComponent userAccountComponent,
			StandardWebAuthenticationProcessor authnProcessor, EnquiresDialogLauncher enquiryDialogLauncher,
			DelegatedGroupManagement delGroupMan, @Qualifier("insecure") EndpointManagement endpointMan,
			NetworkServer networkServer)
	{
		super(msg, enquiryDialogLauncher);
		this.userAccount = userAccountComponent;
		this.authnProcessor = authnProcessor;
		this.delGroupMan = delGroupMan;
		this.endpointMan = endpointMan;
		this.networkServer = networkServer;
	}

	@Override
	public void configure(ResolvedEndpoint description, List<AuthenticationFlow> authenticators,
			EndpointRegistrationConfiguration regCfg, Properties endpointProperties)
	{
		super.configure(description, authenticators, regCfg, endpointProperties);
		this.config = new HomeEndpointProperties(endpointProperties);
	}

	@Override
	protected void enter(VaadinRequest request)
	{
		VerticalLayout contents = new VerticalLayout();
		contents.setMargin(false);
		contents.setSpacing(false);
		I18nString displayedName = endpointDescription.getEndpoint().getConfiguration().getDisplayedName();
		TopHeader header = new HomeTopHeader(displayedName.getValue(msg), authnProcessor, msg,
				getProjectManLinkIfAvailable());
		contents.addComponent(header);

		userAccount.initUI(config, sandboxRouter, getSandboxServletURLForAssociation());

		userAccount.setWidth(80, Unit.PERCENTAGE);
		contents.addComponent(userAccount);
		contents.setComponentAlignment(userAccount, Alignment.TOP_CENTER);
		contents.setExpandRatio(userAccount, 1.0f);

		setSizeFull();
		setContent(contents);
	}

	private Optional<String> getProjectManLinkIfAvailable()
	{

		if (!config.isProjectManLinkIsEnabled())
			return Optional.empty();

		Set<ResolvedEndpoint> allEndpoints = getAllProjectManEndpoints();
		if (allEndpoints.isEmpty())
		{
			log.debug("Project mamangement link is enable, but project management endpoins are not available");
			return Optional.empty();

		}

		if (!checkIfUserHasProjects())
		{
			log.debug("Project mamangement link is enable, but user is not a manager of any group");
			return Optional.empty();
		}

		String projectManEndpointName = config.getProjectManEndpoint();

		if (projectManEndpointName == null)
		{
			log.debug("Project mamangement link is enable, using first available project management endpoint");
			return Optional.ofNullable(getLinkToProjectManagementEndpoint(allEndpoints.iterator().next()));
		} else
		{

			ResolvedEndpoint endpoint = allEndpoints.stream()
					.filter(e -> e.getName().equals(config.getProjectManEndpoint())).findAny()
					.orElse(null);

			if (endpoint == null)
			{
				log.debug("Project mamangement link is enable, but endpoint with name "
						+ projectManEndpointName + " is not available");
				return Optional.empty();
			}

			return Optional.ofNullable(getLinkToProjectManagementEndpoint(endpoint));
		}
	}

	private boolean checkIfUserHasProjects()

	{
		LoginSession entity = InvocationContext.getCurrent().getLoginSession();
		if (entity == null)
			return false;
		List<DelegatedGroup> projectsForEntity;
		try
		{
			projectsForEntity = delGroupMan.getProjectsForEntity(entity.getEntityId());
		} catch (EngineException e)
		{
			log.error("Can not get projects for entity " + entity, e);
			return false;
		}

		return !projectsForEntity.isEmpty();
	}

	private String getLinkToProjectManagementEndpoint(ResolvedEndpoint endpoint)
	{
		if (endpoint == null)
			return null;
		String path = endpoint.getType().getPaths().keySet().iterator().next();
		return networkServer.getAdvertisedAddress() + endpoint.getEndpoint().getContextAddress() + path;
	}

	private Set<ResolvedEndpoint> getAllProjectManEndpoints()
	{
		try
		{
			return endpointMan.getEndpoints().stream().filter(
					e -> e.getType().getName().equals(ProjectManagementConstants.ENDPOINT_NAME))
					.collect(Collectors.toSet());
		} catch (EngineException e)
		{
			log.error("Can not get project management endpoints", e);
			return Collections.emptySet();
		}

	}
}
