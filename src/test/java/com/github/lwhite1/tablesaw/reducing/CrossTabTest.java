package com.github.lwhite1.tablesaw.reducing;

import com.github.lwhite1.tablesaw.api.Table;
import com.github.lwhite1.tablesaw.io.csv.CsvReader;
import org.junit.Test;

/**
 * Tests for Cross Tabs
 */
public class CrossTabTest {

  @Test
  public void testXCount() throws Exception {

    Table t = CsvReader.read("data/tornadoes_1950-2014.csv");

    Table xtab = CrossTab.xCount(t, t.categoryColumn("State"), t.shortColumn("Scale"));
    //System.out.println(xtab.print());

    Table rPct = CrossTab.rowPercents(xtab);
    //System.out.println(rPct.print());

    Table tPct = CrossTab.tablePercents(xtab);
   // System.out.println(tPct.print());

    //TODO(lwhite): Real tests go here
  }
}