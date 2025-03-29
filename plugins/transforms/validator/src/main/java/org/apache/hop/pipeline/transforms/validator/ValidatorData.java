/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.hop.pipeline.transforms.validator;

import java.util.regex.Pattern;
import org.apache.hop.core.row.IRowMeta;
import org.apache.hop.core.row.IValueMeta;
import org.apache.hop.pipeline.transform.BaseTransformData;
import org.apache.hop.pipeline.transform.ITransformData;

@SuppressWarnings("java:S1104")
public class ValidatorData extends BaseTransformData implements ITransformData {

  public int[] fieldIndexes;

  public IValueMeta[] constantsMeta;
  public String[] minimumValueAsString;
  public String[] maximumValueAsString;
  public int[] fieldsMinimumLengthAsInt;
  public int[] fieldsMaximumLengthAsInt;
  public Object[][] listValues;

  public Pattern[] patternExpected;

  public Pattern[] patternDisallowed;

  public String[] errorCode;
  public String[] errorDescription;
  public String[] conversionMask;
  public String[] decimalSymbol;
  public String[] groupingSymbol;
  public String[] maximumLength;
  public String[] minimumLength;
  public Object[] maximumValue;
  public Object[] minimumValue;
  public String[] startString;
  public String[] endString;
  public String[] startStringNotAllowed;
  public String[] endStringNotAllowed;
  public String[] regularExpression;
  public String[] regularExpressionNotAllowed;
  public IRowMeta inputRowMeta;

  public ValidatorData() {
    super();
  }
}
