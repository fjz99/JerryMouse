package com.example.resource;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 描述资源的属性
 */
public class ResourceAttributes {

    /**
     * ETag.
     */
    public static final String ETAG = "getetag";
    /**
     * Creation date.
     */
    public static final String CREATION_DATE = "creationdate";
    /**
     * Creation date.
     */
    public static final String ALTERNATE_CREATION_DATE = "creation-date";
    /**
     * Last modification date.
     */
    public static final String LAST_MODIFIED = "getlastmodified";
    /**
     * Last modification date.
     */
    public static final String ALTERNATE_LAST_MODIFIED = "last-modified";
    /**
     * Name.
     */
    public static final String NAME = "displayname";
    /**
     * Type.
     */
    public static final String TYPE = "resourcetype";
    /**
     * Type.
     */
    public static final String ALTERNATE_TYPE = "content-type";
    /**
     * Source.
     */
    public static final String SOURCE = "source";
    /**
     * MIME type of the content.
     */
    public static final String CONTENT_TYPE = "getcontenttype";
    /**
     * Content language.
     */
    public static final String CONTENT_LANGUAGE = "getcontentlanguage";
    /**
     * Content length.
     */
    public static final String CONTENT_LENGTH = "getcontentlength";
    /**
     * Content length.
     */
    public static final String ALTERNATE_CONTENT_LENGTH = "content-length";
    /**
     * Collection type.
     */
    public static final String COLLECTION_TYPE = "<collection/>";
    /**
     * HTTP date format.
     */
    protected static final SimpleDateFormat format =
            new SimpleDateFormat ("yyyy-dd-MM HH:mm:ss zzz", Locale.getDefault ());
    /**
     * Date formats using for Date parsing.
     */
    protected static final SimpleDateFormat[] formats = {
            new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.getDefault ()),
            new SimpleDateFormat ("EEE MMM dd HH:mm:ss zzz yyyy", Locale.getDefault ()),
            new SimpleDateFormat ("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.getDefault ()),
            new SimpleDateFormat ("EEE MMMM d HH:mm:ss yyyy", Locale.getDefault ())
    };
    /**
     * Weak ETag.
     */
    protected String weakETag = null;
    /**
     * Strong ETag.
     */
    protected String strongETag = null;
    /**
     * Collection flag.
     */
    protected boolean collection = false;
    /**
     * Content length.
     */
    protected long contentLength = -1;

    /**
     * Creation time.
     */
    protected long creation = -1;
    /**
     * Creation date.
     */
    protected Date creationDate = null;
    /**
     * Last modified time.
     */
    protected long lastModified = -1;
    /**
     * Last modified date.
     */
    protected Date lastModifiedDate = null;
    /**
     * Name.
     */
    protected String name = null;

    /**
     * Is collection.
     */
    public boolean isCollection() {
        return (collection);
    }


    /**
     * Set collection flag.
     */
    public void setCollection(boolean collection) {
        this.collection = collection;
    }


    /**
     * Get content length.
     *
     * @return content length value
     */
    public long getContentLength() {
        return contentLength;
    }


