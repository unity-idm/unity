/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.tabs;

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.endpoint.common.api.services.authnlayout.AuthnLayoutConfigToUIConverter;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.AuthnLayoutColumn;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.ColumnComponent;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.PaletteButton;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components.AuthnLayoutComponentsFactory;
import io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui.components.SeparatorColumnComponent;
import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Editor for layout which is used when a single last used authN is presented
 * 
 * @author P.Piernik
 *
 */
public class WebServiceReturningLayoutEditor extends CustomField<List<AuthnElementConfiguration>>
{

	public static final String RET_COLUMN_ID = "RETUSER";

	private final MessageSource msg;
	private final Runnable valueChange = () -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), true));
	private HorizontalLayout columnLayout;
	private AuthnLayoutColumn column;
	private final Runnable dragStart = () -> column.dragOn();
	private final Runnable dragStop = () ->
	{
		column.dragOff();
		valueChange.run();
	};
	private final Consumer<ColumnComponent> removeElementListener = e ->
	{
		column.removeElement(e);
		valueChange.run();
	};

	public WebServiceReturningLayoutEditor(MessageSource msg)
	{
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setPadding(false);
		main.setWidth(100, Unit.PERCENTAGE);

		main.add(new NativeLabel(msg.getMessage("WebServiceReturningLayoutEditor.dragElement")));
		main.add(getPallete());
		Div bar = new Div();
		bar.addClassName("u-horizontalBar");
		main.add(bar);


		columnLayout = new HorizontalLayout();
		columnLayout.setWidth(50, Unit.PERCENTAGE);
		column = new AuthnLayoutColumn(msg, null, e -> column.removeElement(e), valueChange);
		column.setRemoveVisible(false);
		column.setHeaderVisible(false);
		columnLayout.add(column);

		main.add(columnLayout);
		add(main);
		setWidthFull();
	}

	private HorizontalLayout getPallete()
	{
		HorizontalLayout componentsPalette = new HorizontalLayout();
		componentsPalette.add(new PaletteButton(msg.getMessage("AuthnColumnLayoutElement.separator"),
				VaadinIcon.TEXT_LABEL, dragStart, dragStop,
				() -> new SeparatorColumnComponent(msg, removeElementListener, valueChange, dragStart, dragStop)));
		return componentsPalette;
	}

	@Override
	protected List<AuthnElementConfiguration> generateModelValue()
	{

		return AuthnLayoutConfigToUIConverter.getColumnElements(column.getElements());
	}

	@Override
	protected void setPresentationValue(List<AuthnElementConfiguration> newPresentationValue)
	{
		column.setElements(
				AuthnLayoutConfigToUIConverter.getColumnElements(newPresentationValue, new AuthnLayoutComponentsFactory(
						msg, null, removeElementListener, dragStart, dragStop, valueChange, null, null, true)));

	}

}
