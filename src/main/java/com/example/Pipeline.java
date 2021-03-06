package com.example;

import com.example.connector.Request;
import com.example.connector.Response;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * <p>Interface describing a collection of Valves that should be executed
 * in sequence when the <code>invoke()</code> method is invoked.  It is
 * required that a Valve somewhere in the pipeline (usually the last one)
 * must process the request and create the corresponding response, rather
 * than trying to pass the request on.</p>
 *
 * <p>There is generally a single Pipeline instance associated with each
 * Container.  The container's normal request processing functionality is
 * generally encapsulated in a container-specific Valve, which should always
 * be executed at the end of a pipeline.  To facilitate this, the
 * <code>setBasic()</code> method is provided to set the Valve instance that
 * will always be executed last.  Other Valves will be executed in the order
 * that they were added, before the basic Valve is executed.</p>
 *
 * @author Craig R. McClanahan
 * @author Peter Donald
 */
public interface Pipeline extends Contained {

    /**
     * @return the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).
     */
    Valve getBasic();


    /**
     * <p>Set the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).  Prior to setting the basic Valve,
     * the Valve's <code>setContainer()</code> will be called, if it
     * implements <code>Contained</code>, with the owning Container as an
     * argument.  The method may throw an <code>IllegalArgumentException</code>
     * if this Valve chooses not to be associated with this Container, or
     * <code>IllegalStateException</code> if it is already associated with
     * a different Container.</p>
     *
     * @param valve Valve to be distinguished as the basic Valve
     */
    void setBasic(Valve valve);


    /**
     * <p>Add a new Valve to the end of the pipeline associated with this
     * Container.  Prior to adding the Valve, the Valve's
     * <code>setContainer()</code> method will be called, if it implements
     * <code>Contained</code>, with the owning Container as an argument.
     * The method may throw an
     * <code>IllegalArgumentException</code> if this Valve chooses not to
     * be associated with this Container, or <code>IllegalStateException</code>
     * if it is already associated with a different Container.</p>
     *
     * <p>Implementation note: Implementations are expected to trigger the
     * {@link Container#ADD_VALVE_EVENT} for the associated container if this
     * call is successful.</p>
     *
     * @param valve Valve to be added
     * @throws IllegalArgumentException if this Container refused to
     *                                  accept the specified Valve
     * @throws IllegalArgumentException if the specified Valve refuses to be
     *                                  associated with this Container
     * @throws IllegalStateException    if the specified Valve is already
     *                                  associated with a different Container
     */
    void addValve(Valve valve);


    /**
     * @return the set of Valves in the pipeline associated with this
     * Container, including the basic Valve (if any).  If there are no
     * such Valves, a zero-length array is returned.
     */
    Valve[] getValves();


    /**
     * Remove the specified Valve from the pipeline associated with this
     * Container, if it is found; otherwise, do nothing.  If the Valve is
     * found and removed, the Valve's <code>setContainer(null)</code> method
     * will be called if it implements <code>Contained</code>.
     *
     * <p>Implementation note: Implementations are expected to trigger the
     * {@link Container#REMOVE_VALVE_EVENT} for the associated container if this
     * call is successful.</p>
     *
     * @param valve Valve to be removed
     */
    void removeValve(Valve valve);


    /**
     * @return the Valve instance that has been distinguished as the basic
     * Valve for this Pipeline (if any).
     */
    Valve getFirst();

    void invoke(Request request, Response response) throws IOException, ServletException;
}
