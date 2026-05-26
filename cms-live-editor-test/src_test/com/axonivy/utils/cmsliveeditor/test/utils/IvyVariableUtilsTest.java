package com.axonivy.utils.cmsliveeditor.test.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.cmsliveeditor.constants.IvyVariables;
import com.axonivy.utils.cmsliveeditor.utils.IvyVariableUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class IvyVariableUtilsTest {

  private static final int CHANGED_VALUE = 100;
  private static final int DEFAULT_VALUE = 10;

  @Test
  public void testGetIntegerVariableOrDefaultValidVariable() {
    Ivy.var().set(IvyVariables.MAX_TRANSLATED_CMS_ENTRIES_FOR_WARNING, String.valueOf(CHANGED_VALUE));
    assertEquals(CHANGED_VALUE, IvyVariableUtils
        .getIntegerVariableOrDefault(IvyVariables.MAX_TRANSLATED_CMS_ENTRIES_FOR_WARNING, DEFAULT_VALUE));
  }

  @Test
  public void testGetIntegerVariableOrDefaultWithNullVariable() {
    assertEquals(DEFAULT_VALUE, IvyVariableUtils.getIntegerVariableOrDefault(null, DEFAULT_VALUE));
  }
}
