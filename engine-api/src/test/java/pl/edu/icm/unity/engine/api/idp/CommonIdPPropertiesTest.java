/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.idp;

import org.apache.logging.log4j.Logger;
import org.junit.Test;
import pl.edu.icm.unity.base.utils.Log;

public class CommonIdPPropertiesTest
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, CommonIdPPropertiesTest.class);
	
	@Test
	public void shouldReturnFilteredAndRemainingAttributes()
	{
//		Properties properties = new Properties();
//		properties.put(ACTIVE_VALUE_SELECTION_PFX + "1.client", "client");
//		properties.put(ACTIVE_VALUE_SELECTION_PFX + "1.singleValueAttributes.1", "a1");
//		properties.put(ACTIVE_VALUE_SELECTION_PFX + "1.multiValueAttributes.1", "a2");
//		PropertiesHelper cfg = new PropertiesHelper("", properties,
//				CommonIdPProperties.getDefaults("", "defProfile"), log);
//
//		Collection<DynamicAttribute> allAttributes = Lists.newArrayList(
//				new DynamicAttribute(new Attribute("a1", "syntax", "/", Lists.newArrayList("v1"))),
//				new DynamicAttribute(new Attribute("a2", "syntax", "/", Lists.newArrayList("v1"))),
//				new DynamicAttribute(new Attribute("a3", "syntax", "/", Lists.newArrayList("v1")))
//				);
//
//
//		Optional<ActiveValueSelectionConfig> selectionConfig = CommonIdPProperties.getActiveValueSelectionConfig(
//				cfg, "client", allAttributes);
//
//		assertThat(selectionConfig.isPresent(), is(true));
//		assertThat(selectionConfig.get().multiSelectableAttributes.size(), is(1));
//		assertThat(selectionConfig.get().multiSelectableAttributes.get(0).getAttribute().getName(), is("a2"));
//
//		assertThat(selectionConfig.get().singleSelectableAttributes.size(), is(1));
//		assertThat(selectionConfig.get().singleSelectableAttributes.get(0).getAttribute().getName(), is("a1"));
//
//		assertThat(selectionConfig.get().remainingAttributes.size(), is(1));
//		assertThat(selectionConfig.get().remainingAttributes.get(0).getAttribute().getName(), is("a3"));
	}
}
