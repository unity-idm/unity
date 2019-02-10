/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.finalization;

import java.util.function.Consumer;

import org.apache.logging.log4j.util.Strings;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;


/**
 * Arbitrary completed view. Can be used both in case of finishing up with an error or success.
 * Presents main info, additional text and optional button with redirect button.
 */
public class WorkflowCompletedComponent extends CustomComponent
{
	public WorkflowCompletedComponent(WorkflowFinalizationConfiguration config, Consumer<String> redirector)
	{
		Resource logoResource = Strings.isEmpty(config.logoURL) ? 
				null : ImageUtils.getConfiguredImageResource(config.logoURL);
		createUI(config, logoResource, redirector);
	}
	
	public WorkflowCompletedComponent(WorkflowFinalizationConfiguration config, Resource logo, 
			Consumer<String> redirector)
	{
		createUI(config, logo, redirector);
	}

	private void createUI(WorkflowFinalizationConfiguration config, Resource logo, Consumer<String> redirector)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(true);
		main.setSpacing(true);
		
		if (logo != null)
		{
			Image image = new Image(null, logo);
			image.addStyleName("u-final-logo");
			main.addComponent(image);
			main.setComponentAlignment(image, Alignment.MIDDLE_CENTER);
		}
		
		Label infoL = new Label(config.mainInformation);
		infoL.addStyleName(Styles.vLabelH1.toString());
		infoL.addStyleName(config.success ? "u-final-info" : "u-final-error");
		main.addComponent(infoL);
		main.setComponentAlignment(infoL, Alignment.MIDDLE_CENTER);
		
		if (!Strings.isEmpty(config.extraInformation))
		{
			HtmlConfigurableLabel extraInfoL = new HtmlConfigurableLabel(config.extraInformation);
			extraInfoL.addStyleName(config.success ? "u-final-ext-info" : "u-final-ext-error");
			main.addComponent(extraInfoL);
			main.setComponentAlignment(extraInfoL, Alignment.MIDDLE_CENTER);
		}
		
		if (config.redirectURL != null)
		{
			Button redirectB = new Button(config.redirectButtonText);
			redirectB.setStyleName(Styles.vButtonPrimary.toString());
			redirectB.addStyleName("u-final-redirect");
			redirectB.addClickListener(e -> redirector.accept(config.redirectURL));
			redirectB.setClickShortcut(KeyCode.ENTER);
			main.addComponent(redirectB);
			main.setComponentAlignment(redirectB, Alignment.MIDDLE_CENTER);
		}
		
		setCompositionRoot(main);
	}
}
