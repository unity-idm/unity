/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.confirmations;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.ConfirmationServlet;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.TopHeaderLight;

import com.vaadin.annotations.Theme;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Shows confirmation status
 * 
 * @author P. Piernik
 * 
 */
@Component("ConfirmationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class ConfirmationUI extends UnityUIBase implements UnityWebUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ConfirmationUI.class);

	private ConfirmationManager confirmationMan;

	@Autowired
	public ConfirmationUI(UnityMessageSource msg, ConfirmationManager confirmationMan,
			TokensManagement tokensMan)
	{
		super(msg);
		this.confirmationMan = confirmationMan;
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration registrationConfiguration,
			Properties genericEndpointConfiguration)
	{
		// TODO Auto-generated method stub
		
	}

	public void initUI(boolean status, String info)
	{
		VerticalLayout contents = new VerticalLayout();
		VerticalLayout mainWrapper = new VerticalLayout();
		mainWrapper.setSizeFull();
		mainWrapper.addComponent(new TopHeaderLight(msg.getMessage("ConfirmationUI.title"),
				msg));

		HorizontalLayout infoWrapper = new HorizontalLayout();
		infoWrapper.setWidth(30, Unit.PERCENTAGE);
		infoWrapper.addComponent(status == true ? getSuccessfullStatus(info)
				: getUnsuccessfullStatus(info));

		Label spacerB = new Label();
		Label spacerU = new Label();
		mainWrapper.addComponent(spacerU);
		mainWrapper.addComponent(infoWrapper);
		mainWrapper.setComponentAlignment(infoWrapper, Alignment.TOP_CENTER);
		mainWrapper.addComponent(spacerB);
		mainWrapper.setExpandRatio(spacerU, 0.2f);
		mainWrapper.setExpandRatio(infoWrapper, 0);
		mainWrapper.setExpandRatio(spacerB, 0.8f);

		contents.addComponent(mainWrapper);
		contents.setExpandRatio(mainWrapper, 1.0f);
		contents.setComponentAlignment(mainWrapper, Alignment.TOP_LEFT);
		contents.setSizeFull();
		setContent(contents);
	}

	private VerticalLayout getSuccessfullStatus(String infoKey)
	{
		return getStatus(Images.ok.getResource(),
				msg.getMessage("ConfirmationStatus.successfull"), infoKey);
	}

	private VerticalLayout getUnsuccessfullStatus(String infoKey)
	{
		return getStatus(Images.error.getResource(),
				msg.getMessage("ConfirmationStatus.unsuccessfull"), infoKey);
	}

	private VerticalLayout getStatus(Resource icon, String title, String infoKey)
	{
		VerticalLayout mainStatus = new VerticalLayout();
		HorizontalLayout header = new HorizontalLayout();
		header.setSizeFull();
		HorizontalLayout headerWrapper = new HorizontalLayout();
		Image statusIcon = new Image();
		statusIcon.setSource(icon);
		Label titleL = new Label(title);
		titleL.addStyleName(Styles.textXLarge.toString());
		headerWrapper.addComponents(statusIcon, new Label(title));
		header.addComponent(headerWrapper);
		header.setComponentAlignment(headerWrapper, Alignment.TOP_CENTER);
		mainStatus.addComponent(header);
		mainStatus.addComponent(new Label(msg.getMessage(infoKey)));
		return mainStatus;
	}

	@Override
	protected void appInit(VaadinRequest request)
	{
		ConfirmationStatus status = null;
		String token = request.getParameter(ConfirmationServlet.CONFIRMATION_TOKEN_ARG);
		try
		{
			status = confirmationMan.proccessConfirmation(token);

		} catch (Exception e)
		{
			log.error("Internal unity problem with confirmation", e);
			status = new ConfirmationStatus(false, "ConfirmationStatus.internalError");
		}

		initUI(status.isSuccess(), status.getUserMessageKey());
	}
}
