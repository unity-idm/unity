/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset;


import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.LinkButton;

/**
 * Component providing a common layout of all screens in credential reset flow.
 * Full screen component, centered content's column, on top heading with common styles applied, below fixed width column
 * with variable contents.
 */
public abstract class CredentialResetLayout extends VerticalLayout
{
	protected final float INFO_WIDTH_EM;
	protected final float MAIN_WIDTH_EM;
	private final CredentialResetFlowConfig uiConfig;

	public CredentialResetLayout(CredentialResetFlowConfig credResetConfig)
	{
		this.uiConfig = credResetConfig;
		INFO_WIDTH_EM = credResetConfig.infoWidth;
		MAIN_WIDTH_EM = credResetConfig.contentsWidth;
	}

	protected void initUI(String title, Component variableContents)
	{
		addClassName("u-credreset-dialog");
				
		H2 info = new H2(title);
		info.addClassName("u-passwordResetTitle");

		VerticalLayout wrappingCol = new VerticalLayout();
		wrappingCol.getStyle().set("gap", "0");
		wrappingCol.setWidth(INFO_WIDTH_EM, Unit.EM);
		if (uiConfig.logo.isPresent())
		{
			Image image = uiConfig.logo.get();
			image.addClassName("u-authn-logo");
			image.getStyle().set("max-height", "5rem");
			wrappingCol.add(image);
		}
		wrappingCol.add(info);

		wrappingCol.add(variableContents);
		wrappingCol.setAlignItems(Alignment.CENTER);

		setWidth(INFO_WIDTH_EM, Unit.EM);
		removeAll();
		add(wrappingCol);
	}
	
	protected Component getButtonsBar(String confirmLabel, Runnable onConfirm, String cancelLabel, Runnable onCancel)
	{
		Button proceed = new Button(confirmLabel);
		proceed.addClickListener(e -> onConfirm.run());
		proceed.setWidthFull();
		proceed.addClickShortcut(Key.ENTER);
		proceed.addClassName("u-cred-reset-proceed");
		proceed.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		
		LinkButton cancel = new LinkButton(cancelLabel, e -> {});
		cancel.addClickListener(e -> onCancel.run());
		cancel.addClassName("u-cred-reset-cancel");
		
		VerticalLayout buttons = new VerticalLayout();
		buttons.setMargin(false);
		buttons.setPadding(false);
		buttons.getStyle().set("gap", "0");
		buttons.setAlignItems(Alignment.END);
		buttons.setWidth(MAIN_WIDTH_EM, Unit.EM);
		buttons.add(proceed, cancel);
		return buttons;
	}
}
