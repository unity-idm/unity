/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.confirmations;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.Theme;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder.Status;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationServletProvider;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationStatus;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.ConfirmationComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeaderLight;

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
	private boolean autoRedirect;
	private String defaultRedirect;

	@Autowired
	public EmailConfirmationUI(UnityMessageSource msg, EmailConfirmationManager confirmationMan,
			TokensManagement tokensMan, UnityServerConfiguration serverConfig)
	{
		super(msg);
		this.confirmationMan = confirmationMan;
		this.autoRedirect = serverConfig.getBooleanValue(UnityServerConfiguration.CONFIRMATION_AUTO_REDIRECT);
		this.defaultRedirect = serverConfig.getValue(UnityServerConfiguration.CONFIRMATION_DEFAULT_RETURN_URL);
	}

	public void initUI(EmailConfirmationStatus status)
	{
		final String returnUrl = status.getReturnUrl();
		
		if (autoRedirect && returnUrl != null)
		{
			Page.getCurrent().open(returnUrl, null);
			return;
		}
		
		VerticalLayout contents = new VerticalLayout();
		contents.setMargin(false);
		contents.setSpacing(false);
		VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.setSpacing(false);
		mainWrapper.setMargin(false);
		mainWrapper.setSizeFull();
		mainWrapper.addComponent(new TopHeaderLight(msg.getMessage("ConfirmationUI.title"),
				msg));
		HorizontalLayout infoWrapper = new HorizontalLayout();
		infoWrapper.setSpacing(false);
		infoWrapper.setMargin(false);
		infoWrapper.setWidth(50, Unit.PERCENTAGE);
		String infoKey = status.getUserMessageKey();
		String[] infoArgs = status.getUserMessageArgs();

		infoWrapper.addComponent(status.isSuccess() == true ? 
				getSuccessfullStatus(msg.getMessage(infoKey, (Object[])infoArgs))
				: getUnsuccessfullStatus(msg.getMessage(infoKey, (Object[])infoArgs)));
		Label spacerB = new Label();
		Label spacerU = new Label();
		
		mainWrapper.addComponent(spacerU);
		mainWrapper.addComponent(infoWrapper);
		mainWrapper.setComponentAlignment(infoWrapper, Alignment.TOP_CENTER);
	
		if (returnUrl != null && !returnUrl.equals(""))
		{	
			Link returnUrlLink =  new Link(msg.getMessage("ConfirmationUI.returnUrl"),
					new ExternalResource(returnUrl));
			returnUrlLink.addStyleName(Styles.textLarge.toString());
			Label spacerR = new Label();
			mainWrapper.addComponent(spacerR);
			mainWrapper.addComponent(returnUrlLink);
			mainWrapper.setComponentAlignment(returnUrlLink, Alignment.TOP_CENTER);
			mainWrapper.setExpandRatio(spacerR, 0.1f);
		}
		
		
		mainWrapper.addComponent(spacerB);
		mainWrapper.setExpandRatio(spacerU, 0.2f);
		mainWrapper.setExpandRatio(infoWrapper, 0);
		mainWrapper.setExpandRatio(spacerB, 0.7f);

		contents.addComponent(mainWrapper);
		contents.setExpandRatio(mainWrapper, 1.0f);
		contents.setComponentAlignment(mainWrapper, Alignment.TOP_LEFT);
		contents.setSizeFull();
		setContent(contents);
	}

	private com.vaadin.ui.Component getSuccessfullStatus(String info)
	{
		return new ConfirmationComponent(Images.ok, 
				msg.getMessage("ConfirmationStatus.successful"), info);
	}

	private com.vaadin.ui.Component getUnsuccessfullStatus(String info)
	{
		return new ConfirmationComponent(Images.error, 
				msg.getMessage("ConfirmationStatus.unsuccessful"), info);
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		EmailConfirmationStatus status = null;
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
			status = new EmailConfirmationStatus(false, redirectURL, "ConfirmationStatus.internalError");
		}
		initUI(status);
	}
}
