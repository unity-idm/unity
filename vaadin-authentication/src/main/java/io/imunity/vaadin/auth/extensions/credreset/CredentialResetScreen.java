/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Minimal component covering full screen, and allowing for setting (and changing) the contents.
 */
public class CredentialResetScreen extends VerticalLayout
{
	public void setContents(Component contents)
	{
		setMargin(false);
		setPadding(false);
		add(contents);
		setSizeFull();
		setAlignItems(Alignment.CENTER);
	}
}
