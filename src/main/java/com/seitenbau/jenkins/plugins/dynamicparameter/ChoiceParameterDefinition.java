/*
 * Copyright 2012 Seitenbau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.seitenbau.jenkins.plugins.dynamicparameter;

import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;

import java.util.Collections;
import java.util.List;

import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/** Choice parameter, with dynamically generated list of values. */
public class ChoiceParameterDefinition extends ParameterDefinitionBase
{
  /** Serial version UID. */
  private static final long serialVersionUID = 5454277528808586236L;

  /**
   * Constructor.
   * @param name parameter name
   * @param script script, which generates the parameter value
   * @param description parameter description
   * @param uuid identifier (optional)
   * @param remote execute the script on a remote node
   */
  @DataBoundConstructor
  public ChoiceParameterDefinition(String name, String script, String description, String uuid,
      boolean remote)
  {
    super(name, script, description, uuid, remote);
  }

  /**
   * Get the possible choice, generated by the script.
   * @return list of values if the script returns a non-null list;
   *         {@link Collections#EMPTY_LIST}, otherwise
   */
  @SuppressWarnings("unchecked")
  public final List<Object> getChoices()
  {
    final Object value = getValue();
    if (value instanceof List)
    {
      return (List<Object>) value;
    }
    final String name = getName();
    String msg = String.format("Script parameter with name '%s' is not a instance of "
        + "java.util.List the parameter value is : %s", name, value);
    logger.info(msg);
    return Collections.EMPTY_LIST;
  }

  @Override
  public final ParameterValue createValue(StaplerRequest req, JSONObject jo)
  {
    final StringParameterValue parameterValue = req.bindJSON(StringParameterValue.class, jo);
    parameterValue.setDescription(getDescription());
    return findPreDefinedParameterValue(parameterValue);
  }
  
  @Override
  public final ParameterValue createValue(StaplerRequest req)
  {
    String name = getName();
    String[] values = req.getParameterValues(name);
    return createParameterValue(name, values);
  }

  private ParameterValue createParameterValue(String name, String[] values) 
  {
    if (values == null)
    {
      return getDefaultParameterValue();
    }
    else if (values.length == 1)
    {
      StringParameterValue parameterValue = createStringParameterValueFor(name, values[0]);
      return findPreDefinedParameterValue(parameterValue);
    }
    else
    {
      throw new IllegalArgumentException(String.format(
          "Illegal number of parameter values for '%s': %d", getName(), values.length));
    }
}
  
  /**
   * Factory methods creates a String parameter value object for the given value.
   * @param value of the object
   * @return String parameter value object not null.
   */
  private StringParameterValue createStringParameterValueFor(String name, String value) 
  {
     String description = getDescription();
     StringParameterValue parameterValue = new StringParameterValue(name, value, description);
     return parameterValue;
   }

  /**
   * Check if the given parameter value is within the list of possible
   * values.
   * @param parameter parameter value to check
   * @return the value if it is valid
   */
  private StringParameterValue findPreDefinedParameterValue(StringParameterValue parameter)
  {
    final String actualValue = ObjectUtils.toString(parameter.value);
    for (final Object choice : getChoices())
    {
      final String choiceValue = ObjectUtils.toString(choice);
      if (StringUtils.equals(actualValue, choiceValue))
      {
        return parameter;
      }
    }
    throw new IllegalArgumentException("Illegal choice: " + actualValue);
  }

  /** Parameter descriptor. */
  @Extension
  public static class DescriptorImpl extends ParameterDescriptor
  {
    private static final String DISPLAY_NAME = "DisplayName";

	@Override
    public final String getDisplayName()
    {
      return ResourceBundleHolder.get(ChoiceParameterDefinition.class).format(DISPLAY_NAME);
    }
  }
}