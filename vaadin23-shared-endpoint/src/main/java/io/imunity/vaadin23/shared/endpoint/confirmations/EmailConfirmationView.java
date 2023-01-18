/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.confirmations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import io.imunity.vaadin23.shared.endpoint.components.WorkflowCompletedComponent;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder.Status;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationServletProvider;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;

import java.util.List;

@Route(value = "/confirmation")
public class EmailConfirmationView extends Composite<Div> implements HasUrlParameter<String>, HasDynamicTitle
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EmailConfirmationView.class);

	private final MessageSource msg;
	private final EmailConfirmationManager confirmationMan;
	private final String defaultRedirect;

	private WorkflowFinalizationConfiguration status;

	@Autowired
	public EmailConfirmationView(MessageSource msg, EmailConfirmationManager confirmationMan,
	                             UnityServerConfiguration serverConfig)
	{
		this.msg = msg;
		this.confirmationMan = confirmationMan;
		this.defaultRedirect = serverConfig.getValue(UnityServerConfiguration.CONFIRMATION_DEFAULT_RETURN_URL);
	}

	private void initUI()
	{
		if (status.autoRedirect && !Strings.isEmpty(status.redirectURL))
		{
			UI.getCurrent().getPage().open(status.redirectURL, null);
			return;
		}
		
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		wrapper.setSizeFull();
		wrapper.getStyle().set("font-size", "1.5em");
		getContent().setSizeFull();

		Component image = null;
		if(status.logoURL == null || status.logoURL.isBlank())
			if(!status.success)
				image = VaadinIcon.EXCLAMATION_CIRCLE.create();
		else
		{
			image = new Image(status.logoURL, "");
			image.getElement().getStyle().set("max-height", "5rem");
			((HasStyle)image).addClassName("u-final-logo");
		}

		WorkflowCompletedComponent contents = new WorkflowCompletedComponent(status, image);
		wrapper.add(contents);
		getContent().add(wrapper);
	}

	@Override
	public String getPageTitle()
	{
		return status.pageTitle == null ? "" : status.pageTitle;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter)
	{
		String token = event.getLocation().getQueryParameters()
				.getParameters()
				.getOrDefault(EmailConfirmationServletProvider.CONFIRMATION_TOKEN_ARG, List.of())
				.stream().findFirst().orElse(null);
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
		initUI();
	}
}
