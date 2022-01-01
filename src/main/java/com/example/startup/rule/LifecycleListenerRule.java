package com.example.startup.rule;

import com.example.life.Lifecycle;
import com.example.life.LifecycleListener;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.xml.sax.Attributes;


public class LifecycleListenerRule extends Rule {


    /**
     * Construct a new instance of this Rule.
     *
     * @param digester Digester we are associated with
     * @param listenerClass Default name of the LifecycleListener
     *  implementation class to be created
     * @param attributeName Name of the attribute that optionally
     *  includes an override name of the LifecycleListener class
     */
    public LifecycleListenerRule(Digester digester, String listenerClass,
                                 String attributeName) {
        super(digester);
        this.listenerClass = listenerClass;
        this.attributeName = attributeName;
    }


    /**
     * The attribute name of an attribute that can override the
     * implementation class name.
     */
    private final String attributeName;


    /**
     * The name of the <code>LifecycleListener</code> implementation class.
     */
    private final String listenerClass;



    /**
     * Handle the beginning of an XML element.
     *
     * @param attributes The attributes of this element
     *
     * @exception Exception if a processing error occurs
     */
    public void begin(Attributes attributes) throws Exception {
        // Instantiate a new LifecyleListener implementation object
        String className = listenerClass;
        if (attributeName != null) {
            String value = attributes.getValue(attributeName);
            if (value != null)
                className = value;
        }
        Class<?> clazz = Class.forName(className);
        LifecycleListener listener =
            (LifecycleListener) clazz.newInstance();

        // Add this LifecycleListener to our associated component
        Lifecycle lifecycle = (Lifecycle) digester.peek();
        lifecycle.addLifecycleListener(listener);
    }


}
