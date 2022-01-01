package com.example.startup.rule;


import com.example.Container;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.xml.sax.Attributes;

import java.lang.reflect.Method;


/**
 * <p>Rule that copies the <code>parentClassLoader</code> property from the
 * next-to-top item on the stack (which must be a <code>Container</code>)
 * to the top item on the stack (which must also be a
 * <code>Container</code>).</p>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2001/10/25 00:23:03 $
 */
public class CopyParentClassLoaderRule extends Rule {


    /**
     * Construct a new instance of this Rule.
     *
     * @param digester Digester we are associated with
     */
    public CopyParentClassLoaderRule(Digester digester) {
        super(digester);
    }


    /**
     * Handle the beginning of an XML element.
     *
     * @param attributes The attributes of this element
     *
     * @exception Exception if a processing error occurs
     */
    public void begin(Attributes attributes) throws Exception {
        if (digester.getDebug() >= 1)
            digester.log("Copying parent class loader");
        Container child = (Container) digester.peek(0);
        Object parent = digester.peek(1);
        Method method = parent.getClass().getMethod("getParentClassLoader");
        ClassLoader classLoader =
            (ClassLoader) method.invoke(parent, new Object[0]);
        child.setParentClassLoader(classLoader);
    }


}
