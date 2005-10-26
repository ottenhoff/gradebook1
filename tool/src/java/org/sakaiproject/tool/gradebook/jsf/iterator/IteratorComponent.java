/**********************************************************************************
*
* $Id$
*
***********************************************************************************
*
* Copyright (c) 2005 The Regents of the University of California, The MIT Corporation
*
* Licensed under the Educational Community License Version 1.0 (the "License");
* By obtaining, using and/or copying this Original Work, you agree that you have read,
* understand, and will comply with the terms and conditions of the Educational Community License.
* You may obtain a copy of the License at:
*
*      http://cvs.sakaiproject.org/licenses/license_1_0.html
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
* INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
* AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
* DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
**********************************************************************************/

package org.sakaiproject.tool.gradebook.jsf.iterator;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A simple looping component which encodes all its children for every record in
 * the input collection.
 *
 * <gbx:iterator value="#{assignmentDetailsBean.scoreRows}" var="scoreRow" rowIndexVar="scoreRowPos">
 *    ...
 * </gbx:iterator>
 */

public class IteratorComponent extends UIComponentBase implements NamingContainer {
	private static final Log log = LogFactory.getLog(IteratorComponent.class);

	public final static String COMPONENT_TYPE = "org.sakaiproject.tool.gradebook.jsf.iterator";
	public final static String COMPONENT_FAMILY = "javax.faces.Data";

	private Object value = null;
	private String var = null;
	private String rowIndexVar = null;
	private Integer rowIndex = null;
	private Boolean rendered = null;

	public void encodeChildren(FacesContext context) throws IOException {
		if (!isRendered()) {
			return;
		}

		Collection dataModel = getDataModel();
		if (dataModel != null) {
			Map requestMap = context.getExternalContext().getRequestMap();
			int rowIndex = 0;
			for (Iterator iter = dataModel.iterator(); iter.hasNext(); rowIndex++) {
				Object varObject = iter.next();
				if (var != null) {
					if (varObject != null) {
						requestMap.put(var, varObject);
					} else {
						requestMap.remove(var);
					}
				}
				if (rowIndexVar != null) {
					requestMap.put(rowIndexVar, new Integer(rowIndex));
				}
				renderRowChildren(context);
			}
			if (var != null) {
				requestMap.remove(var);
			}
			if (rowIndexVar != null) {
				requestMap.remove(rowIndexVar);
			}
		}
	}

	/**

	 * Subclasses can decorate the children as they see fit.
	 */
	protected void renderRowChildren(FacesContext context) throws IOException {
		for (Iterator iter = getChildren().iterator(); iter.hasNext(); ) {
			encodeRecursive(context, (UIComponent)iter.next());
		}
	}

	protected void encodeRecursive(FacesContext context, UIComponent component) throws IOException {
		if (component.isRendered()) {
			component.encodeBegin(context);
			if (component.getRendersChildren()) {
				component.encodeChildren(context);
			} else {
				for (Iterator iter = component.getChildren().iterator(); iter.hasNext(); ) {
					encodeRecursive(context, (UIComponent)iter.next());
				}
			}
			component.encodeEnd(context);
		}
	}

	private Collection getDataModel() {
		Collection dataModel = null;
		Object val = getValue();
		if (val != null) {
			if (val instanceof Collection) {
				dataModel = (Collection)val;
			} else {
				if (log.isDebugEnabled()) log.debug("value is not a Collection: " + val);
			}
		}
		return dataModel;
	}

	public void encodeBegin(FacesContext context) throws IOException {
	}
	public void encodeEnd(FacesContext context) throws IOException {
		return;
	}

	public void decode(FacesContext context) {
		return;
	}

	public boolean getRendersChildren() {
		return true;
	}

	public void setValueBinding(String name, ValueBinding binding) {
		if ("var".equals(name) || "rowIndexVar".equals(name)) {
			throw new IllegalArgumentException();
		}
		super.setValueBinding(name, binding);
	}

	protected Object getFieldOrBinding(Object field, String bindingName) {
		Object retVal = null;
		if (field != null) {
			retVal = field;
		} else {
			ValueBinding binding = getValueBinding(bindingName);
			if (binding != null) {
				retVal = binding.getValue(getFacesContext());
			}
		}
		return retVal;
	}

	public Object getValue() {
		return getFieldOrBinding(value, "value");
	}
	public void setValue(Object value) {
		this.value = value;
	}
	public String getVar() {
		return var;
	}
	public void setVar(String var) {
		this.var = var;
	}
	public void setRowIndexVar(String rowIndexVar) {
		this.rowIndexVar = rowIndexVar;
	}

	public Object saveState(FacesContext context) {
		Object values[] = new Object[4];
		values[0] = super.saveState(context);
		values[1] = value;
		values[2] = var;
		values[3] = rowIndexVar;
		return values;
	}
	public void restoreState(FacesContext context, Object state) {
		Object values[] = (Object[])state;
		super.restoreState(context, values[0]);
		value = values[1];
		var = (String)values[2];
		rowIndexVar = (String)values[3];
	}

	public String getFamily() {
		return COMPONENT_FAMILY;
	}

}
