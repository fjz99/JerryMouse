package com.example.startup.rule;

import com.example.Container;
import com.example.loader.Loader;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.apache.commons.digester.RuleSetBase;
import org.xml.sax.Attributes;

import java.lang.reflect.Constructor;


/**
 * <p><strong>RuleSet</strong> for processing the contents of a
 * Context or DefaultContext definition element.  To enable parsing of a
 * DefaultContext, be sure to specify a prefix that ends with "/Default".</p>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.3 $ $Date: 2001/11/08 21:03:15 $
 */
public class ContextRuleSet extends RuleSetBase {

    /**
     * The matching pattern prefix to use for recognizing our elements.
     */
    protected String prefix;


    /**
     * Construct an instance of this <code>RuleSet</code> with the default
     * matching pattern prefix.
     */
    public ContextRuleSet() {
        this ("");
    }


    /**
     * Construct an instance of this <code>RuleSet</code> with the specified
     * matching pattern prefix.
     *
     * @param prefix Prefix for matching pattern rules (including the
     *               trailing slash character)
     */
    public ContextRuleSet(String prefix) {
        super ();
        this.namespaceURI = null;
        this.prefix = prefix;
    }


    /**
     * <p>Add the set of Rule instances defined in this RuleSet to the
     * specified <code>Digester</code> instance, associating them with
     * our namespace URI (if any).  This method should only be called
     * by a Digester instance.</p>
     *
     * @param digester Digester instance to which the new Rule instances
     *                 should be added.
     */
    public void addRuleInstances(Digester digester) {
        digester.addObjectCreate (prefix + "Context",
                "com.example.core.StandardContext",
                "className");
        digester.addSetProperties (prefix + "Context");

        digester.addRule (prefix + "Context",
                new CopyParentClassLoaderRule (digester));
        //!!ContextConfig这个listener是自动添加的
        //不需要显式设置
        digester.addRule (prefix + "Context",
                new LifecycleListenerRule
                        (digester, "com.example.startup.ContextConfig", "configClass"));
        digester.addSetNext (prefix + "Context",
                "addChild",
                "com.example.Container");

        digester.addCallMethod (prefix + "Context/InstanceListener",
                "addInstanceListener", 0);

        digester.addObjectCreate (prefix + "Context/Listener",
                null, // MUST be specified in the element
                "className");
        digester.addSetProperties (prefix + "Context/Listener");
        digester.addSetNext (prefix + "Context/Listener",
                "addLifecycleListener",
                "com.example.life.LifecycleListener");

        digester.addRule (prefix + "Context/Loader",
                new CreateLoaderRule
                        (digester,
                                "com.example.loader.WebappLoader",
                                "className"));
        digester.addSetProperties (prefix + "Context/Loader");
        digester.addSetNext (prefix + "Context/Loader",
                "setLoader",
                "com.example.loader.Loader");

//        digester.addObjectCreate (prefix + "Context/Logger",
//                null, // MUST be specified in the element
//                "className");
//        digester.addSetProperties (prefix + "Context/Logger");
//        digester.addSetNext (prefix + "Context/Logger",
//                "setLogger",
//                "org.apache.catalina.Logger");

        digester.addObjectCreate (prefix + "Context/Manager",
                "com.example.session.StandardManager",
                "className");
        digester.addSetProperties (prefix + "Context/Manager");
        digester.addSetNext (prefix + "Context/Manager",
                "setManager",
                "com.example.session.Manager");

        digester.addObjectCreate (prefix + "Context/Manager/Store",
                null, // MUST be specified in the element
                "className");
        digester.addSetProperties (prefix + "Context/Manager/Store");
        digester.addSetNext (prefix + "Context/Manager/Store",
                "setStore",
                "com.example.session.Store");

//        digester.addObjectCreate (prefix + "Context/Parameter",
//                "org.apache.catalina.deploy.ApplicationParameter");
//        digester.addSetProperties (prefix + "Context/Parameter");
//        digester.addSetNext (prefix + "Context/Parameter",
//                "addApplicationParameter",
//                "org.apache.catalina.deploy.ApplicationParameter");

//        digester.addObjectCreate (prefix + "Context/Realm",
//                null, // MUST be specified in the element
//                "className");
//        digester.addSetProperties (prefix + "Context/Realm");
//        digester.addSetNext (prefix + "Context/Realm",
//                "setRealm",
//                "org.apache.catalina.Realm");

//        digester.addObjectCreate (prefix + "Context/ResourceLink",
//                "org.apache.catalina.deploy.ContextResourceLink");
//        digester.addSetProperties (prefix + "Context/ResourceLink");
//        digester.addSetNext (prefix + "Context/ResourceLink",
//                "addResourceLink",
//                "org.apache.catalina.deploy.ContextResourceLink");

//        digester.addObjectCreate (prefix + "Context/Resources",
//                "org.apache.naming.resources.FileDirContext",
//                "className");
//        digester.addSetProperties (prefix + "Context/Resources");
//        digester.addSetNext (prefix + "Context/Resources",
//                "setResources",
//                "javax.naming.directory.DirContext");

        digester.addObjectCreate (prefix + "Context/Valve",
                null, // MUST be specified in the element
                "className");
        digester.addSetProperties (prefix + "Context/Valve");
        digester.addSetNext (prefix + "Context/Valve",
                "addValve",
                "com.example.Valve");

//        digester.addCallMethod (prefix + "Context/WrapperLifecycle",
//                "addWrapperLifecycle", 0);
//
//        digester.addCallMethod (prefix + "Context/WrapperListener",
//                "addWrapperListener", 0);
    }

}


/**
 * Rule that creates a new <code>Loader</code> instance, with the parent
 * class loader associated with the top object on the stack (which must be
 * a <code>Container</code>), and pushes it on to the stack.
 */

final class CreateLoaderRule extends Rule {

    private final String attributeName;
    private final String loaderClass;

    public CreateLoaderRule(Digester digester, String loaderClass,
                            String attributeName) {
        super (digester);
        this.loaderClass = loaderClass;
        this.attributeName = attributeName;
    }

    public void begin(Attributes attributes) throws Exception {
        // Look up the required parent class loader
        Container container = (Container) digester.peek ();
        ClassLoader parentClassLoader = container.getParentClassLoader ();

        // Instantiate a new Loader implementation object
        String className = loaderClass;
        if (attributeName != null) {
            String value = attributes.getValue (attributeName);
            if (value != null)
                className = value;
        }
        Class<?> clazz = Class.forName (className);
        Class<?>[] types = {ClassLoader.class};
        Object[] args = {parentClassLoader};
        Constructor<?> constructor = clazz.getDeclaredConstructor (types);
        Loader loader = (Loader) constructor.newInstance (args);

        // Push the new loader onto the stack
        digester.push (loader);
        if (digester.getDebug () >= 1)
            digester.log ("new " + loader.getClass ().getName ());
    }

    public void end() {
        Loader loader = (Loader) digester.pop ();
        if (digester.getDebug () >= 1)
            digester.log ("pop " + loader.getClass ().getName ());
    }


}
