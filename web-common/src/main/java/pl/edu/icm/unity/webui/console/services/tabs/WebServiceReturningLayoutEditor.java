/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.tabs;

import java.util.List;
import java.util.function.Consumer;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.console.services.authnlayout.AuthnLayoutConfigToUIConverter;
import pl.edu.icm.unity.webui.console.services.authnlayout.configuration.elements.AuthnElementConfiguration;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.AuthnLayoutColumn;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.ColumnComponent;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.PaletteButton;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.components.AuthnLayoutComponentsFactory;
import pl.edu.icm.unity.webui.console.services.authnlayout.ui.components.SeparatorColumnComponent;

/**
 * Editor for layout which is used when a single last used authN is presented
 * 
 * @author P.Piernik
 *
 */
public class WebServiceReturningLayoutEditor extends CustomField<List<AuthnElementConfiguration>>
{
	public static final String RET_COLUMN_ID = "RETUSER";

	private MessageSource msg;
	private HorizontalLayout columnLayout;
	private AuthnLayoutColumn column;
	private Runnable valueChange = () -> fireEvent(
			new ValueChangeEvent<List<AuthnElementConfiguration>>(this, getValue(), true));
	private Runnable dragStart = () -> column.dragOn();
	private Runnable dragStop = () -> {
		column.dragOff();
		valueChange.run();
	};
	private Consumer<ColumnComponent> removeElementListener = e -> {
		column.removeElement(e);
		valueChange.run();
	};
	private VerticalLayout main;

	public WebServiceReturningLayoutEditor(MessageSource msg)
	{
		this.msg = msg;
		initUI();
	}

	private void initUI()
	{

		main = new VerticalLayout();
		main.setMargin(false);
		main.setWidth(100, Unit.PERCENTAGE);

		main.addComponent(new Label(msg.getMessage("WebServiceReturningLayoutEditor.dragElement")));
		main.addComponent(getPallete());
		main.addComponent(HtmlTag.horizontalLine());

		columnLayout = new HorizontalLayout();
		columnLayout.setWidth(50, Unit.PERCENTAGE);
		column = new AuthnLayoutColumn(msg, null, e -> column.removeElement(e), valueChange);
		column.setRemoveVisible(false);
		column.setHeaderVisible(false);
		columnLayout.addComponent(column);

		main.addComponent(columnLayout);
	}

	private HorizontalLayout getPallete()
	{
		HorizontalLayout componentsPalette = new HorizontalLayout();
		componentsPalette.addComponent(new PaletteButton(msg.getMessage("AuthnColumnLayoutElement.separator"),
				Images.text.getResource(), dragStart, dragStop, () -> new SeparatorColumnComponent(msg,
						removeElementListener, valueChange, dragStart, dragStop)));
		return componentsPalette;
	}

	@Override
	public List<AuthnElementConfiguration> getValue()
	{
		return AuthnLayoutConfigToUIConverter.getColumnElements(column.getElements());
	}

	@Override
	protected Component initContent()
	{
		return main;
	}

	@Override
	protected void doSetValue(List<AuthnElementConfiguration> value)
	{
		column.setElements(AuthnLayoutConfigToUIConverter.getColumnElements(value,
				new AuthnLayoutComponentsFactory(msg, null, removeElementListener, dragStart, dragStop,
						valueChange, null, null, true)));
	}

}
