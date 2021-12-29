package com.example.deploy;


import com.example.util.RequestUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Representation of an error page element for a web application,
 * as represented in a <code>&lt;error-page&gt;</code> element in the
 * deployment descriptor.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.4 $ $Date: 2001/07/22 20:25:10 $
 */
@Getter
@Setter
@ToString
public final class ErrorPage {


    /**
     * The error (status) code for which this error page is active.
     * status一定存在，但是exception type不一定存在，比如404
     */
    private int errorCode = 0;


    /**
     * The exception type for which this error page is active.
     */
    private String exceptionType;


    /**
     * The context-relative location to handle this error or exception.
     */
    private String location;


    /**
     * Set the error code (hack for default XmlMapper data type).
     *
     * @param errorCode The new error code
     */
    public void setErrorCode(String errorCode) {
        try {
            this.errorCode = Integer.parseInt (errorCode);
        } catch (Throwable t) {
            this.errorCode = 0;
        }
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }


    public void setLocation(String location) {
        //        if ((location == null) || !location.startsWith("/"))
        //            throw new IllegalArgumentException
        //                ("Error Page Location must start with a '/'");
        this.location = RequestUtil.URLDecode (location);
    }

}