    /**
     * Set content length.
     *
     * @param contentLength New content length value
     */
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }


    /**
     * Get creation time.
     *
     * @return creation time value
     */
    public long getCreation() {
        if (creation != -1L)
            return creation;
        if (creationDate != null)
            return creationDate.getTime ();
        return creation;
    }


    /**
     * Set creation.
     *
     * @param creation New creation value
     */
    public void setCreation(long creation) {
        this.creation = creation;
        this.creationDate = null;
    }


    /**
     * Get creation date.
     *
     * @return Creation date value
     */
    public Date getCreationDate() {
        if (creationDate != null)
            return creationDate;
        if (creation != -1L) {
            creationDate = new Date (creation);
            return creationDate;
        }
        return null;
    }


    /**
     * Creation date mutator.
     *
     * @param creationDate New creation date
     */
    public void setCreationDate(Date creationDate) {
        this.creation = creationDate.getTime ();
        this.creationDate = creationDate;
    }


    /**
     * Get last modified time.
     *
     * @return lastModified time value
     */
    public long getLastModified() {
        if (lastModified != -1L)
            return lastModified;
        if (lastModifiedDate != null)
            return lastModifiedDate.getTime ();
        return lastModified;
    }


    /**
     * Set last modified.
     *
     * @param lastModified New last modified value
     */
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
        this.lastModifiedDate = null;
    }


    /**
     * Set last modified date.
     *
     * @param lastModified New last modified date value
     * @deprecated
     */
    public void setLastModified(Date lastModified) {
        setLastModifiedDate (lastModified);
    }


    /**
     * Get lastModified date.
     *
     * @return LastModified date value
     */
    public Date getLastModifiedDate() {
        if (lastModifiedDate != null)
            return lastModifiedDate;
        if (lastModified != -1L) {
            lastModifiedDate = new Date (lastModified);
            return lastModifiedDate;
        }
        return null;
    }


    /**
     * Last modified date mutator.
     *
     * @param lastModifiedDate New last modified date
     */
    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModified = lastModifiedDate.getTime ();
        this.lastModifiedDate = lastModifiedDate;
    }


    /**
     * Get name.
     *
     * @return Name value
     */
    public String getName() {
        if (name != null)
            return name;
        return null;
    }


    /**
     * Set name.
     *
     * @param name New name value
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * Get resource type.
     *
     * @return String resource type
     */
    public String getResourceType() {
        String result;
        if (collection)
            result = COLLECTION_TYPE;
        else
            result = "";
        return result;
    }


    /**
     * Type mutator.
     *
     * @param resourceType New resource type
     */
    public void setResourceType(String resourceType) {
        collection = resourceType.equals (COLLECTION_TYPE);
    }

    /**
     * Get attribute.
     */
    public Attribute get(String attrID) {
        switch (attrID) {
            case CREATION_DATE:
                return new BasicAttribute (CREATION_DATE, getCreationDate ());
            case ALTERNATE_CREATION_DATE:
                return new BasicAttribute (ALTERNATE_CREATION_DATE,
                        getCreationDate ());
            case LAST_MODIFIED:
                return new BasicAttribute (LAST_MODIFIED,
                        getLastModifiedDate ());
            case ALTERNATE_LAST_MODIFIED:
                return new BasicAttribute (ALTERNATE_LAST_MODIFIED,
                        getLastModifiedDate ());
            case NAME:
                return new BasicAttribute (NAME, getName ());
            case TYPE:
                return new BasicAttribute (TYPE, getResourceType ());
            case ALTERNATE_TYPE:
                return new BasicAttribute (ALTERNATE_TYPE, getResourceType ());
            case CONTENT_LENGTH:
                return new BasicAttribute (CONTENT_LENGTH,
                        getContentLength ());
            case ALTERNATE_CONTENT_LENGTH:
                return new BasicAttribute (ALTERNATE_CONTENT_LENGTH,
                        getContentLength ());
        }
        return null;
    }

    /**
     * Get ETag.
     *
     * @return Weak ETag
     */
    public String getETag() {
        return getETag(false);
    }


    /**
     * Get ETag.
     *
     * @param strong If true, the strong ETag will be returned
     * @return ETag
     */
    public String getETag(boolean strong) {
        return "";
//        String result = null;
//        if (attributes != null) {
//            Attribute attribute = attributes.get(ETAG);
//            if (attribute != null) {
//                try {
//                    result = attribute.get().toString();
//                } catch (NamingException e) {
//                    ; // No value for the attribute
//                }
//            }
//        }
//        if (strong) {
//            // The strong ETag must always be calculated by the resources
//            result = strongETag;
//        } else {
//            // The weakETag is contentLenght + lastModified
//            if (weakETag == null) {
//                weakETag = "W/\"" + getContentLength() + "-"
//                        + getLastModified() + "\"";
//            }
//            result = weakETag;
//        }
//        return result;
    }

    /**
     * Get all attributes.
     */
    public List<BasicAttribute> getAll() {
        List<BasicAttribute> attributes = new ArrayList<> ();
        attributes.add (new BasicAttribute
                (CREATION_DATE, getCreationDate ()));
        attributes.add (new BasicAttribute
                (LAST_MODIFIED, getLastModifiedDate ()));
        attributes.add (new BasicAttribute (NAME, getName ()));
        attributes.add (new BasicAttribute (TYPE, getResourceType ()));
        attributes.add (new BasicAttribute (CONTENT_LENGTH, getContentLength ()));
        return attributes;
    }


    /**
     * Get all attribute IDs.
     */
    public List<String> getIDs() {
        List<String> attributeIDs = new ArrayList<> ();
        attributeIDs.add (CREATION_DATE);
        attributeIDs.add (LAST_MODIFIED);
        attributeIDs.add (NAME);
        attributeIDs.add (TYPE);
        attributeIDs.add (CONTENT_LENGTH);
        return attributeIDs;
    }


    /**
     * Retrieves the number of attributes in the attribute set.
     */
    public int size() {
        return 5;
    }


    /**
     * Case sensitivity.
     */
    public boolean isCaseIgnored() {
        return false;
    }

    @Override
    public String toString() {
        return "ResourceAttributes{" +
                "collection=" + collection +
                ", contentLength=" + contentLength +
                ", creation=" + creation +
                ", creationDate=" + creationDate +
                ", lastModified=" + lastModified +
                ", lastModifiedDate=" + lastModifiedDate +
                ", name='" + name + '\'' +
                '}';
    }
}
