/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.binder.Binder;

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
import io.imunity.vaadin.elements.CSSVars;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.endpoint.common.api.HtmlTooltipFactory;
import io.imunity.vaadin.endpoint.common.mvel.MVELExpressionField;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.mvel.MVELExpressionContext;

class AttributeMappingComponent extends CustomField<AttributeMappingBean>
{
	private final MessageSource msg;
	private final AttributeEditorData editorData;
	private final Supplier<AttributeEditContext> editContextSupplier;
	private final HtmlTooltipFactory toolTipFactory;	
	private VerticalLayout main;
	private Binder<AttributeMappingBean> binder;
	private ComboBox<DataArrayBean> dataArray;
	private FormItem dataArrayFormItem;
	private AttributeMappingComponent.DataValueField dataValue;
	private FormItem dataValueFormItem;
	private AttributeMappingComponent.ReferenceField referenceEditor;

	AttributeMappingComponent(MessageSource msg, HtmlTooltipFactory toolTipFactory, AttributeEditorData editorData,
			Supplier<AttributeEditContext> editContextSupplier)
	{
		this.toolTipFactory = toolTipFactory;
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
		main.setPadding(false);
		FormLayout header = new FormLayout();
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		main.add(header);

		dataArray = new ComboBox<>();
		dataArray.setWidth(CSSVars.TEXT_FIELD_BIG.value());
		dataArrayFormItem = header.addFormItem(dataArray,
				msg.getMessage("AttributeDefinitionConfigurationEditor.dataArray"));
		List<DataArrayBean> items = new ArrayList<>();
		items.addAll(editorData.identityTypes.stream()
				.sorted()
				.map(i -> new DataArrayBean(DataArrayType.IDENTITY, Optional.of(i)))
				.collect(Collectors.toList()));
		items.addAll(editorData.attributeTypes.stream()
				.sorted()
				.map(a -> new DataArrayBean(DataArrayType.ATTRIBUTE, Optional.of(a)))
				.collect(Collectors.toList()));
		items.add(new DataArrayBean(DataArrayType.MEMBERSHIP, Optional.empty()));

		dataArray.setItems(items);
		dataArray.setItemLabelGenerator(
				i -> i != null && i.getType() != null ? (msg.getMessage("DataArrayType." + i.getType()) + (i.getValue()
						.isEmpty() ? ""
								: i.getValue()
										.get()))
						: "");

		binder.forField(dataArray)
				.bind("dataArray");

		dataValue = new DataValueField(msg, toolTipFactory, editorData);
		dataValueFormItem = header.addFormItem(dataValue,
				msg.getMessage("AttributeDefinitionConfigurationEditor.dataValue"));
		binder.forField(dataValue)
				.bind("dataValue");

		referenceEditor = new ReferenceField(msg, toolTipFactory);
		referenceEditor.addToLayout(header);
		binder.forField(referenceEditor)
				.bind("dataReference");

		binder.addValueChangeListener(e -> fire(e));

		add(main);
	}

