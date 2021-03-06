package com.github.lwhite1.tablesaw.api;

import com.github.lwhite1.tablesaw.columns.AbstractColumn;
import com.github.lwhite1.tablesaw.columns.Column;
import com.github.lwhite1.tablesaw.filtering.FloatBiPredicate;
import com.github.lwhite1.tablesaw.filtering.FloatPredicate;
import com.github.lwhite1.tablesaw.io.TypeUtils;
import com.github.lwhite1.tablesaw.store.ColumnMetadata;
import com.github.lwhite1.tablesaw.util.StatUtil;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatComparator;
import it.unimi.dsi.fastutil.floats.FloatIterable;
import it.unimi.dsi.fastutil.floats.FloatIterator;
import it.unimi.dsi.fastutil.floats.FloatOpenHashSet;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.ints.IntComparator;
import org.roaringbitmap.RoaringBitmap;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.lwhite1.tablesaw.columns.FloatColumnUtils.*;

/**
 * A column in a base table that contains float values
 */
public class FloatColumn extends AbstractColumn implements FloatIterable {

  public static final float MISSING_VALUE = (float) ColumnType.FLOAT.getMissingValue();
  private static final int BYTE_SIZE = 4;

  private static int DEFAULT_ARRAY_SIZE = 128;

  private FloatArrayList data;

  public FloatColumn(String name) {
    super(name);
    data = new FloatArrayList(DEFAULT_ARRAY_SIZE);
  }

  public FloatColumn(String name, int initialSize) {
    super(name);
    data = new FloatArrayList(initialSize);
  }

  public FloatColumn(ColumnMetadata metadata) {
    super(metadata);
    data = new FloatArrayList(metadata.getSize());
  }

  public int size() {
    return data.size();
  }

  @Override
  public Table summary() {
    return StatUtil.stats(this).asTable("Column: " + name());
  }

  public String describe() {
    return StatUtil.stats(this).printString();
  }

  @Override
  public int countUnique() {
    FloatSet floats = new FloatOpenHashSet();
    for (int i = 0; i < size(); i++) {
      floats.add(data.getFloat(i));
    }
    return floats.size();
  }

  /**
   * Returns the largest ("top") n values in the column
   *
   * @param n The maximum number of records to return. The actual number will be smaller if n is greater than the
   *          number of observations in the column
   * @return A list, possibly empty, of the largest observations
   */
  public FloatArrayList top(int n) {
    FloatArrayList top = new FloatArrayList();
    float[] values = data.toFloatArray();
    FloatArrays.parallelQuickSort(values, reverseFloatComparator);
    for (int i = 0; i < n && i < values.length; i++) {
      top.add(values[i]);
    }
    return top;
  }

  /**
   * Returns the smallest ("bottom") n values in the column
   *
   * @param n The maximum number of records to return. The actual number will be smaller if n is greater than the
   *          number of observations in the column
   * @return A list, possibly empty, of the smallest n observations
   */
  public FloatArrayList bottom(int n) {
    FloatArrayList bottom = new FloatArrayList();
    float[] values = data.toFloatArray();
    FloatArrays.parallelQuickSort(values);
    for (int i = 0; i < n && i < values.length; i++) {
      bottom.add(values[i]);
    }
    return bottom;
  }

  @Override
  public FloatColumn unique() {
    FloatSet floats = new FloatOpenHashSet();
    for (int i = 0; i < size(); i++) {
      floats.add(data.getFloat(i));
    }
    FloatColumn column = new FloatColumn(name() + " Unique values", floats.size());
    floats.forEach(column::add);
    return column;
  }

  @Override
  public ColumnType type() {
    return ColumnType.FLOAT;
  }

  public double sum() {
    return StatUtil.sum(this);
  }

  public float mean() {
    return StatUtil.mean(this);
  }

  public float firstElement() {
    if (size() > 0) {
      return data.getFloat(0);
    }
    return MISSING_VALUE;
  }

  public float max() {
    return StatUtil.max(this);
  }

  public float min() {
    return StatUtil.min(this);
  }

  public void add(float f) {
    data.add(f);
  }

  public RoaringBitmap isLessThan(float f) {
    return apply(isLessThan, f);
  }

  public RoaringBitmap isMissing() {
    return apply(isMissing);
  }

  public RoaringBitmap isNotMissing() {
    return apply(isNotMissing);
  }

  public RoaringBitmap isGreaterThan(float f) {
    return apply(isGreaterThan, f);
  }

  public RoaringBitmap isGreaterThanOrEqualTo(float f) {
    return apply(isGreaterThanOrEqualTo, f);
  }

  public RoaringBitmap isLessThanOrEqualTo(float f) {
    return apply(isLessThanOrEqualTo, f);
  }

  public RoaringBitmap isEqualTo(float f) {
    return apply(isEqualTo, f);
  }

