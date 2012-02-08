package urn.gov.hhs.fha.nhinc.adapter.fact;


import com.clarkparsia.empire.SupportsRdfId;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import java.net.URI;
import java.util.UUID;

@XmlAccessorType(XmlAccessType.PROPERTY)
public abstract class UIdAble implements SupportsRdfId {

    private String key;

    private String actualType;

    private boolean isReference;

    @XmlTransient
    public RdfKey getRdfId() {
        if ( key == null && getUniversalId() != null ) {
            key = new Key( getUniversalId() ).toString();
        }
        return new Key( key );
    }

    public void setRdfId( RdfKey theId ) {
        key = theId.toString();
        setUniversalId( key );
    }

    @XmlTransient
    public abstract String getUniversalId();
    public abstract void setUniversalId( String x );

    public String getActualType() {
        return actualType;
    }

    public void setActualType(String actualType) {
        this.actualType = actualType;
    }

    public UIdAble() {
        setUniversalId( "http://" + UUID.randomUUID().toString() );
    }

    public boolean isReference() {
        return isReference;
    }

    public void setReference(boolean reference) {
        isReference = reference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UIdAble uIdAble = (UIdAble) o;

        if (isReference != uIdAble.isReference) return false;

        if ( this.getUniversalId() != null ) {
            return getUniversalId().equals( uIdAble.getUniversalId() );
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = getUniversalId() != null ? getUniversalId().hashCode() : 0;
        result = 31 * result + (isReference ? 1 : 0);
        return result;
    }

    protected static class Key implements RdfKey {

        private URI innerKey;

        public Key() {
            innerKey = null;
        }

        public Key( Object val ) {
            innerKey = URI.create( val.toString() );
        }

        public Key( URI val ) {
            innerKey = val;
        }

        public Object value() {
            return innerKey;
        }

        public String toString() {
            return innerKey == null ? null : innerKey.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (innerKey != null ? !innerKey.equals(key.innerKey) : key.innerKey != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return innerKey != null ? innerKey.hashCode() : 0;
        }
    }
}