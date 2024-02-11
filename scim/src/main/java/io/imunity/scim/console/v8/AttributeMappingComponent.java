/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console.v8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TabSheet.Tab;
import com.vaadin.ui.VerticalLayout;

import io.imunity.scim.config.DataArray.DataArrayType;
import io.imunity.scim.config.DataValue.DataValueType;
import io.imunity.scim.config.ReferenceAttributeMapping.ReferenceType;
import io.imunity.scim.console.mapping.AttributeDefinitionBean;
import io.imunity.scim.console.mapping.AttributeMappingBean;
import io.imunity.scim.console.mapping.DataArrayBean;
import io.imunity.scim.console.mapping.DataValueBean;
import io.imunity.scim.console.mapping.ReferenceDataBean;
import io.imunity.scim.schema.SCIMAttributeType;
import io.imunity.scim.user.mapping.evaluation.SCIMMvelContextKey;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.mvel.MVELExpressionField;

class AttributeMappingComponent extends CustomField<AttributeMappingBean>
{
	private final MessageSource msg;

	private VerticalLayout main;
	private Binder<AttributeMappingBean> binder;
	private ComboBox<DataArrayBean> dataArray;
	private AttributeMappingComponent.DataValueField dataValue;
	private AttributeMappingComponent.ReferenceField referenceEditor;
	private final AttributeEditorData editorData;
	private final Supplier<AttributeEditContext> editContextSupplier;

	AttributeMappingComponent(MessageSource msg, AttributeEditorData editorData,
			Supplier<AttributeEditContext> editContextSupplier)
	{
		this.msg = msg;
		this.editorData = editorData;
		this.editContextSupplier = editContextSupplier;
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
		dataArray.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		dataArray.setCaption(msg.getMessage("AttributeDefinitionConfigurationEditor.dataArray"));
		header.addComponent(dataArray);
		List<DataArrayBean> items = new ArrayList<>();
		items.addAll(editorData.identityTypes.stream().sorted()
				.map(i -> new DataArrayBean(DataArrayType.IDENTITY, Optional.of(i))).collect(Collectors.toList()));

		items.addAll(editorData.attributeTypes.stream().sorted()
				.map(a -> new DataArrayBean(DataArrayType.ATTRIBUTE, Optional.of(a))).collect(Collectors.toList()));
		items.add(new DataArrayBean(DataArrayType.MEMBERSHIP, Optional.empty()));

		dataArray.setItems(items);
		dataArray.setItemCaptionGenerator(i -> i != null && i.getType() != null
				? (msg.getMessage("DataArrayType." + i.getType()) + (i.getValue().isEmpty() ? "" : i.getValue().get()))
				: "");
		dataArray.setEmptySelectionAllowed(false);

		binder.forField(dataArray).bind("dataArray");

		dataValue = new DataValueField(msg, editorData);
		header.addComponent(dataValue);
		binder.forField(dataValue).bind("dataValue");
		binder.addValueChangeListener(
				e -> fireEvent(new ValueChangeEvent<>(this, binder.getBean(), e.isUserOriginated())));
		referenceEditor = new ReferenceField(msg);
		referenceEditor.addToLayout(header);
		binder.forField(referenceEditor).bind("dataReference");

	}

