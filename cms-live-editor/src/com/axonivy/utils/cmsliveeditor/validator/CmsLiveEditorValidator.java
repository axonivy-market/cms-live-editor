package com.axonivy.utils.cmsliveeditor.validator;

import java.util.List;
import java.util.Map;

import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.FacesValidator;
import jakarta.faces.validator.Validator;
import jakarta.faces.validator.ValidatorException;

import org.primefaces.PF;

import com.axonivy.utils.cmsliveeditor.managedbean.CmsLiveEditorBean;
import com.axonivy.utils.cmsliveeditor.model.Cms;
import com.axonivy.utils.cmsliveeditor.model.SavedCms;
import com.axonivy.utils.cmsliveeditor.service.PlaceholderService;
import com.axonivy.utils.cmsliveeditor.utils.FacesContexts;
import jakarta.enterprise.context.ApplicationScoped;

@FacesValidator(value = "cmsLiveEditorValidator", managed = true)
@ApplicationScoped
public class CmsLiveEditorValidator implements Validator<Object> {
  private final PlaceholderService placeholderService = PlaceholderService.getInstance();

  @Override
  public void validate(FacesContext context, UIComponent component, Object value) throws ValidatorException {
    CmsLiveEditorBean bean = FacesContexts.evaluateValueExpression("#{cmsLiveEditorBean}", CmsLiveEditorBean.class);
    if (bean == null) {
      return;
    }

    Cms selectedCms = bean.getSelectedCms();
    Map<String, SavedCms> savedLocales = bean.getSavedCmsMap().getOrDefault(selectedCms.getUri(), Map.of());
    List<Integer> invalidIndices = placeholderService.findInvalidLanguageIndices(selectedCms, savedLocales);
    selectedCms.getContents().forEach(content -> content.setInvalid(invalidIndices.contains(content.getIndex())));
    int languageIndex = (int) component.getAttributes().get("languageIndex");
    if (invalidIndices.contains(languageIndex)) {
      ((UIInput) component).setValid(false);
      context.validationFailed();
      PF.current().ajax().addCallbackParam("invalidIndices", invalidIndices.toString());
      selectedCms.getContents().stream()
          .filter(content -> !bean.isTheSameContent(content.getOriginalContent(), content.getContent()))
          .forEach(content -> content.setEditing(true));
    }
  }

}
