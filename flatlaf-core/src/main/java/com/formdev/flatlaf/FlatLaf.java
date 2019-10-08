/*
 * Copyright 2019 FormDev Software GmbH
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

package com.formdev.flatlaf;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIDefaults.LazyValue;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;
import javax.swing.plaf.metal.MetalLookAndFeel;
import com.formdev.flatlaf.ui.FlatEmptyBorder;
import com.formdev.flatlaf.ui.FlatLineBorder;
import com.formdev.flatlaf.util.ScaledNumber;
import com.formdev.flatlaf.util.SystemInfo;
import com.formdev.flatlaf.util.UIScale;

/**
 * The base class for all Flat LaFs.
 *
 * @author Karl Tauber
 */
public abstract class FlatLaf
	extends BasicLookAndFeel
{
	private static final String VARIABLE_PREFIX = "@";
	private static final String REF_PREFIX = VARIABLE_PREFIX + "@";
	private static final String OPTIONAL_PREFIX = "?";
	private static final String GLOBAL_PREFIX = "*.";

	private BasicLookAndFeel base;

	private AWTEventListener mnemonicListener;
	private static boolean altKeyPressed;

	@Override
	public String getID() {
		return getName();
	}

	@Override
	public boolean isNativeLookAndFeel() {
		return true;
	}

	@Override
	public boolean isSupportedLookAndFeel() {
		return true;
	}

	@Override
	public void initialize() {
		getBase().initialize();

		super.initialize();

		// add mnemonic listener
		mnemonicListener = e -> {
			if( e instanceof KeyEvent && ((KeyEvent)e).getKeyCode() == KeyEvent.VK_ALT )
				altKeyChanged( e.getID() == KeyEvent.KEY_PRESSED );
		};
		Toolkit.getDefaultToolkit().addAWTEventListener( mnemonicListener, AWTEvent.KEY_EVENT_MASK );
	}

	@Override
	public void uninitialize() {
		// remove mnemonic listener
		if( mnemonicListener != null ) {
			Toolkit.getDefaultToolkit().removeAWTEventListener( mnemonicListener );
			mnemonicListener = null;
		}

		if( base != null )
			base.uninitialize();

		super.uninitialize();
	}

	/**
	 * Get/create base LaF. This is used to grab base UI defaults from different LaFs.
	 * E.g. on Mac from system dependent LaF, otherwise from Metal LaF.
	 */
	private BasicLookAndFeel getBase() {
		if( base == null ) {
			if( SystemInfo.IS_MAC ) {
				// use Mac Aqua LaF as base
				try {
					base = (BasicLookAndFeel) Class.forName( "com.apple.laf.AquaLookAndFeel" ).newInstance();
				} catch( Exception ex ) {
					ex.printStackTrace();
					throw new IllegalStateException();
				}
			} else
				base = new MetalLookAndFeel();
		}
		return base;
	}

	@Override
	public UIDefaults getDefaults() {
		UIDefaults defaults = getBase().getDefaults();

		// initialize some defaults (for overriding) that are used in basic UI delegates,
		// but are not set in MetalLookAndFeel or BasicLookAndFeel
		Color control = defaults.getColor( "control" );
		defaults.put( "EditorPane.disabledBackground", control );
		defaults.put( "EditorPane.inactiveBackground", control );
		defaults.put( "FormattedTextField.disabledBackground", control );
		defaults.put( "PasswordField.disabledBackground", control );
		defaults.put( "TextArea.disabledBackground", control );
		defaults.put( "TextArea.inactiveBackground", control );
		defaults.put( "TextField.disabledBackground", control );
		defaults.put( "TextPane.disabledBackground", control );
		defaults.put( "TextPane.inactiveBackground", control );

		// initialize some own defaults (for overriding)
		defaults.put( "Spinner.disabledBackground", control );
		defaults.put( "Spinner.disabledForeground", control );

		// remember MenuBarUI from Mac Aqua LaF if Mac screen menubar is enabled
		boolean useScreenMenuBar = SystemInfo.IS_MAC && "true".equals( System.getProperty( "apple.laf.useScreenMenuBar" ) );
		Object aquaMenuBarUI = useScreenMenuBar ? defaults.get( "MenuBarUI" ) : null;

		initFonts( defaults );
		loadDefaultsFromProperties( defaults );

		// use Aqua MenuBarUI if Mac screen menubar is enabled
		if( useScreenMenuBar )
			defaults.put( "MenuBarUI", aquaMenuBarUI );

		return defaults;
	}

	private void initFonts( UIDefaults defaults ) {
		FontUIResource uiFont = null;

		if( SystemInfo.IS_WINDOWS ) {
			Font winFont = (Font) Toolkit.getDefaultToolkit().getDesktopProperty( "win.messagebox.font" );
			if( winFont != null )
				uiFont = new FontUIResource( winFont );

		} else if( SystemInfo.IS_MAC ) {
			Font font = defaults.getFont( "Label.font" );

			if( SystemInfo.IS_MAC_OS_10_11_EL_CAPITAN_OR_LATER ) {
				// use San Francisco Text font
				font = new FontUIResource( ".SF NS Text", font.getStyle(), font.getSize() );
			}

			uiFont = (font instanceof FontUIResource) ? (FontUIResource) font : new FontUIResource( font );

		} else if( SystemInfo.IS_LINUX ) {
			System.err.println( "WARNING: FlatLaf is not yet tested on Linux!" );
		}

		if( uiFont == null )
			return;

		uiFont = UIScale.applyCustomScaleFactor( uiFont );

		// override fonts
		for( Object key : defaults.keySet() ) {
			if( key instanceof String && ((String)key).endsWith( ".font" ) )
				defaults.put( key, uiFont );
		}
		defaults.put( "MenuItem.acceleratorFont", uiFont );
	}

	/**
	 * Load properties associated to Flat LaF classes and add to UI defaults.
	 *
	 * Each class that extend this class may have its own .properties file
	 * in the same package as the class. Properties from superclasses are loaded
	 * first to give subclasses a chance to override defaults.
	 * E.g. if running FlatDarkLaf, then the FlatLaf.properties is loaded first
	 * and FlatDarkLaf.properties loaded second.
	 */
	private void loadDefaultsFromProperties( UIDefaults defaults ) {
		// determine classes in class hierarchy in reverse order
		ArrayList<Class<?>> lafClasses = new ArrayList<>();
		for( Class<?> lafClass = getClass();
			FlatLaf.class.isAssignableFrom( lafClass );
			lafClass = lafClass.getSuperclass() )
		{
			lafClasses.add( 0, lafClass );
		}

		try {
			// load properties files
			Properties properties = new Properties();
			for( Class<?> lafClass : lafClasses ) {
				String propertiesName = "/" + lafClass.getName().replace( '.', '/' ) + ".properties";
				try( InputStream in = lafClass.getResourceAsStream( propertiesName ) ) {
					if( in != null )
						properties.load( in );
				}
			}

			Function<String, String> resolver = value -> {
				return resolveValue( properties, value );
			};

			// get globals, which override all other defaults that end with same suffix
			HashMap<String, Object> globals = new HashMap<>();
			for( Map.Entry<Object, Object> e : properties.entrySet() ) {
				String key = (String) e.getKey();
				if( !key.startsWith( GLOBAL_PREFIX ) )
					continue;

				String value = resolveValue( properties, (String) e.getValue() );
				globals.put( key.substring( GLOBAL_PREFIX.length() ), parseValue( key, value, resolver ) );
			}

			// override UI defaults with globals
			for( Object key : defaults.keySet() ) {
				if( key instanceof String && ((String)key).contains( "." ) ) {
					String skey = (String) key;
					String globalKey = skey.substring( skey.lastIndexOf( '.' ) + 1 );
					Object globalValue = globals.get( globalKey );
					if( globalValue != null )
						defaults.put( key, globalValue );
				}
			}

			// add non-global properties to UI defaults
			for( Map.Entry<Object, Object> e : properties.entrySet() ) {
				String key = (String) e.getKey();
				if( key.startsWith( VARIABLE_PREFIX ) || key.startsWith( GLOBAL_PREFIX ) )
					continue;

				String value = resolveValue( properties, (String) e.getValue() );
				defaults.put( key, parseValue( key, value, resolver ) );
			}
		} catch( IOException ex ) {
			ex.printStackTrace();
		}
	}

	private String resolveValue( Properties properties, String value ) {
		if( !value.startsWith( VARIABLE_PREFIX ) )
			return value;

		if( value.startsWith( REF_PREFIX ) )
			value = value.substring( REF_PREFIX.length() );

		boolean optional = false;
		if( value.startsWith( OPTIONAL_PREFIX ) ) {
			value = value.substring( OPTIONAL_PREFIX.length() );
			optional = true;
		}

		String newValue = properties.getProperty( value );
		if( newValue == null ) {
			if( optional )
				return "null";

			System.err.println( "variable or reference '" + value + "' not found" );
			throw new IllegalArgumentException( value );
		}

		return resolveValue( properties, newValue );
	}

	private Object parseValue( String key, String value, Function<String, String> resolver ) {
		value = value.trim();

		// null, false, true
		switch( value ) {
			case "null":		return null;
			case "false":	return false;
			case "true":		return true;
		}

		// borders
		if( key.endsWith( ".border" ) || key.endsWith( "Border" ) )
			return parseBorder( value, resolver );

		// icons
		if( key.endsWith( ".icon" ) || key.endsWith( "Icon" ) )
			return parseInstance( value );

		// insets
		if( key.endsWith( ".margin" ) || key.endsWith( ".padding" ) ||
			key.endsWith( "Margins" ) || key.endsWith( "Insets" ) )
			return parseInsets( value );

		// scaled number
		ScaledNumber scaledNumber = parseScaledNumber( key, value );
		if( scaledNumber != null )
			return scaledNumber;

		// size
		if( key.endsWith( "Size" ) && !key.equals( "SplitPane.dividerSize" ))
			return parseSize( value );

		// width, height
		if( key.endsWith( "Width" ) || key.endsWith( "Height" ) )
			return parseInteger( value, true );

		// colors
		ColorUIResource color = parseColor( value, false );
		if( color != null )
			return color;

		// integer
		Integer integer = parseInteger( value, false );
		if( integer != null )
			return integer;

		// string
		return value;
	}

	private Object parseBorder( String value, Function<String, String> resolver ) {
		if( value.indexOf( ',' ) >= 0 ) {
			// top,left,bottom,right[,lineColor]
			List<String> parts = split( value, ',' );
			Insets insets = parseInsets( value );
			ColorUIResource lineColor = (parts.size() == 5)
				? parseColor( resolver.apply( parts.get( 4 ) ), true )
				: null;

			return (LazyValue) t -> {
				return (lineColor != null)
					? new FlatLineBorder( insets, lineColor )
					: new FlatEmptyBorder( insets );
			};
		} else
			return parseInstance( value );
	}

	private Object parseInstance( String value ) {
		return (LazyValue) t -> {
			try {
				return Class.forName( value ).newInstance();
			} catch( InstantiationException | IllegalAccessException | ClassNotFoundException ex ) {
				ex.printStackTrace();
				return null;
			}
		};
	}

	private Insets parseInsets( String value ) {
		List<String> numbers = split( value, ',' );
		try {
			return new InsetsUIResource(
				Integer.parseInt( numbers.get( 0 ) ),
				Integer.parseInt( numbers.get( 1 ) ),
				Integer.parseInt( numbers.get( 2 ) ),
				Integer.parseInt( numbers.get( 3 ) ) );
		} catch( NumberFormatException ex ) {
			System.err.println( "invalid insets '" + value + "'" );
			throw ex;
		}
	}

	private Dimension parseSize( String value ) {
		List<String> numbers = split( value, ',' );
		try {
			return new DimensionUIResource(
				Integer.parseInt( numbers.get( 0 ) ),
				Integer.parseInt( numbers.get( 1 ) ) );
		} catch( NumberFormatException ex ) {
			System.err.println( "invalid size '" + value + "'" );
			throw ex;
		}
	}

	private ColorUIResource parseColor( String value, boolean reportError ) {
		try {
			int rgb = Integer.parseInt( value, 16 );
			if( value.length() == 6 )
				return new ColorUIResource( rgb );
			if( value.length() == 8 )
				return new ColorUIResource( new Color( rgb, true ) );

			if( reportError )
				throw new NumberFormatException( value );
		} catch( NumberFormatException ex ) {
			if( reportError ) {
				System.err.println( "invalid color '" + value + "'" );
				throw ex;
			}
			// not a color --> ignore
		}
		return null;
	}

	private Integer parseInteger( String value, boolean reportError ) {
		try {
			return Integer.parseInt( value );
		} catch( NumberFormatException ex ) {
			if( reportError ) {
				System.err.println( "invalid integer '" + value + "'" );
				throw ex;
			}
		}
		return null;
	}

	private ScaledNumber parseScaledNumber( String key, String value ) {
		if( !key.equals( "OptionPane.buttonMinimumWidth" ) &&
			!key.equals( "SplitPane.oneTouchButtonSize" ) &&
			!key.equals( "SplitPane.oneTouchButtonOffset" ) )
		  return null; // not supported

		try {
			return new ScaledNumber( Integer.parseInt( value ) );
		} catch( NumberFormatException ex ) {
			System.err.println( "invalid integer '" + value + "'" );
			throw ex;
		}
	}

	public static List<String> split( String str, char delim ) {
		ArrayList<String> strs = new ArrayList<>();
		int delimIndex = str.indexOf( delim );
		int index = 0;
		while( delimIndex >= 0 ) {
			strs.add( str.substring( index, delimIndex ) );
			index = delimIndex + 1;
			delimIndex = str.indexOf( delim, index );
		}
		strs.add( str.substring( index ) );

		return strs;
	}

	public static boolean isShowMnemonics() {
		return altKeyPressed || !UIManager.getBoolean( "Component.hideMnemonics" );
	}

	private static void altKeyChanged( boolean pressed ) {
		if( pressed == altKeyPressed )
			return;

		altKeyPressed = pressed;

		// check whether it is necessary to repaint
		if( !UIManager.getBoolean( "Component.hideMnemonics" ) )
			return;

		// get focus owner
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if( focusOwner == null )
			return;

		// get focused window
		Window window = SwingUtilities.windowForComponent( focusOwner );
		if( window == null )
			return;

		// repaint components with mnemonics in focused window
		repaintMnemonics( window );
	}

	private static void repaintMnemonics( Container container ) {
		for( Component c : container.getComponents() ) {
			if( hasMnemonic( c ) )
				c.repaint();

			if( c instanceof Container )
				repaintMnemonics( (Container) c );
		}
	}

	private static boolean hasMnemonic( Component c ) {
		if( c instanceof JLabel && ((JLabel)c).getDisplayedMnemonicIndex() >= 0 )
			return true;

		if( c instanceof AbstractButton && ((AbstractButton)c).getDisplayedMnemonicIndex() >= 0 )
			return true;

		if( c instanceof JTabbedPane ) {
			JTabbedPane tabPane = (JTabbedPane) c;
			int tabCount = tabPane.getTabCount();
			for( int i = 0; i < tabCount; i++ ) {
				if( tabPane.getDisplayedMnemonicIndexAt( i ) >= 0 )
					return true;
			}
		}

		return false;
	}
}
