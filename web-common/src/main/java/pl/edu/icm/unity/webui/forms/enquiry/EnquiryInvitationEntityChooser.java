/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.enquiry;

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;

@PrototypeComponent
class EnquiryInvitationEntityChooser extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, EnquiryInvitationEntityChooser.class);

	private final MessageSource msg;
	private final AttributeSupport attributeSupport;

	private RadioButtonGroup<Entity> entityChooser;
	private final EnquiryInvitationEntityRepresentationProvider entityRepresentationProvider;

	@Autowired
	public EnquiryInvitationEntityChooser(MessageSource msg, AttributeSupport attributeSupport)
	{
		this.msg = msg;
		this.attributeSupport = attributeSupport;
		this.entityRepresentationProvider = new EnquiryInvitationEntityRepresentationProvider(this::getDisplayName);
	}

	public EnquiryInvitationEntityChooser init(ResolvedInvitationParam invitation, Consumer<Long> callback)
	{
		VerticalLayout main = new VerticalLayout();
		entityChooser = new RadioButtonGroup<>();
		entityChooser.setItems(invitation.entities);
		entityChooser.setItemCaptionGenerator(e -> entityRepresentationProvider.getEntityRepresentation(e));
		entityChooser.setSelectedItem(invitation.entities.get(0));
		Button confirm = new Button("confirm");
		confirm.setStyleName(Styles.buttonAction.toString());
		confirm.addClickListener(e -> callback.accept(entityChooser.getSelectedItem().get().getId()));
		Label info = new Label(msg.getMessage("StandalonePublicEnquiryView.chooseEntity", invitation.contactAddress));
		info.setCaptionAsHtml(true);
		info.addStyleName(Styles.textLarge.toString());
		info.addStyleName(Styles.wordWrap.toString());

		main.addComponent(info);
		main.addComponent(entityChooser);
		main.addComponent(confirm);
		main.setComponentAlignment(info, Alignment.MIDDLE_CENTER);
		main.setComponentAlignment(entityChooser, Alignment.MIDDLE_CENTER);
		main.setComponentAlignment(confirm, Alignment.MIDDLE_CENTER);
		setCompositionRoot(main);
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
	public static class InvitationEntityChooserComponentFactory
	{

		private final ObjectFactory<EnquiryInvitationEntityChooser> factory;

		public InvitationEntityChooserComponentFactory(ObjectFactory<EnquiryInvitationEntityChooser> factory)
		{
			this.factory = factory;
		}

		EnquiryInvitationEntityChooser get(ResolvedInvitationParam invitation, Consumer<Long> callback)
		{
			return factory.getObject().init(invitation, callback);
		}
	}

}