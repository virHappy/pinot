package com.linkedin.pinot.core.block.intarray;

import com.linkedin.pinot.core.block.sets.utils.SortedBlockDocIdSet;
import com.linkedin.pinot.core.block.sets.utils.UnSortedBlockDocIdSet;
import com.linkedin.pinot.core.common.BlockDocIdIterator;
import com.linkedin.pinot.core.common.BlockDocIdSet;
import com.linkedin.pinot.core.common.Predicate;
import com.linkedin.pinot.core.indexsegment.columnar.BitmapInvertedIndex;
import com.linkedin.pinot.core.indexsegment.dictionary.Dictionary;
import com.linkedin.pinot.core.indexsegment.utils.IntArray;
import com.linkedin.pinot.core.indexsegment.utils.SortedIntArray;


/**
 * Jul 15, 2014
 * @author Dhaval Patel <dpatel@linkedin.com>
 *
 */
public class CompressedIntBlockDocIdSet implements BlockDocIdSet {

  final IntArray intArray;
  final Dictionary<?> dictionary;
  final int start, end;
  Predicate p;
  final BitmapInvertedIndex invertedIdx;

  public CompressedIntBlockDocIdSet(IntArray intArray, Dictionary<?> dictionary, int start, int end, Predicate p,
      BitmapInvertedIndex invertedIndex) {
    this.intArray = intArray;
    this.dictionary = dictionary;
    this.start = start;
    this.end = end;
    this.p = p;
    invertedIdx = invertedIndex;
  }

  private BlockDocIdIterator unsortedIterator() {
    if (p == null) {
      return UnSortedBlockDocIdSet.OnForwardIndex.getDefaultIterator(intArray, start, end);
    }

    if (invertedIdx != null) {
      return blockDocIdSetOnInvertedIndex();
    }

    return blockDocIdSetOnForwardIndex();
  }

  /**
   * this method returns blockDocIdSetIterator on invertedIndex
   * @return
   */

  private BlockDocIdIterator blockDocIdSetOnInvertedIndex() {
    switch (p.getType()) {
      case EQ:
        final int equalsLookup = dictionary.indexOf(p.getRhs().get(0));
        return UnSortedBlockDocIdSet.OnInvertedIndex.getEqualityMatchIterator(invertedIdx, start, end, equalsLookup);
      case NEQ:
        final int notEqualsLookup = dictionary.indexOf(p.getRhs().get(0));
        return UnSortedBlockDocIdSet.OnForwardIndex.getNotEqualsMatchIterator(intArray, start, end, notEqualsLookup);
      default:
        throw new UnsupportedOperationException("current I don't support predicate type : " + p.getType());
    }
  }

  /**
   * this method returns blockDocIdSetIterator on forwardIndex
   * @return
   */

  private BlockDocIdIterator blockDocIdSetOnForwardIndex() {
    switch (p.getType()) {
      case EQ:
        final int equalsLookup = dictionary.indexOf(p.getRhs().get(0));
        return UnSortedBlockDocIdSet.OnForwardIndex.getEqualityMatchIterator(intArray, start, end, equalsLookup);
      case NEQ:
        final int notEqualsLookup = dictionary.indexOf(p.getRhs().get(0));
        return UnSortedBlockDocIdSet.OnForwardIndex.getNotEqualsMatchIterator(intArray, start, end, notEqualsLookup);
      default:
        throw new UnsupportedOperationException("current I don't support predicate type : " + p.getType());
    }
  }

  private BlockDocIdIterator sortedIterator() {
    if (p == null) {
      return SortedBlockDocIdSet.getDefaultIterator((SortedIntArray) intArray, start, end);
    }

    switch (p.getType()) {
      case EQ:
        final int equalsLookup = dictionary.indexOf(p.getRhs().get(0));
        return SortedBlockDocIdSet.getEqualityMatchIterator((SortedIntArray) intArray, start, end, equalsLookup);
      case NEQ:
        final int notEqualsLookup = dictionary.indexOf(p.getRhs().get(0));
        return SortedBlockDocIdSet.getNotEqualsMatchIterator((SortedIntArray) intArray, start, end, notEqualsLookup);
      default:
        throw new UnsupportedOperationException("current I don't support predicate type : " + p.getType());
    }
  }

  @Override
  public BlockDocIdIterator iterator() {
    //    if (intArray instanceof SortedIntArray) {
    //      return sortedIterator();
    //    }
    return unsortedIterator();
  }
}
