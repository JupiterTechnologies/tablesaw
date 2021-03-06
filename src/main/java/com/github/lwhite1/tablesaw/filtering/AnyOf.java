package com.github.lwhite1.tablesaw.filtering;

import com.github.lwhite1.tablesaw.api.Table;
import org.roaringbitmap.RoaringBitmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A composite filtering that only returns {@code true} if all component filters return true
 */
public class AnyOf extends CompositeFilter {

  private List<Filter> filterList = new ArrayList<>();

  AnyOf(Collection<Filter> filters) {

    this.filterList.addAll(filters);
  }

  public static AnyOf anyOf(Filter... filters) {
    List<Filter> filterList = new ArrayList<>();
    Collections.addAll(filterList, filters);
    return new AnyOf(filterList);
  }

  public static AnyOf anyOf(Collection<Filter> filters) {
    return new AnyOf(filters);
  }

  public RoaringBitmap apply(Table relation) {
    RoaringBitmap roaringBitmap = null;
    for (Filter filter : filterList) {
      if (roaringBitmap == null) {
        roaringBitmap = filter.apply(relation);
      } else {
        roaringBitmap.or(filter.apply(relation));
      }
    }
    return roaringBitmap;
  }
}
