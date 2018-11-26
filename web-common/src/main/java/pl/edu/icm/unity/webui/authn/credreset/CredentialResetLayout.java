/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.webui.common.Styles;

/**
 * Component providing a common layout of all screens in credential reset flow.
 * Full screen component, centered contents column, on top heading with common styles applied, below fixed width column 
 * with variable contents. 
 * 
 * @author K. Benedyczak
 */
public abstract class CredentialResetLayout extends CustomComponent
{
	protected final float INFO_WIDTH_EM;
	protected final float MAIN_WIDTH_EM;
	private CredentialResetFlowConfig uiConfig;

	public CredentialResetLayout(CredentialResetFlowConfig credResetConfig)
	{
		this.uiConfig = credResetConfig;
		INFO_WIDTH_EM = credResetConfig.infoWidth;
		MAIN_WIDTH_EM = credResetConfig.contentsWidth;
	}

	protected void initUI(String title, Component variableContents)
	{
		addStyleName("u-credreset-dialog");
				
		Label info = new Label(title);
		info.addStyleName("u-passwordResetTitle");
		info.addStyleName(Styles.textTitle.toString());
		info.addStyleName(Styles.textCenter.toString());
		info.setWidth(100, Unit.PERCENTAGE);
		
		
		VerticalLayout wrappingCol = new VerticalLayout();
		wrappingCol.setWidth(INFO_WIDTH_EM, Unit.EM);
		if (uiConfig.logo.isPresent())
		{
			Image image = new Image(null, uiConfig.logo.get());
			image.addStyleName("u-authn-logo");
			wrappingCol.addComponent(image);
			wrappingCol.setComponentAlignment(image, Alignment.TOP_CENTER);
		}
		wrappingCol.addComponent(info);
		wrappingCol.setComponentAlignment(info, Alignment.TOP_CENTER);
		
		wrappingCol.addComponent(variableContents);
		wrappingCol.setComponentAlignment(variableContents, Alignment.TOP_CENTER);
		
		setWidth(INFO_WIDTH_EM, Unit.EM);
		setCompositionRoot(wrappingCol);
	}
	
	protected Component getButtonsBar(String confirmLabel, Runnable onConfirm, String cancelLabel, Runnable onCancel)
	{
		Button proceed = new Button(confirmLabel);
		proceed.addClickListener(e -> onConfirm.run());
		proceed.setWidth(100, Unit.PERCENTAGE);
		proceed.setClickShortcut(KeyCode.ENTER);
		proceed.addStyleName("u-cred-reset-proceed");
		
		Button cancel = new Button(cancelLabel);
		cancel.addStyleName(Styles.vButtonLink.toString());
		cancel.addClickListener(e -> onCancel.run());
		cancel.addStyleName("u-cred-reset-cancel");
		
		VerticalLayout buttons = new VerticalLayout();
		buttons.setMargin(new MarginInfo(true, false, false, false));
		buttons.setWidth(MAIN_WIDTH_EM, Unit.EM);
		buttons.addComponents(proceed, cancel);
		buttons.setComponentAlignment(cancel, Alignment.TOP_RIGHT);
		return buttons;
	}
}