  public RoaringBitmap isEqualTo(FloatColumn f) {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    FloatIterator floatIterator = f.iterator();
    for (float floats : data) {
      if (floats == floatIterator.nextFloat()) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  @Override
  public String getString(int row) {
    return String.valueOf(data.getFloat(row));
  }

  @Override
  public FloatColumn emptyCopy() {
    return new FloatColumn(name());
  }

  @Override
  public FloatColumn emptyCopy(int rowSize) {
    return new FloatColumn(name(), rowSize);
  }

  @Override
  public void clear() {
    data = new FloatArrayList(DEFAULT_ARRAY_SIZE);
  }

  private FloatColumn copy() {
    return FloatColumn.create(name(), data);
  }

  @Override
  public void sortAscending() {
    Arrays.parallelSort(data.elements());
  }

  @Override
  public void sortDescending() {
    FloatArrays.parallelQuickSort(data.elements(), reverseFloatComparator);
  }

  @Override
  public boolean isEmpty() {
    return data.isEmpty();
  }

  public static FloatColumn create(String name) {
    return new FloatColumn(name);
  }

  public static FloatColumn create(String name, int initialSize) {
    return new FloatColumn(name, initialSize);
  }

  public static FloatColumn create(String fileName, FloatArrayList floats) {
    FloatColumn column = new FloatColumn(fileName, floats.size());
    column.data = floats;
    return column;
  }

  /**
   * Compares two floats, such that a sort based on this comparator would sort in descending order
   */
  FloatComparator reverseFloatComparator = new FloatComparator() {

    @Override
    public int compare(Float o2, Float o1) {
      return (o1 < o2 ? -1 : (o1.equals(o2) ? 0 : 1));
    }

    @Override
    public int compare(float o2, float o1) {
      return (o1 < o2 ? -1 : (o1 == o2 ? 0 : 1));
    }
  };

  @Override
  public void addCell(String object) {
    try {
      add(convert(object));
    } catch (NumberFormatException nfe) {
      throw new NumberFormatException(name() + ": " + nfe.getMessage());
    } catch (NullPointerException e) {
      throw new RuntimeException(name() + ": "
          + String.valueOf(object) + ": "
          + e.getMessage());
    }
  }

  /**
   * Returns a float that is parsed from the given String
   * <p>
   * We remove any commas before parsing
   */
  public static float convert(String stringValue) {
    if (Strings.isNullOrEmpty(stringValue) || TypeUtils.MISSING_INDICATORS.contains(stringValue)) {
      return MISSING_VALUE;
    }
    Matcher matcher = COMMA_PATTERN.matcher(stringValue);
    return Float.parseFloat(matcher.replaceAll(""));
  }

  /**
   * Returns the natural log of the values in this column as a new FloatColumn
   */
  public FloatColumn logN() {
    FloatColumn newColumn = FloatColumn.create(name() + "[logN]", size());

    for (float value : this) {
      newColumn.add((float) Math.log(value));
    }
    return newColumn;
  }

  public FloatColumn log10() {
    FloatColumn newColumn = FloatColumn.create(name() + "[log10]", size());

    for (float value : this) {
      newColumn.add((float) Math.log10(value));
    }
    return newColumn;
  }

  /**
   * Returns the natural log of the values in this column, after adding 1 to each so that zero
   * values don't return -Infinity
   */
  public FloatColumn log1p() {
    FloatColumn newColumn = FloatColumn.create(name() + "[1og1p]", size());
    for (float value : this) {
      newColumn.add((float) Math.log1p(value));
    }
    return newColumn;
  }

  public FloatColumn round() {
    FloatColumn newColumn = FloatColumn.create(name() + "[rounded]", size());
    for (float value : this) {
      newColumn.add(Math.round(value));
    }
    return newColumn;
  }

  public FloatColumn abs() {
    FloatColumn newColumn = FloatColumn.create(name() + "[abs]", size());
    for (float value : this) {
      newColumn.add(Math.abs(value));
    }
    return newColumn;
  }

  public FloatColumn square() {
    FloatColumn newColumn = FloatColumn.create(name() + "[sq]", size());
    for (float value : this) {
      newColumn.add(value * value);
    }
    return newColumn;
  }

  public FloatColumn sqrt() {
    FloatColumn newColumn = FloatColumn.create(name() + "[sqrt]", size());
    for (float value : this) {
      newColumn.add((float) Math.sqrt(value));
    }
    return newColumn;
  }

  public FloatColumn cubeRoot() {
    FloatColumn newColumn = FloatColumn.create(name() + "[cbrt]", size());
    for (float value : this) {
      newColumn.add((float) Math.cbrt(value));
    }
    return newColumn;
  }

  public FloatColumn cube() {
    FloatColumn newColumn = FloatColumn.create(name() + "[cb]", size());
    for (float value : this) {
      newColumn.add(value * value * value);
    }
    return newColumn;
  }

  public FloatColumn mod(FloatColumn column2) {
    FloatColumn result = FloatColumn.create(name() + " % " + column2.name(), size());
    for (int r = 0; r < size(); r++) {
      result.add(get(r) % column2.get(r));
    }
    return result;
  }

  public FloatColumn difference(FloatColumn column2) {
    FloatColumn result = FloatColumn.create(name() + " - " + column2.name(), size());
    for (int r = 0; r < size(); r++) {
      result.add(get(r) - column2.get(r));
    }
    return result;
  }

  public float[] mode() {
    return StatUtil.mode(data.elements());
  }

  /**
   * For each item in the column, returns the same number with the sign changed.
   * For example:
   * -1.3   returns  1.3,
   * 2.135 returns -2.135
   * 0     returns  0
   */
  public FloatColumn neg() {
    FloatColumn newColumn = FloatColumn.create(name() + "[neg]", size());
    for (float value : this) {
      newColumn.add(value * -1);
    }
    return newColumn;
  }

  private static final Pattern COMMA_PATTERN = Pattern.compile(",");

  /**
   * Compares the given ints, which refer to the indexes of the floats in this column, according to the values of the
   * floats themselves
   */
  @Override
  public IntComparator rowComparator() {
    return comparator;
  }

  private final IntComparator comparator = new IntComparator() {

    @Override
    public int compare(Integer r1, Integer r2) {
      float f1 = data.getFloat(r1);
      float f2 = data.getFloat(r2);
      return Float.compare(f1, f2);
    }

    public int compare(int r1, int r2) {
      float f1 = data.getFloat(r1);
      float f2 = data.getFloat(r2);
      return Float.compare(f1, f2);
    }
  };

  public float get(int index) {
    return data.getFloat(index);
  }

  public void set(int r, float value) {
    data.set(r, value);
  }

  // TODO(lwhite): Reconsider the implementation of this functionality to allow user to provide a specific max error.
  // TODO(lwhite): continued: Also see section in Effective Java on floating point comparisons.
  RoaringBitmap isCloseTo(float target) {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (float f : data) {
      if (Float.compare(f, target) == 0) {
        results.add(i);
      }
      i++;
    }

    return results;
  }

  RoaringBitmap isCloseTo(double target) {
    RoaringBitmap results = new RoaringBitmap();
    int i = 0;
    for (float f : data) {
      if (Double.compare(f, 0.0) == 0) {
        results.add(i);
      }
      i++;
    }
    return results;
  }

  RoaringBitmap isPositive() {
    return apply(isPositive);
  }

  RoaringBitmap isNegative() {
    return apply(isNegative);
  }

  RoaringBitmap isNonNegative() {
    return apply(isNonNegative);
  }

  public double[] toDoubleArray() {
    double[] output = new double[data.size()];
    for (int i = 0; i < data.size(); i++) {
      output[i] = data.getFloat(i);
    }
    return output;
  }

  public String print() {
    StringBuilder builder = new StringBuilder();
    builder.append(title());
    for (Float aData : data) {
      builder.append(String.valueOf(aData));
      builder.append('\n');
    }
    return builder.toString();
  }

  @Override
  public String toString() {
    return "Float column: " + name();
  }

  @Override
  public void append(Column column) {
    Preconditions.checkArgument(column.type() == this.type());
    FloatColumn floatColumn = (FloatColumn) column;
    for (int i = 0; i < floatColumn.size(); i++) {
      add(floatColumn.get(i));
    }
  }

  @Override
  public FloatIterator iterator() {
    return data.iterator();
  }

  public RoaringBitmap apply(FloatPredicate predicate) {
    RoaringBitmap bitmap = new RoaringBitmap();
    for (int idx = 0; idx < data.size(); idx++) {
      float next = data.getFloat(idx);
      if (predicate.test(next)) {
        bitmap.add(idx);
      }
    }
    return bitmap;
  }

  public RoaringBitmap apply(FloatBiPredicate predicate, float value) {
    RoaringBitmap bitmap = new RoaringBitmap();
    for (int idx = 0; idx < data.size(); idx++) {
      float next = data.getFloat(idx);
      if (predicate.test(next, value)) {
        bitmap.add(idx);
      }
    }
    return bitmap;
  }

  FloatSet asSet() {
    return new FloatOpenHashSet(data);
  }

  public boolean contains(float value) {
    return data.contains(value);
  }

  @Override
  public int byteSize() {
    return BYTE_SIZE;
  }

  /**
   * Returns the contents of the cell at rowNumber as a byte[]
   */
  @Override
  public byte[] asBytes(int rowNumber) {
    return ByteBuffer.allocate(4).putFloat(get(rowNumber)).array();
  }
}
