/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.Collections;
import java.util.List;

import com.vaadin.data.Binder;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import io.imunity.scim.scheme.SCIMAttributeType;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.mvel.MVELExpressionField;

// TODO UY-1219
class AttributeMappingComponent extends CustomField<AttributeMappingBean>
{
	private final MessageSource msg;
	
	private VerticalLayout main;
	private Binder<AttributeMappingBean> binder;
	private ComboBox<String> dataArray;
	private AttributeMappingComponent.DataValueField dataValue;
	private AttributeMappingComponent.ReferenceEditor referenceEditor;

	AttributeMappingComponent(MessageSource msg)
	{
		this.msg = msg;
		init();
	}

	private void init()
	{
		binder = new Binder<>(AttributeMappingBean.class);
		main = new VerticalLayout();
		main.setMargin(false);
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(false);
		main.addComponent(header);

		dataArray = new ComboBox<>();
		dataArray.setCaption(msg.getMessage("AttributeDefinitionConfigurationEditor.dataArray"));
		header.addComponent(dataArray);
		dataArray.setItems(
				List.of("Attributes:name", "Attributes:email", "Group membership", "Identities: username"));
		dataValue = new DataValueField(msg);
		header.addComponent(dataValue);

		referenceEditor = new ReferenceEditor(msg);
		header.addComponent(referenceEditor);
	}

	public void update(AttributeDefinitionBean value)
	{
		if (value == null)
			return;
		dataArray.setVisible(value.isMultiValued());
		dataValue.setVisible(!(value.getType().equals(SCIMAttributeType.COMPLEX)
				|| value.getType().equals(SCIMAttributeType.REFERENCE)));
		referenceEditor.setVisible(value.getType().equals(SCIMAttributeType.REFERENCE));
	}

	@Override
	public AttributeMappingBean getValue()
	{
		return binder.getBean();
	}

	@Override
	protected Component initContent()
	{
		return main;
	}

	@Override
	protected void doSetValue(AttributeMappingBean value)
	{
		binder.setBean(value);
	}

	private static class DataValueField extends CustomField<String>
	{
		private final MessageSource msg;
		private TabSheet tab;

		public DataValueField(MessageSource msg)
		{
			setCaption(msg.getMessage("AttributeDefinitionConfigurationEditor.dataValue"));
			this.msg = msg;
		}

		@Override
		public String getValue()
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected Component initContent()
		{
			tab = new TabSheet();
			tab.addStyleName("u-logoFieldTabsheet");
			VerticalLayout mainDataLayout = new VerticalLayout();
			mainDataLayout.setMargin(new MarginInfo(true, false));
			ComboBox<String> dataValue = new ComboBox<>();
			mainDataLayout.addComponent(dataValue);
			dataValue.setItems(
					List.of("Attribute value:name", "Attribute value:email", "Identity value: username", "Array"));

			VerticalLayout mainExpressionLayout = new VerticalLayout();
			mainExpressionLayout.setMargin(new MarginInfo(true, false));
			MVELExpressionField expression = new MVELExpressionField(msg, null, "",
					MVELExpressionContext.builder().withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
							.withEvalToKey("MVELExpressionField.evalToBoolean").withVars(Collections.emptyMap())
							.build());
			mainExpressionLayout.addComponent(expression);
			tab.addTab(mainDataLayout, "Data");
			tab.addTab(mainExpressionLayout, "Expression");

			VerticalLayout main = new VerticalLayout(tab);
			main.setMargin(false);
			return main;

		}

		@Override
		protected void doSetValue(String value)
		{
			// TODO Auto-generated method stub

		}
	}

	private static class ReferenceEditor extends CustomField<String>
	{
		private final MessageSource msg;

		public ReferenceEditor(MessageSource msg)
		{
			setCaption(msg.getMessage("AttributeDefinitionConfigurationEditor.dataValue"));
			this.msg = msg;
		}

		@Override
		public String getValue()
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected Component initContent()
		{
			FormLayoutWithFixedCaptionWidth main = FormLayoutWithFixedCaptionWidth.withShortCaptions();
			main.setMargin(false);
			ComboBox<String> refToTypeCombo = new ComboBox<>("Reference to:");
			refToTypeCombo.setItems("User", "Group", "Generic resource");
			main.addComponent(refToTypeCombo);

			MVELExpressionField expression = new MVELExpressionField(msg, "Resource:", "",
					MVELExpressionContext.builder().withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
							.withEvalToKey("MVELExpressionField.evalToBoolean").withVars(Collections.emptyMap())
							.build());
			main.addComponent(expression);
			return main;

		}

		@Override
		protected void doSetValue(String value)
		{
			// TODO Auto-generated method stub
		}
	}

}