	public void update(AttributeDefinitionBean value)
	{
		if (value == null)
		{
			return;
		}

		dataArray.setVisible(value.isMultiValued());
		dataValue.setVisible(!(value.getType().equals(SCIMAttributeType.COMPLEX)
				|| value.getType().equals(SCIMAttributeType.REFERENCE)));
		dataValue.setMulti(value.isMultiValued(), editContextSupplier.get().complexMultiParent);
		referenceEditor.setVisible(value.getType().equals(SCIMAttributeType.REFERENCE));
		referenceEditor.setMulti(value.isMultiValued());
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

	private static class DataValueField extends CustomField<DataValueBean>
	{
		private final MessageSource msg;
		private final AttributeEditorData editorData;
		private ComboBox<DataValueBean> dataValue;
		private TabSheet tab;
		private Tab staticValueTab;
		private Tab mvelTab;
		private MVELExpressionField expression;
		private boolean multi;
		private boolean parentMulti;

		public DataValueField(MessageSource msg, AttributeEditorData editorData)
		{
			setCaption(msg.getMessage("AttributeDefinitionConfigurationEditor.dataValue"));
			this.msg = msg;
			this.editorData = editorData;
			init();
		}

		void init()
		{
			tab = new TabSheet();
			tab.addStyleName("u-logoFieldTabsheet");

			VerticalLayout mainDataLayout = new VerticalLayout();
			mainDataLayout.setMargin(new MarginInfo(true, false));
			dataValue = new ComboBox<>();
			dataValue.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
			mainDataLayout.addComponent(dataValue);
			dataValue.addValueChangeListener(
					e -> fireEvent(new ValueChangeEvent<>(this, getValue(), e.isUserOriginated())));
			dataValue.setItemCaptionGenerator(
					i -> i != null && i.getType() != null ? (msg.getMessage("DataValueType." + i.getType())
							+ (i.getValue().isEmpty() ? "" : i.getValue().get())) : "");
			dataValue.setEmptySelectionAllowed(false);
			VerticalLayout mainExpressionLayout = new VerticalLayout();
			mainExpressionLayout.setMargin(new MarginInfo(true, false));
			expression = new MVELExpressionField(msg, null, "",
					MVELExpressionContext.builder().withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
							.withEvalToKey("MVELExpressionField.evalToString").withVars(Collections.emptyMap())
							.build());
			expression.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);

			expression.addValueChangeListener(
					e -> fireEvent(new ValueChangeEvent<>(this, getValue(), e.isUserOriginated())));

			mainExpressionLayout.addComponent(expression);
			staticValueTab = tab.addTab(mainDataLayout, msg.getMessage("DataValueField.data"));
			mvelTab = tab.addTab(mainExpressionLayout, msg.getMessage("DataValueField.expression"));
			tab.addSelectedTabChangeListener(
					e -> fireEvent(new ValueChangeEvent<>(this, getValue(), e.isUserOriginated())));
			setItemsForSingleTypeSelect();
		}

		public void setMulti(boolean multiValued, boolean parentMultiValued)
		{
			if (multi == multiValued && parentMulti == parentMultiValued)
				return;
			multi = multiValued;
			parentMulti = parentMultiValued;

			if (parentMultiValued)
			{
				setAllItems();
				return;
			}
			List<DataValueBean> items = null;
			if (multiValued)
			{
				items = setItemsForMultiTypeSelect();
			} else
			{
				items = setItemsForSingleTypeSelect();
			}

			DataValueBean value = dataValue.getValue();
			if (value != null)
			{
				if (items.stream().filter(a -> a.getType().equals(value.getType())).findAny().isEmpty())
				{
					dataValue.setValue(null);
				}
			}

		}

		@Override
		public DataValueBean getValue()
		{
			if (tab.getSelectedTab() != null && tab.getSelectedTab().equals(mvelTab.getComponent()))
			{
				return new DataValueBean(DataValueType.MVEL, Optional.ofNullable(expression.getValue()));
			} else
			{
				return dataValue.getValue();
			}
		}

		private List<DataValueBean> setAllItems()
		{
			List<DataValueBean> items = getIdentitiesAndAttributesItems();
			items.add(new DataValueBean(DataValueType.ARRAY, Optional.empty()));
			dataValue.setItems(items);
			expression.setContext(
					MVELExpressionContext.builder().withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
							.withEvalToKey("MVELExpressionField.evalToString")
							.withVars(SCIMMvelContextKey.mapForMulti()).build());
			return items;
		}

		private List<DataValueBean> setItemsForSingleTypeSelect()
		{
			List<DataValueBean> items = getIdentitiesAndAttributesItems();
			dataValue.setItems(items);
			expression.setContext(
					MVELExpressionContext.builder().withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
							.withEvalToKey("MVELExpressionField.evalToString")
							.withVars(SCIMMvelContextKey.mapForSingle()).build());
			return items;
		}

		private List<DataValueBean> getIdentitiesAndAttributesItems()
		{
			List<DataValueBean> items = new ArrayList<>();
			items.addAll(editorData.identityTypes.stream().sorted()
					.map(a -> new DataValueBean(DataValueType.IDENTITY, Optional.ofNullable(a)))
					.collect(Collectors.toList()));
			items.addAll(editorData.attributeTypes.stream().sorted()
					.map(a -> new DataValueBean(DataValueType.ATTRIBUTE, Optional.ofNullable(a)))
					.collect(Collectors.toList()));
			return items;

		}

		private List<DataValueBean> setItemsForMultiTypeSelect()
		{
			List<DataValueBean> items = List.of(new DataValueBean(DataValueType.ARRAY, Optional.empty()));
			dataValue.setItems(List.of(new DataValueBean(DataValueType.ARRAY, Optional.empty())));
			expression.setContext(
					MVELExpressionContext.builder().withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
							.withEvalToKey("MVELExpressionField.evalToString")
							.withVars(SCIMMvelContextKey.mapForMulti()).build());
			return items;

		}

		@Override
		protected Component initContent()
		{
			VerticalLayout main = new VerticalLayout(tab);
			main.setMargin(false);
			return main;
		}

		@Override
		protected void doSetValue(DataValueBean value)
		{
			if (value == null)
			{
				dataValue.setValue(null);
				expression.setValue(null);
				return;
			}

			if (value.getType() == null)
			{
				tab.setSelectedTab(staticValueTab);
				return;
			}

			if (value.getType().equals(DataValueType.MVEL))
			{
				expression.setValue(value.getValue().orElse(""));
				tab.setSelectedTab(mvelTab);
			} else
			{
				dataValue.setValue(value);
				tab.setSelectedTab(staticValueTab);
			}
		}
	}

