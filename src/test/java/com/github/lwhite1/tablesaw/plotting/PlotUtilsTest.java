package com.github.lwhite1.tablesaw.plotting;

import com.github.lwhite1.tablesaw.api.IntColumn;
import com.github.lwhite1.tablesaw.api.ShortColumn;
import com.github.lwhite1.tablesaw.api.Table;
import com.github.lwhite1.tablesaw.io.csv.CsvReader;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *  Interactive tests for plotting
 */
public class PlotUtilsTest {


  private Table table;

  @Before
  public void setUp() throws Exception {
    table = CsvReader.read("data/BushApproval.csv");
  }

  @Ignore
  @Test
  public void testPlot() {

    PlotUtils.xyPlot(table.dateColumn("date"), table.shortColumn("approval"));
  }

}