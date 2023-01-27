/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common.plugins.credentials.password;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.progressbar.ProgressBarVariant;


class PasswordProgressBar extends VerticalLayout
{
	private final LabelWithIcon title = new LabelWithIcon();
	private final ProgressBar progressBar = new ProgressBar();
	private final LabelWithIcon minLengthStatus = new LabelWithIcon();
	private final LabelWithIcon minClassesStatus = new LabelWithIcon();
	private final LabelWithIcon sequencesStatus = new LabelWithIcon();
	private final Label hint = new Label();

	public PasswordProgressBar(boolean minLengthStatus, boolean minClassesStatus, boolean sequencesStatus)
	{
		this.minLengthStatus.setVisible(minLengthStatus);
		this.minClassesStatus.setVisible(minClassesStatus);
		this.sequencesStatus.setVisible(sequencesStatus);
		getStyle().set("gap", "0");
		hint.getStyle().set("font-style", "italic");

		add(title, progressBar, this.minLengthStatus, this.minClassesStatus, this.sequencesStatus, hint);
	}

	public void setValue(double value)
	{
		progressBar.setValue(value);
	}

	public void setColorToGreen()
	{
		progressBar.removeThemeVariants(ProgressBarVariant.LUMO_CONTRAST);
		progressBar.addThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
	}

	public void setColorToRed()
	{
		progressBar.removeThemeVariants(ProgressBarVariant.LUMO_SUCCESS);
		progressBar.addThemeVariants(ProgressBarVariant.LUMO_CONTRAST);
	}

	public void setHint(String txt)
	{
		hint.setText(txt);
	}

	public void setTitle(String txt)
	{
		title.setLabel(txt);
	}

	public void setTitleIcon(boolean status)
	{
		title.setIcon(createIcon(status));
	}

	public void setMinLengthStatus(String txt, boolean status)
	{
		minLengthStatus.setLabel(txt);
		minLengthStatus.setIcon(createIcon(status));
	}

	public void setMinClassesStatus(String txt, boolean status)
	{
		minClassesStatus.setLabel(txt);
		minClassesStatus.setIcon(createIcon(status));
	}

	public void setSequencesStatus(String txt, boolean status)
	{
		sequencesStatus.setLabel(txt);
		sequencesStatus.setIcon(createIcon(status));
	}

	private Icon createIcon(boolean status)
	{
		Icon icon;
		if(status)
		{
			icon = VaadinIcon.CHECK_CIRCLE_O.create();
		}
		else
		{
			icon = VaadinIcon.EXCLAMATION_CIRCLE_O.create();
			icon.getStyle().set("color", "red");
		}
		icon.setSize("var(--lumo-icon-size-s)");
		return icon;
	}

	static class LabelWithIcon extends HorizontalLayout
	{
		private final Div iconDiv = new Div();
		private final Label label = new Label();

		public LabelWithIcon()
		{
			getStyle().set("gap", "0.3em");
			iconDiv.getStyle().set("display", "inline");
			add(iconDiv, label);
		}

		public void setLabel(String txt)
		{
			label.setText(txt);
		}

		public void setIcon(Icon icon)
		{
			iconDiv.removeAll();
			iconDiv.add(icon);
		}
	}
}