	private static class ReferenceField extends CustomField<ReferenceDataBean>
	{
		private final MessageSource msg;
		private ComboBox<ReferenceType> refToTypeCombo;
		private MVELExpressionField expression;

		public ReferenceField(MessageSource msg)
		{
			this.msg = msg;
			init();
		}

		void init()
		{
			refToTypeCombo = new ComboBox<>(msg.getMessage("ReferenceField.reference"));
			refToTypeCombo.setItems(ReferenceType.values());
			refToTypeCombo.setValue(ReferenceType.GENERIC);
			refToTypeCombo.setEmptySelectionAllowed(false);
			refToTypeCombo.setItemCaptionGenerator(s -> s == null ? "" : msg.getMessage("ReferenceType." + s));

			expression = new MVELExpressionField(msg, msg.getMessage("ReferenceField.referenceUri"), "",
					MVELExpressionContext.builder().withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
							.withEvalToKey("MVELExpressionField.evalToUri").withVars(SCIMMvelContextKey.mapForSingle())
							.build());
			expression.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);

			refToTypeCombo.addValueChangeListener(e ->
			{
				updateExpressionFiled(e);
			});

			expression.addValueChangeListener(
					e -> fireEvent(new ValueChangeEvent<>(this, getValue(), e.isUserOriginated())));
		}

		private void updateExpressionFiled(ValueChangeEvent<ReferenceType> e)
		{
			MVELExpressionContext context = expression.getContext();
			if (refToTypeCombo.getValue().equals(ReferenceType.GENERIC))
			{
				expression.setCaption(msg.getMessage("ReferenceField.referenceUri"));
				expression.setContext(MVELExpressionContext.builder()
						.withTitleKey("AttributeDefinitionConfigurationEditor.referenceGeneralDataValue")
						.withEvalToKey("MVELExpressionField.evalToUri").withVars(context.vars).build());

			} else
			{
				expression.setCaption(msg.getMessage("ReferenceField.referencedResourceId"));
				expression.setContext(MVELExpressionContext.builder().withTitleKey(getMvelEditorTitleKey())
						.withEvalToKey(getMvelEditorTypeKey()).withVars(context.vars).build());
			}

			fireEvent(new ValueChangeEvent<>(this, getValue(), e.isUserOriginated()));
		}

		private String getMvelEditorTitleKey()
		{
			switch (refToTypeCombo.getValue())
			{
			case USER:
				return "AttributeDefinitionConfigurationEditor.referenceUserDataValue";
			case GROUP:
				return "AttributeDefinitionConfigurationEditor.referenceGroupDataValue";
			default:
				return "AttributeDefinitionConfigurationEditor.referenceGeneralDataValue";
			}
		}

		private String getMvelEditorTypeKey()
		{
			switch (refToTypeCombo.getValue())
			{
			case USER:
				return "MVELExpressionField.evalToStringWithUserId";
			case GROUP:
				return "MVELExpressionField.evalToStringWithGroupPath";
			default:
				return "MVELExpressionField.evalToUri";
			}
		}

		@Override
		public ReferenceDataBean getValue()
		{
			return new ReferenceDataBean(refToTypeCombo.getValue(), expression.getValue());
		}

		@Override
		protected Component initContent()
		{
			FormLayoutWithFixedCaptionWidth main = FormLayoutWithFixedCaptionWidth.withShortCaptions();
			main.setMargin(false);
			main.addComponent(refToTypeCombo);
			main.addComponent(expression);
			return main;

		}

		@Override
		protected void doSetValue(ReferenceDataBean value)
		{
			if (value == null)
				return;
			refToTypeCombo.setValue(value.getType());
			expression.setValue(value.getExpression());
		}

		void addToLayout(Layout layout)
		{
			layout.addComponent(refToTypeCombo);
			layout.addComponent(expression);
		}

		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);
			refToTypeCombo.setVisible(visible);
			expression.setVisible(visible);
		}

		public void setMulti(boolean multi)
		{
			MVELExpressionContext context = expression.getContext();
			expression.setContext(MVELExpressionContext.builder().withTitleKey(context.titleKey)
					.withEvalToKey(context.evalToKey)
					.withVars(multi ? SCIMMvelContextKey.mapForMulti() : SCIMMvelContextKey.mapForSingle()).build());
		}
	}

}