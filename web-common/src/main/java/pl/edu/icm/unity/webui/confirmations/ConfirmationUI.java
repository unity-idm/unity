/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.confirmations;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder.Status;
import pl.edu.icm.unity.confirmations.ConfirmationServlet;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeaderLight;

import com.vaadin.annotations.Theme;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

/**
 * Shows confirmation status
 * 
 * @author P. Piernik
 * 
 */
@Component("ConfirmationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class ConfirmationUI extends UnityUIBase implements UnityWebUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ConfirmationUI.class);

	private ConfirmationManager confirmationMan;
	private boolean autoRedirect;
	private String defaultRedirect;

	@Autowired
	public ConfirmationUI(UnityMessageSource msg, ConfirmationManager confirmationMan,
			TokensManagement tokensMan, UnityServerConfiguration serverConfig)
	{
		super(msg);
		this.confirmationMan = confirmationMan;
		this.autoRedirect = serverConfig.getBooleanValue(UnityServerConfiguration.CONFIRMATION_AUTO_REDIRECT);
		this.defaultRedirect = serverConfig.getValue(UnityServerConfiguration.CONFIRMATION_DEFAULT_RETURN_URL);
	}

	public void initUI(ConfirmationStatus status)
	{
		final String returnUrl = status.getReturnUrl();
		
		if (autoRedirect && returnUrl != null)
		{
			Page.getCurrent().open(returnUrl, null);
			return;
		}
		
		VerticalLayout contents = new VerticalLayout();
		VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.setSizeFull();
		mainWrapper.addComponent(new TopHeaderLight(msg.getMessage("ConfirmationUI.title"),
				msg));
		HorizontalLayout infoWrapper = new HorizontalLayout();
		infoWrapper.setWidth(50, Unit.PERCENTAGE);
		String infoKey = status.getUserMessageKey();
		String[] infoArgs = status.getUserMessageArgs();

		infoWrapper.addComponent(status.isSuccess() == true ? getSuccessfullStatus(infoKey, infoArgs)
				: getUnsuccessfullStatus(infoKey, infoArgs));
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

	private VerticalLayout getSuccessfullStatus(String infoKey, String[] infoArgs)
	{
		return getStatus(Images.ok32.getResource(),
				msg.getMessage("ConfirmationStatus.successful"), infoKey, infoArgs);
	}

	private VerticalLayout getUnsuccessfullStatus(String infoKey, String[] infoArgs)
	{
		return getStatus(Images.error32.getResource(),
				msg.getMessage("ConfirmationStatus.unsuccessful"), infoKey, infoArgs);
	}

	private VerticalLayout getStatus(Resource icon, String title, String infoKey, Object[] infoArgs)
	{
		VerticalLayout mainStatus = new VerticalLayout();
		HorizontalLayout header = new HorizontalLayout();
		header.setSizeFull();
		HorizontalLayout headerWrapper = new HorizontalLayout();
		Image statusIcon = new Image();
		statusIcon.setSource(icon);
		Label titleL = new Label(title);
		titleL.addStyleName(Styles.textXLarge.toString());
		headerWrapper.addComponents(statusIcon, titleL);
		headerWrapper.setComponentAlignment(statusIcon, Alignment.MIDDLE_CENTER);
		headerWrapper.setComponentAlignment(titleL, Alignment.MIDDLE_CENTER);
		header.addComponent(headerWrapper);
		header.setComponentAlignment(headerWrapper, Alignment.TOP_CENTER);
		mainStatus.addComponent(header);
		Label info = new Label(msg.getMessage(infoKey, infoArgs));
		info.addStyleName(Styles.textCenter.toString());
		info.addStyleName(Styles.textLarge.toString());
		mainStatus.addComponent(info);
		return mainStatus;
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		ConfirmationStatus status = null;
		String token = request.getParameter(ConfirmationServlet.CONFIRMATION_TOKEN_ARG);
		try
		{
			status = confirmationMan.processConfirmation(token);
		} catch (Exception e)
		{
			log.error("Internal unity problem with confirmation", e);
			String redirectURL = new ConfirmationRedirectURLBuilder(defaultRedirect, Status.elementConfirmationError).
				setErrorCode(e.toString()).build();
			status = new ConfirmationStatus(false, redirectURL, "ConfirmationStatus.internalError");
		}
		initUI(status);
	}
}
