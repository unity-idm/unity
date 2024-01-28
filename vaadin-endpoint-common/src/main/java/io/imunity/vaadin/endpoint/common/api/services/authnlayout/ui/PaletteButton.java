/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services.authnlayout.ui;

import java.util.function.Supplier;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Button which can be drag and drop on column component
 * 
 * @author P.Piernik
 *
 */
public class PaletteButton extends Button
{

	public PaletteButton(String title, VaadinIcon icon, Runnable dragStart, Runnable dragStop,
			Supplier<ColumnComponent> layoutElementSupplier)
	{
		setText(title);
		setIcon(new Icon(icon));

//		DragSourceExtension<Button> dragSource = new DragSourceExtension<>(this);
//		dragSource.setEffectAllowed(EffectAllowed.MOVE);
//		dragSource.setDragData(layoutElementSupplier);
//		setStyleName("u-inactiveButton");
		
		DragSource<Button> dragSource = DragSource.create(this);
		dragSource.setEffectAllowed(EffectAllowed.MOVE);
		dragSource.setDragData(layoutElementSupplier);

		dragSource.addDragStartListener(e -> dragStart.run());
		dragSource.addDragEndListener(e -> dragStop.run());
	}
}