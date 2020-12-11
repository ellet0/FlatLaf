/*
 * Copyright 2020 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.formdev.flatlaf.extras.components;

import static com.formdev.flatlaf.FlatClientProperties.*;
import javax.swing.JComboBox;

/**
 * Subclass of {@link JComboBox} that provides easy access to FlatLaf specific client properties.
 *
 * @author Karl Tauber
 */
public class FlatComboBox<E>
	extends JComboBox<E>
	implements FlatComponentExtension
{
	/**
	 * Returns the placeholder text that is only painted if the editable combo box is empty.
	 */
	public String getPlaceholderText() {
		return (String) getClientProperty( PLACEHOLDER_TEXT );
	}

	/**
	 * Sets the placeholder text that is only painted if the editable combo box is empty.
	 */
	public void setPlaceholderText( String placeholderText ) {
		putClientProperty( PLACEHOLDER_TEXT, placeholderText );
	}


	/**
	 * Returns minimum width of a component.
	 */
	public int getMinimumWidth() {
		return getClientPropertyInt( MINIMUM_WIDTH, "ComboBox.minimumWidth" );
	}

	/**
	 * Specifies minimum width of a component.
	 */
	public void setMinimumWidth( int minimumWidth ) {
		putClientProperty( MINIMUM_WIDTH, (minimumWidth >= 0) ? minimumWidth : null );
	}


	/**
	 * Returns whether the component is painted with round edges.
	 */
	public boolean isRoundRect() {
		return getClientPropertyBoolean( COMPONENT_ROUND_RECT, false );
	}

	/**
	 * Specifies whether the component is painted with round edges.
	 */
	public void setRoundRect( boolean roundRect ) {
		putClientPropertyBoolean( COMPONENT_ROUND_RECT, roundRect, false );
	}
}