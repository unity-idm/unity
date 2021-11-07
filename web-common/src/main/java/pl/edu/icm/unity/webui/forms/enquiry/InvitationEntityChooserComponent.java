/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.forms.enquiry;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;

@PrototypeComponent
class InvitationEntityChooserComponent extends CustomComponent
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, InvitationEntityChooserComponent.class);

	private final MessageSource msg;
	private final AttributeSupport attributeSupport;

	private RadioButtonGroup<Entity> entityChooser;

	@Autowired
	public InvitationEntityChooserComponent(MessageSource msg, AttributeSupport attributeSupport)
	{
		this.msg = msg;
		this.attributeSupport = attributeSupport;
	}

	public InvitationEntityChooserComponent init(ResolvedInvitationParam invitation, Consumer<Long> callback)
	{
		VerticalLayout main = new VerticalLayout();
		entityChooser = new RadioButtonGroup<>();
		entityChooser.setItems(invitation.entities);
		entityChooser.setItemCaptionGenerator(e -> getEntityRepresentation(e));
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

	private String getEntityRepresentation(Entity entity)
	{

		StringBuilder entityRep = new StringBuilder();
		Optional<String> displayedName = getDisplayName(entity);

		if (displayedName.isPresent())
		{
			entityRep.append(displayedName.get());
		}

		List<Identity> remoteIds = getRemoteIdentities(entity);
		if (!remoteIds.isEmpty())
		{
			if (displayedName.isPresent())
				entityRep.append(": ");
			entityRep.append(
					remoteIds.stream().map(i -> getRemoteIdentityRepresentation(i)).collect(Collectors.joining(" & ")));

		} else
		{
			String localIds = getLocalIdentitiesWithoutIdentifier(entity).stream()
					.map(i -> getLocalIdentityRepresentation(i)).collect(Collectors.joining(" & "));
			if (!localIds.isEmpty() && displayedName.isPresent())
				entityRep.append(": ");
			entityRep.append(localIds);
		}

		return entityRep.toString();
	}

	private String getLocalIdentityRepresentation(Identity i)
	{
		return i.getValue();
	}

	private String getRemoteIdentityRepresentation(Identity i)
	{

		try
		{
			URI uri = new URI(i.getRemoteIdp());
			return uri.getHost();
		} catch (URISyntaxException e)
		{
			return i.getRemoteIdp();
		}
	}

	private List<Identity> getRemoteIdentities(Entity entity)
	{
		return entity.getIdentities().stream().filter(i -> !i.isLocal()).collect(Collectors.toList());
	}

	private List<Identity> getLocalIdentitiesWithoutIdentifier(Entity entity)
	{
		return entity.getIdentities().stream().filter(i -> i.isLocal() && !i.getTypeId().equals(IdentifierIdentity.ID))
				.collect(Collectors.toList());
	}

	Optional<String> getDisplayName(Entity enitity)
	{
		try
		{
			return attributeSupport.getAttributeValueByMetadata(new EntityParam(enitity.getId()), "/",
					EntityNameMetadataProvider.NAME);
		} catch (EngineException e)
		{
			log.error("Failed to get entity {} display name", enitity.getEntityInformation().getId(), e);
		}
		return Optional.empty();
	}

	@Component
	public static class InvitationEntityChooserComponentFactory
	{

		private final ObjectFactory<InvitationEntityChooserComponent> factory;

		public InvitationEntityChooserComponentFactory(ObjectFactory<InvitationEntityChooserComponent> factory)
		{
			this.factory = factory;
		}

		InvitationEntityChooserComponent get(ResolvedInvitationParam invitation, Consumer<Long> callback)
		{
			return factory.getObject().init(invitation, callback);
		}
	}

}