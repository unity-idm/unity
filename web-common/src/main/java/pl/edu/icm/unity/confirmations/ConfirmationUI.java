/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

@Component("ConfirmationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityTheme")
public class ConfirmationUI extends UnityUIBase implements UnityWebUI
{
	private ConfirmationManager confirmationMan;
	private HorizontalLayout main; 
	
	@Autowired
	public ConfirmationUI(UnityMessageSource msg, ConfirmationManager confirmationMan, TokensManagement tokensMan)
	{
		super(msg);
		this.confirmationMan = confirmationMan;
		main = new HorizontalLayout();
	}

	@Override
	public void configure(EndpointDescription description,
			List<Map<String, BindingAuthn>> authenticators,
			EndpointRegistrationConfiguration registrationConfiguration)
	{
		// TODO Auto-generated method stub

	}
	
	private void setFail(String msg)
	{
		Label l = new Label("FAIL " + msg);
		main.removeAllComponents();
		main.addComponent(l);
	}
	
	private void setSuccess(String msg)
	{
		Label l = new Label("SUCCESSFULL" + msg);
		main.removeAllComponents();
		main.addComponent(l);
	}
	
	@Override
	protected void appInit(VaadinRequest request)
	{
		String token = request.getParameter("token");	
		try
		{
			ConfirmationStatus status = confirmationMan.proccessConfirmation(token);
			if (status.isSuccess())
				setSuccess(status.getUserMessage());
			else
				setFail("FAIL");
			
		} catch (EngineException e)
		{
		//	setFail(e.printStackTrace());
			e.printStackTrace();
			
		}	
		setContent(main);
	}

}
