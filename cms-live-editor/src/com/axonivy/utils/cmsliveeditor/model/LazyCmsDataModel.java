package com.axonivy.utils.cmsliveeditor.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;

import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.filter.FilterConstraint;
import org.primefaces.util.LocaleUtils;

public class LazyCmsDataModel extends LazyDataModel<Cms> {

  private static final long serialVersionUID = 1L;

  private final List<Cms> datasource;

  public LazyCmsDataModel(List<Cms> datasource) {
    this.datasource = datasource;
  }

  @Override
  public Cms getRowData(String uri) {
    var validUri = StringUtils.defaultString(uri);
    return datasource.stream().filter(c -> validUri.equals(c.getUri())).findFirst().orElse(null);
  }

  @Override
  public String getRowKey(Cms cms) {
    return String.valueOf(cms.getUri());
  }

  @Override
  public int count(Map<String, FilterMeta> filterBy) {
    return (int) datasource.stream().filter(o -> filter(FacesContext.getCurrentInstance(), filterBy.values(), o))
        .count();
  }

  @Override
  public List<Cms> load(int offset, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
    var faceContext = FacesContext.getCurrentInstance();
    // apply offset & filters
    List<Cms> cmsEntries = datasource.stream()
        .filter(o -> filter(faceContext, filterBy.values(), o)).collect(Collectors.toList());

    // sort
    if (!sortBy.isEmpty()) {
      List<Comparator<Cms>> comparators =
          sortBy.values().stream().map(o -> new LazyCmsSorter(o.getOrder())).collect(Collectors.toList());
      Comparator<Cms> cp = ComparatorUtils.chainedComparator(comparators);
      cmsEntries.sort(cp);
    }

    if (offset >= cmsEntries.size()) {
      return List.of();
    }

    return cmsEntries.subList(offset, Math.min(offset + pageSize, cmsEntries.size()));
  }

  private boolean filter(FacesContext context, Collection<FilterMeta> filterBy, Object o) {
    boolean matching = true;

    for (FilterMeta filter : filterBy) {
      FilterConstraint constraint = filter.getConstraint();
      Object filterValue = filter.getFilterValue();

      try {
        Object columnValue = String.valueOf(o.toString());
        matching = constraint.isMatching(context, columnValue, filterValue, LocaleUtils.getCurrentLocale());
      } catch (Exception e) {
        matching = false;
      }

      if (!matching) {
        break;
      }
    }

    return matching;
  }

}
