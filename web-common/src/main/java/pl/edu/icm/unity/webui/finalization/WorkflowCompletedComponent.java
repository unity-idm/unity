/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.finalization;

import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.util.Strings;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.AutoClickButton;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;


/**
 * Arbitrary completed view. Can be used both in case of finishing up with an error or success.
 * Presents main info, additional text and optional button with redirect button.
 */
public class WorkflowCompletedComponent extends CustomComponent
{
	public WorkflowCompletedComponent(WorkflowFinalizationConfiguration config, BiConsumer<Page, String> redirector,
			ImageAccessService imageAccessService)
	{
		Optional<Resource> logoResource = imageAccessService.getConfiguredImageResourceFromNullableUri(config.logoURL);
		createUI(config, logoResource, redirector);
	}
	
	public WorkflowCompletedComponent(WorkflowFinalizationConfiguration config, Optional<Resource> logo, 
			BiConsumer<Page, String> redirector)
	{
		createUI(config, logo, redirector);
	}

	private void createUI(WorkflowFinalizationConfiguration config, Optional<Resource> logo, BiConsumer<Page, String> redirector)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(true);
		main.setSpacing(true);
		
		if (logo.isPresent())
		{
			Image image = new Image(null, logo.get());
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
		Page p = Page.getCurrent();
		if (config.redirectURL != null)
		{
			Button redirectB;
			if (config.redirectAfterTime != null && config.redirectAfterTime.getSeconds() > 0)
			{
				redirectB = new AutoClickButton(config.redirectButtonText, UI.getCurrent(), config.redirectAfterTime.getSeconds());
			}else
			{
				redirectB = new Button(config.redirectButtonText);	
			}
			redirectB.setStyleName(Styles.vButtonPrimary.toString());
			redirectB.addStyleName("u-final-redirect");
			redirectB.addClickListener(e -> redirector.accept(p, config.redirectURL));
			redirectB.setClickShortcut(KeyCode.ENTER);
			main.addComponent(redirectB);
			main.setComponentAlignment(redirectB, Alignment.MIDDLE_CENTER);	
		}
		
		setCompositionRoot(main);
	}
	
	public Component getWrappedForFullSizeComponent()
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setSpacing(false);
		wrapper.setMargin(false);
		wrapper.setSizeFull();
		wrapper.addComponent(this);
		wrapper.setComponentAlignment(this, Alignment.MIDDLE_CENTER);
		return wrapper;
	}
}
