package com.gpl.rpg.AndorsTrail.model.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class DropListTest {

	@Test
	public void getMarketCostMarkupPercentReturnsConfiguredEntryValue() {
		DropList.DropItem item = new DropList.DropItem("priced_item", null, null, 10);
		DropList dropList = new DropList(
				new DropList.DropItem[] { item },
				new ItemTypeCollection(),
				new ItemFilterCollection()
		);

		assertEquals(Integer.valueOf(10), dropList.getMarketCostMarkupPercent("priced_item"));
		assertNull(dropList.getMarketCostMarkupPercent("other_item"));
	}
}
