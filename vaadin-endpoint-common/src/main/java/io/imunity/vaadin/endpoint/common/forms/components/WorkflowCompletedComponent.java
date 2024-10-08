/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.forms.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.AutoClickButton;
import org.apache.logging.log4j.util.Strings;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;

public class WorkflowCompletedComponent extends VerticalLayout
{
	private final H2 infoL;
	public WorkflowCompletedComponent(WorkflowFinalizationConfiguration config, Component logo)
	{
		setMargin(true);
		setSpacing(true);
		setWidth("unset");
		if(logo != null)
			add(logo);

		infoL = new H2(config.mainInformation);
		infoL.addClassName(config.success ? "u-final-info" : "u-final-error");
		add(infoL);

		if (!Strings.isEmpty(config.extraInformation))
		{
			Span extraInfoL = new Span(config.extraInformation);
			extraInfoL.addClassName(config.success ? "u-final-ext-info" : "u-final-ext-error");
			extraInfoL.getStyle().set("word-break", "word-break");
			extraInfoL.getStyle().set("font-size", "initial");
			add(extraInfoL);
		}

		UI ui = UI.getCurrent();
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
			redirectB.addClassName("u-final-redirect");
			redirectB.addClickListener(e -> ui.access(() -> ui.getPage().open(config.redirectURL, "_self")));
			redirectB.addClickShortcut(Key.ENTER);
			add(redirectB);
		}

		setAlignItems(Alignment.CENTER);
	}

	public void setFontSize(String size)
	{
		infoL.getStyle().set("font-size", size);
	}
}
