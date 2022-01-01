package com.example.startup.rule;


import org.apache.commons.digester.Digester;
import org.apache.commons.digester.RuleSetBase;


public class TldRuleSet extends RuleSetBase {

    /**
     * The matching pattern prefix to use for recognizing our elements.
     */
    protected String prefix = null;

    /**
     * Construct an instance of this <code>RuleSet</code> with the default
     * matching pattern prefix.
     */
    public TldRuleSet() {
        this("");
    }


    /**
     * Construct an instance of this <code>RuleSet</code> with the specified
     * matching pattern prefix.
     *
     * @param prefix Prefix for matching pattern rules (including the
     *  trailing slash character)
     */
    public TldRuleSet(String prefix) {
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
        digester.addCallMethod(prefix + "taglib/listener/listener-class",
                               "addApplicationListener", 0);
    }

}
