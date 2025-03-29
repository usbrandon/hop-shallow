/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.ui.hopgui.file.pipeline.extension;

import org.apache.hop.core.gui.plugin.ITypeFilename;
import org.apache.hop.ui.hopgui.file.pipeline.HopPipelineFileType;

public class TypePipelineFile implements ITypeFilename {

  private HopPipelineFileType fileType;

  public TypePipelineFile() {
    fileType = new HopPipelineFileType();
  }

  @Override
  public String getDefaultFileExtension() {
    return fileType.getDefaultFileExtension();
  }

  @Override
  public String[] getFilterExtensions() {
    return fileType.getFilterExtensions();
  }

  @Override
  public String[] getFilterNames() {
    return fileType.getFilterNames();
  }
}
