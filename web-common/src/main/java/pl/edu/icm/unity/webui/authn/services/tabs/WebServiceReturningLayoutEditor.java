/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.services.tabs;

import java.util.Properties;
import java.util.function.Consumer;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.services.authnlayout.AuthnLayoutColumn;
import pl.edu.icm.unity.webui.authn.services.authnlayout.AuthnLayoutPropertiesHelper;
import pl.edu.icm.unity.webui.authn.services.authnlayout.ColumnElement;
import pl.edu.icm.unity.webui.authn.services.authnlayout.PalleteButton;
import pl.edu.icm.unity.webui.authn.services.layout.elements.SeparatorColumnElement;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

/**
 * Editor for layout which is used when a single last used authN is presented
 * 
 * @author P.Piernik
 *
 */
public class WebServiceReturningLayoutEditor extends CustomField<Properties>
{
	public static final String RET_COLUMN_ID = "RETUSER";

	private UnityMessageSource msg;
	private HorizontalLayout columnLayout;
	private AuthnLayoutColumn column;
	private Runnable valueChange = () -> fireEvent(new ValueChangeEvent<Properties>(this, getValue(), true));
	private Runnable dragStart = () -> column.dragOn();
	private Runnable dragStop = () -> {column.dragOff(); valueChange.run();};
	private Consumer<ColumnElement> removeElementListener = e -> {column.removeElement(e); valueChange.run();};
	private VerticalLayout main;

	public WebServiceReturningLayoutEditor(UnityMessageSource msg)
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
		columnLayout.addComponent(column);

		main.addComponent(columnLayout);
	}

	private HorizontalLayout getPallete()
	{
		HorizontalLayout componentsPalette = new HorizontalLayout();
		componentsPalette.addComponent(new PalleteButton(msg.getMessage("AuthnColumnLayoutElement.separator"),
				Images.text.getResource(), dragStart, dragStop,
				() -> new SeparatorColumnElement(msg, removeElementListener, valueChange, dragStart, dragStop)));
		return componentsPalette;
	}

	@Override
	public Properties getValue()
	{
		Properties raw = new Properties();
		String content = AuthnLayoutPropertiesHelper.getColumnContent(msg, column, RET_COLUMN_ID, raw);
		raw.put(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY_LAYOUT,
				content);
		return raw;
	}

	@Override
	protected Component initContent()
	{
		return main;
	}

	@Override
	protected void doSetValue(Properties value)
	{
		column.setElements(AuthnLayoutPropertiesHelper.getReturingUserColumnElements(new VaadinEndpointProperties(value), msg,
				removeElementListener, valueChange, dragStart, dragStop));
	}

}
