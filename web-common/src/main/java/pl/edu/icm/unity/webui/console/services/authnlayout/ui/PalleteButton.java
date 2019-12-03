/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services.authnlayout.ui;

import java.util.function.Supplier;

import com.vaadin.server.Resource;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.ui.Button;
import com.vaadin.ui.dnd.DragSourceExtension;

/**
 * 
 * @author P.Piernik
 *
 */
public class PalleteButton extends Button implements PalleteElement
{

	public PalleteButton(String title, Resource icon, Runnable dragStart, Runnable dragStop,
			Supplier<ColumnElement> layoutElementSupplier)
	{
		setCaption(title);
		setIcon(icon);

		DragSourceExtension<Button> dragSource = new DragSourceExtension<>(this);
		dragSource.setEffectAllowed(EffectAllowed.MOVE);
		dragSource.setDragData(layoutElementSupplier);
		setStyleName("u-inactiveButton");
		
		dragSource.addDragStartListener(e -> dragStart.run());
		dragSource.addDragEndListener(e -> dragStop.run());
	}
}