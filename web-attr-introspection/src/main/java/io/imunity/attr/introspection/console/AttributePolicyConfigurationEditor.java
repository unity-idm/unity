/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.attr.introspection.config.Attribute;
import io.imunity.attr.introspection.config.AttributePolicy;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.authn.IdPInfo;
import pl.edu.icm.unity.types.authn.IdPInfo.IdpGroup;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ListOfDnDCollapsableElements.Editor;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;

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
		this.idPs = idps.stream().filter(distinctByKey(i -> i.id)).collect(Collectors.toMap(p -> p.id, p -> p));
		this.IdPsGroups = idps.stream().distinct().map(p -> p.group).filter(g -> !g.isEmpty()).distinct()
				.collect(Collectors.toMap(g -> g.get().id, g -> g.get()));
		init();
	}

	private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor)
	{
		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}
	
	private void init()
	{
		binder = new Binder<>(AttributePolicyBean.class);
		main = new VerticalLayout();
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(false);
		main.addComponent(header);

		name = new TextField(msg.getMessage("AttributePolicyConfigurationEditor.name"));
		header.addComponent(name);
		binder.forField(name).bind("name");
		ChipsWithDropdown<String> targetIdps = new ChipsWithDropdown<>(p -> getDisplayeName(p), true);
		targetIdps.setItems(idPs.keySet());
		targetIdps.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);
		targetIdps.setCaption(msg.getMessage("AttributePolicyConfigurationEditor.targetIdps"));
		binder.forField(targetIdps).bind("targetIdps");
		header.addComponent(targetIdps);

		ChipsWithDropdown<String> targetFederations = new ChipsWithDropdown<>(p -> getGroupDisplayeName(p), true);
		targetFederations.setItems(IdPsGroups.keySet());
		targetFederations.setWidth(FieldSizeConstans.WIDE_FIELD_WIDTH, FieldSizeConstans.WIDE_FIELD_WIDTH_UNIT);

		targetFederations.setCaption(msg.getMessage("AttributePolicyConfigurationEditor.targetFederations"));
		binder.forField(targetFederations).bind("targetFederations");
		header.addComponent(targetFederations);

		AttributesGrid attributes = new AttributesGrid(msg);
		binder.forField(attributes).bind("attributes");
		main.addComponent(attributes);

		binder.addValueChangeListener(e -> fireEvent(new ValueChangeEvent<>(this, getValue(), true)));

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
		return value.displayedName.get().getValue(msg);
	}

	@Override
	protected String getHeaderText()
	{
		return name.getValue() == null || name.getValue().isEmpty() ? "" : name.getValue();

	}

	@Override
	protected void validate() throws FormValidationException
	{
		if (binder.validate().hasErrors())
		{
			throw new FormValidationException(
					msg.getMessage("AttributePolicyConfigurationEditor.invalidConfiguration"));
		}
	}

	@Override
	public AttributePolicy getValue()
	{
		if (binder.validate().hasErrors())
			return null;
		AttributePolicyBean bean = binder.getBean();
		if (bean == null)
			return null;
		return new AttributePolicy(bean.getName(), bean.getAttributes(), bean.getTargetIdps(),
				bean.getTargetFederations());
	}

	@Override
	protected Component initContent()
	{
		return main;
	}

	@Override
	protected void doSetValue(AttributePolicy value)
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
