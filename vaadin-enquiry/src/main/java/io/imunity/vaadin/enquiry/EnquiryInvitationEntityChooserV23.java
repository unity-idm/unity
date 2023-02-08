/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.enquiry;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;

import java.util.Optional;
import java.util.function.Consumer;

@PrototypeComponent
class EnquiryInvitationEntityChooserV23 extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryInvitationEntityChooserV23.class);

	private final MessageSource msg;
	private final AttributeSupport attributeSupport;

	private RadioButtonGroup<Entity> entityChooser;
	private final EnquiryInvitationEntityRepresentationProvider entityRepresentationProvider;

	@Autowired
	public EnquiryInvitationEntityChooserV23(MessageSource msg, AttributeSupport attributeSupport)
	{
		this.msg = msg;
		this.attributeSupport = attributeSupport;
		this.entityRepresentationProvider = new EnquiryInvitationEntityRepresentationProvider(this::getDisplayName, msg);
	}

	public EnquiryInvitationEntityChooserV23 init(ResolvedInvitationParam invitation, Consumer<Long> callback, Runnable cancel)
	{
		VerticalLayout main = new VerticalLayout();
		entityChooser = new RadioButtonGroup<>();
		entityChooser.setItems(invitation.entities);
		entityChooser.setItemLabelGenerator(entityRepresentationProvider::getEntityRepresentation);
		entityChooser.setValue(invitation.entities.iterator().next());
		Button confirm = new Button(msg.getMessage("EnquiryInvitationEntityChooser.proceed"));
		confirm.addClickListener(e -> callback.accept(entityChooser.getValue().getId()));
		confirm.addClassName("u-button-form");
		
		Button cancelB = new Button(msg.getMessage("cancel"));
		cancelB.addClickListener(e -> cancel.run());
		cancelB.addClassName("u-button-form");
		
		HorizontalLayout buttons = new HorizontalLayout();
		buttons.add(cancelB);
		buttons.add(confirm);

		Label infoTitle = new Label(msg.getMessage("EnquiryInvitationEntityChooser.chooseEntityTitle", invitation.contactAddress));
		
		Label infoDesc = new Label(msg.getMessage("EnquiryInvitationEntityChooser.chooseEntityDescription"));;

		main.add(infoTitle);
		main.add(infoDesc);
		main.add(entityChooser);
		main.add(buttons);
		main.setAlignItems(Alignment.CENTER);
		add(main);
		setWidth(40, Unit.EM);
		return this;
	}

	private Optional<String> getDisplayName(Long entity)
	{
		try
		{
			return attributeSupport.getAttributeValueByMetadata(new EntityParam(entity), "/",
					EntityNameMetadataProvider.NAME);
		} catch (EngineException e)
		{
			log.error("Failed to get entity {} display name", entity, e);
		}
		return Optional.empty();
	}

	@Component
	public static class InvitationEntityChooserComponentFactoryV23
	{

		private final ObjectFactory<EnquiryInvitationEntityChooserV23> factory;

		public InvitationEntityChooserComponentFactoryV23(ObjectFactory<EnquiryInvitationEntityChooserV23> factory)
		{
			this.factory = factory;
		}

		EnquiryInvitationEntityChooserV23 get(ResolvedInvitationParam invitation, Consumer<Long> callback, Runnable cancel)
		{
			return factory.getObject().init(invitation, callback, cancel);
		}
	}

}