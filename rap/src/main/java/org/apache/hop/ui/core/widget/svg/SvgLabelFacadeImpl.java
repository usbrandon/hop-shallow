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

package org.apache.hop.ui.core.widget.svg;

import org.apache.hop.core.Const;
import org.eclipse.rap.json.JsonObject;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.client.service.JavaScriptExecutor;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolItem;

public class SvgLabelFacadeImpl extends SvgLabelFacade {
  @Override
  public void setDataInternal(String id, Label label, String imageFile, int size) {
    // What's the location of the SVG file?
    try {
      String src = RWT.getResourceManager().getLocation(imageFile);
      label.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
      label.setText(
          "<img id='"
              + id
              + "' width='"
              + size
              + "' height='"
              + size
              + "' style='background-color: transparent' src='"
              + src
              + "'/>");

      JsonObject jsonProps = new JsonObject();
      jsonProps.add("id", id);
      jsonProps.add("enabled", true);
      label.setData("props", jsonProps);
    } catch (Exception e) {
      System.err.println(
          "Error setting internal data on tool-item " + id + " label for filename: " + imageFile);
      System.err.println(Const.getSimpleStackTrace(e));
    }
  }

  @Override
  public void enableInternal(ToolItem toolItem, String id, Label label, boolean enable) {
    // Show/Hide the label
    // This causes an event in svg-label.js
    //
    JsonObject jsonProps = new JsonObject();
    jsonProps.add("id", id);
    jsonProps.add("enabled", enable);
    label.setData("props", jsonProps);

    String opacity;
    if (enable) {
      opacity = "'1.0'";
    } else {
      opacity = "'0.3'";
    }
    exec("document.getElementById('", id, "').style.opacity=", opacity, ";");
  }

  @Override
  public void shadeSvgInternal(Label label, String id, boolean shaded) {
    String color;
    if (shaded) {
      color = "'rgb(180,180,180)'";
    } else {
      color = "'transparent'";
    }
    exec("document.getElementById('", id, "').style.background=", color, ";");
  }

  private static void exec(String... strings) {
    StringBuilder builder = new StringBuilder();
    builder.append("try {");
    for (String str : strings) {
      builder.append(str);
    }
    builder.append("} catch (e) { console.log(\"JS error\"); }");
    JavaScriptExecutor executor = RWT.getClient().getService(JavaScriptExecutor.class);
    executor.execute(builder.toString());
  }
}
