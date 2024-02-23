/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import io.imunity.attr.introspection.config.Attribute;
import io.imunity.attr.introspection.config.AttributePolicy;
import io.imunity.vaadin.endpoint.common.api.services.idp.CollapsableGrid.Editor;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IdPInfo;
import pl.edu.icm.unity.engine.api.authn.IdPInfo.IdpGroup;
import pl.edu.icm.unity.webui.common.FormValidationException;

class AttributePolicyConfigurationEditor extends Editor<AttributePolicy>
{
	private final MessageSource msg;

	private Binder<AttributePolicyBean> binder;
	private VerticalLayout main;
	private TextField name;
	private Map<String, IdPInfo> idPs;
	private Map<String, IdpGroup> IdPsGroups;

	AttributePolicyConfigurationEditor(MessageSource msg, List<IdPInfo> idps)
	{
		this.msg = msg;
		this.idPs = idps.stream()
				.collect(Collectors.toMap(i -> i.id, i -> i, (i1, i2) -> i1))
				.values()
				.stream()
				.collect(Collectors.toMap(p -> p.id, p -> p));
		this.IdPsGroups = idps.stream()
				.distinct()
				.map(p -> p.group)
				.filter(g -> !g.isEmpty())
				.distinct()
				.collect(Collectors.toMap(g -> g.get().id, g -> g.get()));
		init();
	}

	private void init()
	{
		binder = new Binder<>(AttributePolicyBean.class);
		main = new VerticalLayout();
		FormLayout header = new FormLayout();
		header.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		header.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		main.add(header);

		name = new TextField();
		header.addFormItem(name, msg.getMessage("AttributePolicyConfigurationEditor.name"));
		binder.forField(name)
				.bind("name");

		MultiSelectComboBox<String> targetIdps = new MultiSelectComboBox<>();
		targetIdps.setItemLabelGenerator(p -> getDisplayeName(p));
		targetIdps.setWidth(TEXT_FIELD_BIG.value());
		targetIdps.setItems(idPs.keySet());
		binder.forField(targetIdps)
				.withConverter(List::copyOf, l -> new HashSet<>(l == null ? new ArrayList<>() : l))
				.bind("targetIdps");
		header.addFormItem(targetIdps, msg.getMessage("AttributePolicyConfigurationEditor.targetIdps"));

		MultiSelectComboBox<String> targetFederations = new MultiSelectComboBox<>();
		targetFederations.setItemLabelGenerator(p -> getGroupDisplayeName(p));
		targetFederations.setWidth(TEXT_FIELD_BIG.value());
		targetFederations.setItems(IdPsGroups.keySet());
		binder.forField(targetFederations)
				.withConverter(List::copyOf, l -> new HashSet<>(l == null ? new ArrayList<>() : l))
				.bind("targetFederations");
		header.addFormItem(targetFederations, msg.getMessage("AttributePolicyConfigurationEditor.targetFederations"));

		AttributesGrid attributes = new AttributesGrid(msg);
		binder.forField(attributes)
				.bind("attributes");
		main.add(attributes);

		binder.addValueChangeListener(e -> new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient()));
		add(main);
		setSizeFull();
	}

	private String getGroupDisplayeName(String p)
	{
		IdpGroup value = IdPsGroups.get(p);
		if (value == null)
			return p;
		return value.displayedName.orElse(p);
	}

	private String getDisplayeName(String p)
	{
		IdPInfo value = idPs.get(p);
		if (value == null)
			return p;
		if (value.displayedName.isEmpty())
			return p;
		return value.displayedName.get()
				.getValue(msg);
	}

	@Override
	protected String getHeaderText()
	{
		return name.getValue() == null || name.getValue()
				.isEmpty() ? "" : name.getValue();

	}

	@Override
	protected void validate() throws FormValidationException
	{
		if (binder.validate()
				.hasErrors())
		{
			throw new FormValidationException(
					msg.getMessage("AttributePolicyConfigurationEditor.invalidConfiguration"));
		}
	}

	@Override
	public AttributePolicy getValue()
	{
		if (binder.validate()
				.hasErrors())
			return null;
		AttributePolicyBean bean = binder.getBean();
		if (bean == null)
			return null;
		return new AttributePolicy(bean.getName(), bean.getAttributes(), bean.getTargetIdps(),
				bean.getTargetFederations());
	}

	@Override
	protected AttributePolicy generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(AttributePolicy value)
	{
		binder.setBean(
				new AttributePolicyBean(value.name, value.attributes, value.targetIdps, value.targetFederations));

	}

	public class AttributePolicyBean
	{
		private String name;
		private List<Attribute> attributes;
		private List<String> targetIdps;
		private List<String> targetFederations;

		public AttributePolicyBean()
		{
		}

		public AttributePolicyBean(String name, List<Attribute> attributes, List<String> targetIdps,
				List<String> targetFederations)
		{
			this.name = name;
			this.attributes = attributes;
			this.targetIdps = targetIdps;
			this.targetFederations = targetFederations;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public List<Attribute> getAttributes()
		{
			return attributes;
		}

		public void setAttributes(List<Attribute> attributes)
		{
			this.attributes = attributes;
		}

		public List<String> getTargetIdps()
		{
			return targetIdps;
		}

		public void setTargetIdps(List<String> targetIdps)
		{
			this.targetIdps = targetIdps;
		}

		public List<String> getTargetFederations()
		{
			return targetFederations;
		}

		public void setTargetFederations(List<String> targetFederations)
		{
			this.targetFederations = targetFederations;
		}
	}
}
