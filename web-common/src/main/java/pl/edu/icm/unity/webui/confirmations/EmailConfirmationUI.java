/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.confirmations;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder.Status;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationServletProvider;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.finalization.WorkflowCompletedComponent;

/**
 * Shows confirmation status
 * 
 * @author P. Piernik
 * 
 */
@Component("EmailConfirmationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class EmailConfirmationUI extends UnityUIBase implements UnityWebUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EmailConfirmationUI.class);

	private EmailConfirmationManager confirmationMan;
	private String defaultRedirect;

	@Autowired
	public EmailConfirmationUI(UnityMessageSource msg, EmailConfirmationManager confirmationMan,
			TokensManagement tokensMan, UnityServerConfiguration serverConfig)
	{
		super(msg);
		this.confirmationMan = confirmationMan;
		this.defaultRedirect = serverConfig.getValue(UnityServerConfiguration.CONFIRMATION_DEFAULT_RETURN_URL);
	}

	private void initUI(WorkflowFinalizationConfiguration status)
	{
		if (status.autoRedirect && !Strings.isEmpty(status.redirectURL))
		{
			Page.getCurrent().open(status.redirectURL, null);
			return;
		}
		
		if (status.pageTitle != null)
			Page.getCurrent().setTitle(status.pageTitle);
		
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		wrapper.setSizeFull();
		setSizeFull();
		
		Resource logo = null;
		if (!Strings.isEmpty(status.logoURL))
			logo = ImageUtils.getConfiguredImageResource(status.logoURL);
		if (logo == null)
			logo = status.success ? Images.ok.getResource() : Images.error.getResource();
		WorkflowCompletedComponent contents = new WorkflowCompletedComponent(status, 
				logo,
				url -> Page.getCurrent().open(status.redirectURL, null));
		wrapper.addComponent(contents);
		wrapper.setComponentAlignment(contents, Alignment.MIDDLE_CENTER);
		setContent(wrapper);
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		WorkflowFinalizationConfiguration status = null;
		String token = request.getParameter(EmailConfirmationServletProvider.CONFIRMATION_TOKEN_ARG);
		try
		{
			status = confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			log.error("Internal unity problem with confirmation", e);
			String redirectURL = new EmailConfirmationRedirectURLBuilder(defaultRedirect, 
					Status.elementConfirmationError).
				setErrorCode(e.toString()).build();
			status = WorkflowFinalizationConfiguration.builder()
					.setRedirectURL(redirectURL)
					.setMainInformation(msg.getMessage("ConfirmationStatus.internalError"))
					.build();
		}
		initUI(status);
	}
}
