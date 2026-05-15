package com.axonivy.utils.cmsliveeditor.model;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;

import org.apache.commons.collections4.ComparatorUtils;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.filter.FilterConstraint;
import org.primefaces.util.LocaleUtils;

public class LazyCmsDataModel  extends LazyDataModel<Cms> {

  private static final long serialVersionUID = 1L;

  private List<Cms> datasource;

  public LazyCmsDataModel(List<Cms> datasource) {
      this.datasource = datasource;
  }

  @Override
  public Cms getRowData(String uri) {
    return datasource.stream()
        .filter(c -> c.getUri().toString().equals(uri))
        .findFirst()
        .orElse(null);
  }

  @Override
  public String getRowKey(Cms cms) {
      return String.valueOf(cms.getUri());
  }

  @Override
  public int count(Map<String, FilterMeta> filterBy) {
      return (int) datasource.stream()
              .filter(o -> filter(FacesContext.getCurrentInstance(), filterBy.values(), o))
              .count();
  }

  @Override
  public List<Cms> load(int offset, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
      // apply offset & filters
      List<Cms> cmses = datasource.stream()
              .filter(o -> filter(FacesContext.getCurrentInstance(), filterBy.values(), o))
              .collect(Collectors.toList());

      // sort
      if (!sortBy.isEmpty()) {
          List<Comparator<Cms>> comparators = sortBy.values().stream()
                  .map(o -> new LazyCmsSorter(o.getOrder()))
                  .collect(Collectors.toList());
          Comparator<Cms> cp = ComparatorUtils.chainedComparator(comparators);
          cmses.sort(cp);
      }

      return cmses.subList(offset, Math.min(offset + pageSize, cmses.size()));
  }

  private boolean filter(FacesContext context, Collection<FilterMeta> filterBy, Object o) {
      boolean matching = true;

      for (FilterMeta filter : filterBy) {
          FilterConstraint constraint = filter.getConstraint();
          Object filterValue = filter.getFilterValue();

          try {
              Object columnValue = String.valueOf(o.toString());
              matching = constraint.isMatching(context, columnValue, filterValue, LocaleUtils.getCurrentLocale());
          }
          catch (Exception e) {
              matching = false;
          }

          if (!matching) {
              break;
          }
      }

      return matching;
  }

}
