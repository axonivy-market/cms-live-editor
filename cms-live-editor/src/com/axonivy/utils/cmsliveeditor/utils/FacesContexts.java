package com.axonivy.utils.cmsliveeditor.utils;

import java.util.Objects;

import javax.faces.application.Application;
import javax.faces.context.FacesContext;

/**
 * This class is to provide utility functions interacting JSF by FacesContext
 */
public final class FacesContexts {

  private FacesContexts() {
  }

  @SuppressWarnings("unchecked")
  public static <E> E evaluateValueExpression(String valueExpressionLiteal, Class<E> returnedType) {
    Application application = getApplication();
    Object value = application.evaluateExpressionGet(getCurrentInstance(), valueExpressionLiteal, returnedType);
    return Objects.nonNull(returnedType) ? returnedType.cast(value) : (E) value;
  }

  public static Application getApplication() {
    return getCurrentInstance().getApplication();
  }

  private static FacesContext getCurrentInstance() {
    return FacesContext.getCurrentInstance();
  }

}