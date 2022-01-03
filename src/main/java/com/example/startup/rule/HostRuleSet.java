package com.example.startup.rule;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;

/**
 * <p><strong>RuleSet</strong> for processing the contents of a
 * Host definition element.  This <code>RuleSet</code> does NOT include
 * any rules for nested Context or DefaultContext elements, which should
 * be added via instances of <code>ContextRuleSet</code>.</p>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2001/11/11 19:58:35 $
 */

public class HostRuleSet extends RuleSetBase {


    /**
     * The matching pattern prefix to use for recognizing our elements.
     */
    protected String prefix;


    /**
     * Construct an instance of this <code>RuleSet</code> with the default
     * matching pattern prefix.
     */
    public HostRuleSet() {
        this("");
    }


    /**
     * Construct an instance of this <code>RuleSet</code> with the specified
     * matching pattern prefix.
     *
     * @param prefix Prefix for matching pattern rules (including the
     *  trailing slash character)
     */
    public HostRuleSet(String prefix) {
        super();
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
     *  should be added.
     */
    public void addRuleInstances(Digester digester) {

        digester.addObjectCreate(prefix + "Host",
                                 "com.example.core.StandardHost",
                                 "className");
        digester.addSetProperties(prefix + "Host");
        digester.addRule(prefix + "Host",
                         new CopyParentClassLoaderRule(digester));
        digester.addRule(prefix + "Host",
                         new LifecycleListenerRule
                         (digester,
                          "com.example.startup.HostConfig",
                          "hostConfigClass"));
        digester.addSetNext(prefix + "Host",
                            "addChild",
                            "com.example.Container");

        digester.addCallMethod(prefix + "Host/Alias",
                               "addAlias", 0);


        digester.addObjectCreate(prefix + "Host/Listener",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties(prefix + "Host/Listener");
        digester.addSetNext(prefix + "Host/Listener",
                            "addLifecycleListener",
                            "com.example.life.LifecycleListener");

//        digester.addObjectCreate(prefix + "Host/Logger",
//                                 null, // MUST be specified in the element
//                                 "className");
//        digester.addSetProperties(prefix + "Host/Logger");
//        digester.addSetNext(prefix + "Host/Logger",
//                            "setLogger",
//                            "org.apache.catalina.Logger");

//        digester.addObjectCreate(prefix + "Host/Realm",
//                                 null, // MUST be specified in the element
//                                 "className");
//        digester.addSetProperties(prefix + "Host/Realm");
//        digester.addSetNext(prefix + "Host/Realm",
//                            "setRealm",
//                            "org.apache.catalina.Realm");

        digester.addObjectCreate(prefix + "Host/Valve",
                                 null, // MUST be specified in the element
                                 "className");
        digester.addSetProperties(prefix + "Host/Valve");
        digester.addSetNext(prefix + "Host/Valve",
                            "addValve",
                            "com.example.Valve");

    }


}
