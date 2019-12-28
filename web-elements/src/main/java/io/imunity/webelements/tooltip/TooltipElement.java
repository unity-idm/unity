/**********************************************************************
 *                     Copyright (c) 2019, Jirav100
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package io.imunity.webelements.tooltip;

import org.vaadin.marcus.MouseEvents;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.themes.ValoTheme;

class TooltipElement extends CustomComponent
{
	private static final int DEFAULT_POPUP_WIDTH_EM = 30;

	TooltipElement(String tooltipInfo)
	{
		HorizontalLayout hl = new HorizontalLayout();
		
		if (tooltipInfo != null)
		{
			PopupView tooltipPopupView = getTooltipPopupView(tooltipInfo);
			Button helpButton = getHelpButton(tooltipPopupView);
			hl.addComponents(helpButton, tooltipPopupView);
		}
		
		hl.setMargin(false);
		hl.setSpacing(false);
		setCompositionRoot(hl);
	}
	
	private Button getHelpButton(final PopupView pv)
	{
		Button helpButton = new Button();
		helpButton.addStyleName(ValoTheme.BUTTON_QUIET);
		helpButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		helpButton.addStyleName("tooltipButton");
		helpButton.setIcon(VaadinIcons.QUESTION_CIRCLE);
		MouseEvents mouseEvents = MouseEvents.enableFor(helpButton);
		mouseEvents.addMouseOverListener(() -> pv.setPopupVisible(true));
		return helpButton;
	}

	private PopupView getTooltipPopupView(String tooltipInfo)
	{
		Label tooltip = new Label(tooltipInfo);
		int width = tooltipInfo.length() > DEFAULT_POPUP_WIDTH_EM 
				? DEFAULT_POPUP_WIDTH_EM 
				: tooltipInfo.length();
		tooltip.setWidth(width, Unit.EM);
		PopupView pv = new PopupView("", tooltip);
		pv.setHideOnMouseOut(true);
		return pv;
	}
}
