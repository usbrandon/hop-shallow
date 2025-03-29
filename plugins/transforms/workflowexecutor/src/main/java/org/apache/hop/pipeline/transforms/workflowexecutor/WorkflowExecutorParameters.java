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
 */

package org.apache.hop.pipeline.transforms.workflowexecutor;

import lombok.Getter;
import lombok.Setter;
import org.apache.hop.metadata.api.HopMetadataProperty;

/**
 * The workflow to be executed in the pipeline can receive parameters. These are either coming from
 * an input row (the first row in a group of rows) or from a static variable or value.
 */
@Getter
@Setter
public class WorkflowExecutorParameters implements Cloneable {

  @HopMetadataProperty(key = "variable")
  private String variable;

  @HopMetadataProperty(key = "field")
  private String field;

  @HopMetadataProperty(key = "input")
  private String input;
}