	void fire(ValueChangeEvent<?> e)
	{
		fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient()));
	}

	public void update(AttributeDefinitionBean value)
	{
		if (value == null)
		{
			return;
		}

		dataArrayFormItem.setVisible(value.isMultiValued());
		dataValueFormItem.setVisible(!(value.getType()
				.equals(SCIMAttributeType.COMPLEX)
				|| value.getType()
						.equals(SCIMAttributeType.REFERENCE)));
		dataValue.setMulti(value.isMultiValued(), editContextSupplier.get().complexMultiParent);
		referenceEditor.setVisible(value.getType()
				.equals(SCIMAttributeType.REFERENCE));
		referenceEditor.setMulti(value.isMultiValued());
	}

	@Override
	protected AttributeMappingBean generateModelValue()
	{
		return binder.getBean();
	}

	@Override
	protected void setPresentationValue(AttributeMappingBean newPresentationValue)
	{
		binder.setBean(newPresentationValue);

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
		private final HtmlTooltipFactory htmlTooltipFactory;

		public DataValueField(MessageSource msg, HtmlTooltipFactory htmlTooltipFactory, AttributeEditorData editorData)
		{
			this.htmlTooltipFactory = htmlTooltipFactory;
			this.msg = msg;
			this.editorData = editorData;
			init();
		}

		void init()
		{
			tab = new TabSheet();
			tab.addClassName(CssClassNames.TABSHEET_FULL.getName());

			VerticalLayout mainDataLayout = new VerticalLayout();
			mainDataLayout.setMargin(false);
			mainDataLayout.setPadding(false);
			dataValue = new ComboBox<>();
			dataValue.setWidth(CSSVars.TEXT_FIELD_BIG.value());
			mainDataLayout.add(dataValue);
			dataValue.addValueChangeListener(
					e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient())));
			dataValue.setItemLabelGenerator(i -> i != null && i.getType() != null
					? (msg.getMessage("DataValueType." + i.getType()) + (i.getValue()
							.isEmpty() ? ""
									: i.getValue()
											.get()))
					: "");
			VerticalLayout mainExpressionLayout = new VerticalLayout();
			mainExpressionLayout.setMargin(false);
			mainExpressionLayout.setPadding(false);
			expression = new MVELExpressionField(msg, null, null, MVELExpressionContext.builder()
					.withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
					.withEvalToKey("MVELExpressionField.evalToString")
					.withVars(Collections.emptyMap())
					.build(), htmlTooltipFactory);
			expression.setWidth(CSSVars.TEXT_FIELD_BIG.value());
			expression.addValueChangeListener(
					e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient())));

			mainExpressionLayout.add(expression);
			staticValueTab = tab.add(msg.getMessage("DataValueField.data"), mainDataLayout);
			mvelTab = tab.add(msg.getMessage("DataValueField.expression"), mainExpressionLayout);
			tab.addSelectedChangeListener(
					e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient())));
			setItemsForSingleTypeSelect();

			VerticalLayout tabs = new VerticalLayout(tab);
			tabs.setMargin(false);
			tabs.setPadding(false);
			add(tabs);
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
				if (items.stream()
						.filter(a -> a.getType()
								.equals(value.getType()))
						.findAny()
						.isEmpty())
				{
					dataValue.setValue(null);
				}
			}

		}

		@Override
		public DataValueBean getValue()
		{
			if (tab.getSelectedTab() != null && tab.getSelectedTab()
					.equals(mvelTab))
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
			expression.setContext(MVELExpressionContext.builder()
					.withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
					.withEvalToKey("MVELExpressionField.evalToString")
					.withVars(SCIMMvelContextKey.mapForMulti())
					.build());
			return items;
		}

		private List<DataValueBean> setItemsForSingleTypeSelect()
		{
			List<DataValueBean> items = getIdentitiesAndAttributesItems();
			dataValue.setItems(items);
			expression.setContext(MVELExpressionContext.builder()
					.withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
					.withEvalToKey("MVELExpressionField.evalToString")
					.withVars(SCIMMvelContextKey.mapForSingle())
					.build());
			return items;
		}

		private List<DataValueBean> getIdentitiesAndAttributesItems()
		{
			List<DataValueBean> items = new ArrayList<>();
			items.addAll(editorData.identityTypes.stream()
					.sorted()
					.map(a -> new DataValueBean(DataValueType.IDENTITY, Optional.ofNullable(a)))
					.collect(Collectors.toList()));
			items.addAll(editorData.attributeTypes.stream()
					.sorted()
					.map(a -> new DataValueBean(DataValueType.ATTRIBUTE, Optional.ofNullable(a)))
					.collect(Collectors.toList()));
			return items;

		}

		private List<DataValueBean> setItemsForMultiTypeSelect()
		{
			List<DataValueBean> items = List.of(new DataValueBean(DataValueType.ARRAY, Optional.empty()));
			dataValue.setItems(List.of(new DataValueBean(DataValueType.ARRAY, Optional.empty())));
			expression.setContext(MVELExpressionContext.builder()
					.withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
					.withEvalToKey("MVELExpressionField.evalToString")
					.withVars(SCIMMvelContextKey.mapForMulti())
					.build());
			return items;

		}

		@Override
		protected DataValueBean generateModelValue()
		{
			return getValue();
		}

		@Override
		protected void setPresentationValue(DataValueBean value)
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

			if (value.getType()
					.equals(DataValueType.MVEL))
			{
				expression.setValue(value.getValue()
						.orElse(""));
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
		private HtmlTooltipFactory htmlTooltipFactory;
		private NativeLabel expressionFormItemLabel;
		private FormItem referenceFormItem;
		private FormItem expressionFormItem;

		public ReferenceField(MessageSource msg, HtmlTooltipFactory htmlTooltipFactory)
		{
			this.htmlTooltipFactory = htmlTooltipFactory;
			this.msg = msg;
			init();
		}

		void init()
		{
			refToTypeCombo = new ComboBox<>();
			refToTypeCombo.setItems(ReferenceType.values());
			refToTypeCombo.setValue(ReferenceType.GENERIC);
			refToTypeCombo.setItemLabelGenerator(s -> s == null ? "" : msg.getMessage("ReferenceType." + s));

			expression = new MVELExpressionField(msg, null, null, MVELExpressionContext.builder()
					.withTitleKey("AttributeDefinitionConfigurationEditor.dataValue")
					.withEvalToKey("MVELExpressionField.evalToUri")
					.withVars(SCIMMvelContextKey.mapForSingle())
					.build(), htmlTooltipFactory);
			expression.setWidth(CSSVars.TEXT_FIELD_BIG.value());

			refToTypeCombo.addValueChangeListener(e ->
			{
				updateExpressionFiled(e);
			});

			expression.addValueChangeListener(
					e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient())));

			FormLayout main = new FormLayout();
			main.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
			main.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
			referenceFormItem = main.addFormItem(refToTypeCombo, msg.getMessage("ReferenceField.reference"));
			expressionFormItem = main.addFormItem(expression, msg.getMessage("ReferenceField.referenceUri"));
			add(main);
		}

		private void updateExpressionFiled(ValueChangeEvent<ReferenceType> e)
		{
			MVELExpressionContext context = expression.getContext();
			if (refToTypeCombo.getValue()
					.equals(ReferenceType.GENERIC))
			{
				if (expressionFormItemLabel != null)
					expressionFormItemLabel.setText(msg.getMessage("ReferenceField.referenceUri"));
				expression.setContext(MVELExpressionContext.builder()
						.withTitleKey("AttributeDefinitionConfigurationEditor.referenceGeneralDataValue")
						.withEvalToKey("MVELExpressionField.evalToUri")
						.withVars(context.vars)
						.build());

			} else
			{
				if (expressionFormItemLabel != null)
					expressionFormItemLabel.setText(msg.getMessage("ReferenceField.referencedResourceId"));
				expression.setContext(MVELExpressionContext.builder()
						.withTitleKey(getMvelEditorTitleKey())
						.withEvalToKey(getMvelEditorTypeKey())
						.withVars(context.vars)
						.build());
			}

			fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient()));
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

		void addToLayout(FormLayout layout)
		{
			referenceFormItem = layout.addFormItem(refToTypeCombo, msg.getMessage("ReferenceField.reference"));
			expressionFormItemLabel = new NativeLabel(msg.getMessage("ReferenceField.referenceUri"));
			expressionFormItem = layout.addFormItem(expression, expressionFormItemLabel);

		}

		@Override
		public void setVisible(boolean visible)
		{
			super.setVisible(visible);
			referenceFormItem.setVisible(visible);
			expressionFormItem.setVisible(visible);
		}

		public void setMulti(boolean multi)
		{
			MVELExpressionContext context = expression.getContext();
			expression.setContext(MVELExpressionContext.builder()
					.withTitleKey(context.titleKey)
					.withEvalToKey(context.evalToKey)
					.withVars(multi ? SCIMMvelContextKey.mapForMulti() : SCIMMvelContextKey.mapForSingle())
					.build());
		}

		@Override
		protected ReferenceDataBean generateModelValue()
		{
			return getValue();
		}

		@Override
		protected void setPresentationValue(ReferenceDataBean value)
		{
			if (value == null)
				return;
			refToTypeCombo.setValue(value.getType());
			expression.setValue(value.getExpression());

		}
	}

}